package org.wso2.carbon.utils;

import org.wso2.carbon.exceptions.JarToBundleConvertException;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarToBundleConvertUtils {

    public static final String JAR_TO_BUNDLE_DIR = System.getProperty("java.io.tmpdir").endsWith(File.separator) ?
            System.getProperty("java.io.tmpdir") + "jarsToBundles" :
            System.getProperty("java.io.tmpdir") + File.separator + "jarsToBundles";

    static {
        Path jarsToBundlesDir = Paths.get(JAR_TO_BUNDLE_DIR);
        try {
            if (Files.exists(jarsToBundlesDir)) {
                deleteDir(jarsToBundlesDir);
            }
        } catch (JarToBundleConvertException e) {
            System.exit(1);
        }
    }

    public static void createBundle(Path jarFile, Path targetDir, Manifest manifest, String extensionPrefix)
            throws JarToBundleConvertException {
        if (manifest == null) {
            manifest = new Manifest();
        }
        String exportedPackages = JarToBundleConvertUtils.parseJar(jarFile);

        String fileName = jarFile.getFileName().toString();
        fileName = fileName.replaceAll("-", "_");
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        String symbolicName = extensionPrefix + fileName;
        String pluginName = extensionPrefix + fileName + "_1.0.0.jar";
        Path extensionBundle = Paths.get(targetDir.toString(), pluginName);

        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(JarToBundleConvertConstants.MANIFEST_VERSION, "1.0");
        attributes.putValue(JarToBundleConvertConstants.BUNDLE_MANIFEST_VERSION, "2");
        attributes.putValue(JarToBundleConvertConstants.BUNDLE_NAME, fileName);
        attributes.putValue(JarToBundleConvertConstants.BUNDLE_SYMBOLIC_NAME, symbolicName);
        attributes.putValue(JarToBundleConvertConstants.BUNDLE_VERSION, "1.0.0");
        attributes.putValue(JarToBundleConvertConstants.EXPORT_PACKAGE, exportedPackages);
        attributes.putValue(JarToBundleConvertConstants.BUNDLE_CLASSPATH, ".," + jarFile.getFileName().toString());

        JarToBundleConvertUtils.createBundle(jarFile, extensionBundle, manifest);
    }

    public static void createBundle(Path jarFile, Path bundle, Manifest manifest) throws JarToBundleConvertException {
        String extractedDirPath = JAR_TO_BUNDLE_DIR + File.separator + System.currentTimeMillis() + Math.random();
        Path extractedDir = Paths.get(extractedDirPath);
        OutputStream manifestOutputStream = null;
        OutputStream p2InfOutputStream = null;

        try {
            if (!Files.exists(extractedDir)) {
                Files.createDirectories(extractedDir);
            }

            JarToBundleConvertUtils.copyFileToDir(jarFile, extractedDir);
            Path manifestDirectory = Paths.get(extractedDirPath, "META-INF");
            if (!Files.exists(manifestDirectory)) {
                Files.createDirectories(manifestDirectory);
            }

            Path manifestFile = Paths.get(extractedDirPath, "META-INF", "MANIFEST.MF");
            manifestOutputStream = Files.newOutputStream(manifestFile);
            manifest.write(manifestOutputStream);

            Path p2InfFile = Paths.get(extractedDirPath, "META-INF", "p2.inf");
            if (!Files.exists(p2InfFile)) {
                Files.createFile(p2InfFile);
            }
            p2InfOutputStream = Files.newOutputStream(p2InfFile);
            p2InfOutputStream.write("instructions.configure=markStarted(started:true);".getBytes());
            p2InfOutputStream.flush();

            JarToBundleConvertUtils.archiveDir(bundle, extractedDir);
            JarToBundleConvertUtils.deleteDir(extractedDir);
        } catch (IOException e) {
            String message = "Could not create the OSGi bundle.";
            throw new JarToBundleConvertException(message, e);
        } finally {
            try {
                if (manifestOutputStream != null) {
                    manifestOutputStream.close();
                }
            } catch (IOException e) {
                String message = String.format("Unable to close the OutputStream %s", e.getMessage());
                throw new JarToBundleConvertException(message, e);
            }

            try {
                if (p2InfOutputStream != null) {
                    p2InfOutputStream.close();
                }
            } catch (IOException e) {
                String message = String.format("Unable to close the OutputStream %s", e.getMessage());
                throw new JarToBundleConvertException(message, e);
            }
        }
    }

    public static void copyFileToDir(Path source, Path destination) throws JarToBundleConvertException {
        Path file;
        try {
            if (!Files.exists(destination)) {
                if (Files.isDirectory(destination)) {
                    // if the destination points to a non-existing directory
                    Files.createDirectories(destination);
                    file = Paths.get(destination.toString(), source.getFileName().toString());
                } else {
                    // if the destination points to a non-existing file
                    String message = String
                            .format("Path instance destination points to the file %s. Path instance destination cannot point to a file.",
                                    destination.toString());
                    throw new JarToBundleConvertException(message);
                }
            } else {
                if (Files.isDirectory(destination)) {
                    // if the destination points to an existing directory
                    file = Paths.get(destination.toString(), source.getFileName().toString());
                } else {
                    // if the destination points to an existing file
                    String message = String
                            .format("Path instance destination points to the file %s. Path instance destination cannot point to a file.",
                                    destination.toString());
                    throw new JarToBundleConvertException(message);
                }
            }

            Files.copy(source, file);
        } catch (Exception e) {
            String message = "Could not copy the specified file to the specified destination.";
            throw new JarToBundleConvertException(message, e);
        }
    }

    public static void archiveDir(Path destinationArchive, Path sourceDir) throws JarToBundleConvertException {
        if (!Files.isDirectory(sourceDir)) {
            String message = String.format("%s is not a directory", sourceDir);
            throw new JarToBundleConvertException(message);
        }

        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(destinationArchive));
            zipDir(sourceDir, zipOutputStream, sourceDir);
            zipOutputStream.close();
        } catch (IOException e) {
            String message = "Could not create the ZipOutputStream instance.";
            throw new JarToBundleConvertException(message);
        }
    }

    protected static void zipDir(Path zipDir, ZipOutputStream zos, Path archiveSourceDir)
            throws JarToBundleConvertException {
        // get a listing of the directory content
        List<String> dirList = JarToBundleConvertUtils.fileList(zipDir);
        byte[] readBuffer = new byte[40960];
        int bytesIn;

        // loop through dirList, and zip the files
        for (String aDirList : dirList) {
            InputStream fileInputStream = null;
            try {
                Path file = Paths.get(zipDir.toString(), aDirList);
                // place the zip entry in the ZipOutputStream object
                zos.putNextEntry(new ZipEntry(getZipEntryPath(file, archiveSourceDir)));
                if (Files.isDirectory(file)) {
                    /*
                        if the File object is a directory, call this
                        function again to add its content recursively
                     */
                    zipDir(file, zos, archiveSourceDir);
                    // loop again
                    continue;
                }

                /*
                    if we reached here, the File object file was not a directory
                    create an InputStream on top of file
                */
                fileInputStream = Files.newInputStream(file);
                //now write the content of the file to the ZipOutputStream
                while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
            } catch (IOException e) {
                String message = "Could not create the ZipOutputStream instance.";
                throw new JarToBundleConvertException(message);
            } finally {
                try {
                    //close the Stream
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (IOException e) {
                    String message = "Could not create the ZipOutputStream instance.";
                    throw new JarToBundleConvertException(message);
                }
            }
        }
    }

    protected static String getZipEntryPath(Path file, Path archiveSourceDir) {
        String entryPath = file.toString();
        entryPath = entryPath.substring(archiveSourceDir.toString().length() + 1);
        if (File.separatorChar == '\\') {
            entryPath = entryPath.replace(File.separatorChar, '/');
        }
        if (Files.isDirectory(file)) {
            entryPath += "/";
        }
        return entryPath;
    }

    public static boolean deleteDir(Path directory) throws JarToBundleConvertException {
        if (Files.isDirectory(directory)) {
            List<String> children = JarToBundleConvertUtils.fileList(directory);
            if (children.size() > 0) {
                for (String aChild : children) {
                    boolean success = deleteDir(Paths.get(aChild));
                    if (!success) {
                        return false;
                    }
                }
            }
        }

        try {
            // The directory is now empty so delete it
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            String message = String.format("Could not delete the directory %s", directory);
            throw new JarToBundleConvertException(message, e);
        }
        return true;
    }

    public static List<String> fileList(Path directory) throws JarToBundleConvertException {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                fileNames.add(path.toString());
            }
        } catch (IOException e) {
            String message = "Could not load the directory's content.";
            throw new JarToBundleConvertException(message, e);
        }
        return fileNames;
    }

    /**
     * returns a list of Java package names within the jar file, separated by a comma
     *
     * @param jarFile the jar file of which the package name list is to be returned
     * @return a list of Java package names within the jar file, separated by a comma
     * @throws JarToBundleConvertException if the jar file parsing failed or if the
     *                                     ZipInputStream could not be closed
     */
    public static String parseJar(Path jarFile) throws JarToBundleConvertException {
        List<String> exportedPackagesList;
        List<ZipEntry> entries;
        ZipInputStream zipInputStream = null;
        try {
            exportedPackagesList = new ArrayList<>();
            zipInputStream = new ZipInputStream(Files.newInputStream(jarFile));
            entries = JarToBundleConvertUtils.populateList(zipInputStream);
        } catch (IOException e) {
            String message = "Could not parse the jar file, successfully.";
            throw new JarToBundleConvertException(message, e);
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (IOException e) {
                String message = "Could not close the ZipInputStream instance.";
                throw new JarToBundleConvertException(message, e);
            }
        }

        for (ZipEntry entry : entries) {
            String path = entry.getName();

            if (!path.endsWith("/") && path.endsWith(".class")) {
                //This is package that contains classes. Thus, exportedPackagesList
                int index = path.lastIndexOf('/');
                if (index != -1) {
                    path = path.substring(0, index);
                    path = path.replaceAll("/", ".");
                    if (!exportedPackagesList.contains(path)) {
                        exportedPackagesList.add(path);
                    }
                }
            }
        }

        String[] packageArray = exportedPackagesList.toArray(new String[exportedPackagesList.size()]);
        StringBuffer exportedPackages = new StringBuffer();
        for (int i = 0; i < packageArray.length; i++) {
            exportedPackages.append(packageArray[i]);
            if (i != (packageArray.length - 1)) {
                exportedPackages.append(",");
            }
        }
        return exportedPackages.toString();
    }

    /**
     * returns a list of content in the zip file corresponding to the ZipInputStream instance in the
     * form of ZipEntry instances
     *
     * @param zipInputStream the ZipInputStream instance
     * @return a list of content in the zip file corresponding to the ZipInputStream instance in the
     * form of ZipEntry instances
     * @throws JarToBundleConvertException if the zip file content could not be loaded to the list
     */
    private static List<ZipEntry> populateList(ZipInputStream zipInputStream) throws JarToBundleConvertException {
        List<ZipEntry> listEntry;
        try {
            listEntry = new ArrayList<>();
            while (zipInputStream.available() == 1) {
                ZipEntry entry = zipInputStream.getNextEntry();
                if (entry == null) {
                    break;
                }
                listEntry.add(entry);
            }
        } catch (IOException e) {
            String message = "Could not load the zip file content, to the list.";
            throw new JarToBundleConvertException(message, e);
        }
        return listEntry;
    }

}
