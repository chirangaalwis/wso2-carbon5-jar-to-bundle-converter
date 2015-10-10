/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.util;

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

public class Utils {

    public static final String JAR_TO_BUNDLE_DIRECTORY = System.getProperty("java.io.tmpdir").endsWith(File.separator) ?
            System.getProperty("java.io.tmpdir") + "jarsToBundles" :
            System.getProperty("java.io.tmpdir") + File.separator + "jarsToBundles";

    static {
        Path jarsToBundlesDirectory = Paths.get(JAR_TO_BUNDLE_DIRECTORY);
        try {
            if (Files.exists(jarsToBundlesDirectory)) {
                deleteDirectory(jarsToBundlesDirectory);
            }
        } catch (IOException e) {
            System.exit(1);
        }
    }

    public static void createBundle(Path jarFile, Path targetDirectory, Manifest manifest, String extensionPrefix)
            throws IOException {
        if (manifest == null) {
            manifest = new Manifest();
        }
        String exportedPackages = Utils.parseJar(jarFile);

        String fileName = jarFile.getFileName().toString();
        fileName = fileName.replaceAll("-", "_");
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        String symbolicName = extensionPrefix + fileName;
        String pluginName = extensionPrefix + fileName + "_1.0.0.jar";
        Path extensionBundle = Paths.get(targetDirectory.toString(), pluginName);

        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(Constants.MANIFEST_VERSION, "1.0");
        attributes.putValue(Constants.BUNDLE_MANIFEST_VERSION, "2");
        attributes.putValue(Constants.BUNDLE_NAME, fileName);
        attributes.putValue(Constants.BUNDLE_SYMBOLIC_NAME, symbolicName);
        attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
        attributes.putValue(Constants.EXPORT_PACKAGE, exportedPackages);
        attributes.putValue(Constants.BUNDLE_CLASSPATH, ".," + jarFile.getFileName().toString());

        Utils.createBundle(jarFile, extensionBundle, manifest);
    }

    public static void createBundle(Path jarFile, Path bundle, Manifest manifest) throws IOException {
        String extractedDirectoryPath =
                JAR_TO_BUNDLE_DIRECTORY + File.separator + System.currentTimeMillis() + Math.random();
        Path extractedDirectory = Paths.get(extractedDirectoryPath);
        OutputStream manifestOutputStream = null;
        OutputStream p2InfOutputStream = null;

        try {
            if (!Files.exists(extractedDirectory)) {
                Files.createDirectories(extractedDirectory);
            }

            Utils.copyFileToDirectory(jarFile, extractedDirectory);
            Path manifestDirectory = Paths.get(extractedDirectoryPath, "META-INF");
            if (!Files.exists(manifestDirectory)) {
                Files.createDirectories(manifestDirectory);
            }

            Path manifestFile = Paths.get(extractedDirectoryPath, "META-INF", "MANIFEST.MF");
            manifestOutputStream = Files.newOutputStream(manifestFile);
            manifest.write(manifestOutputStream);

            Path p2InfFile = Paths.get(extractedDirectoryPath, "META-INF", "p2.inf");
            if (!Files.exists(p2InfFile)) {
                Files.createFile(p2InfFile);
            }
            p2InfOutputStream = Files.newOutputStream(p2InfFile);
            p2InfOutputStream.write("instructions.configure=markStarted(started:true);".getBytes());
            p2InfOutputStream.flush();

            Utils.archiveDirectory(bundle, extractedDirectory);
            Utils.deleteDirectory(extractedDirectory);
        } finally {
            if (manifestOutputStream != null) {
                manifestOutputStream.close();
            }
            if (p2InfOutputStream != null) {
                p2InfOutputStream.close();
            }
        }
    }

    public static void copyFileToDirectory(Path source, Path destination) throws IOException {
        Path file;

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
                throw new RuntimeException(message);
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
                throw new RuntimeException(message);
            }
        }

        Files.copy(source, file);
    }

    public static void archiveDirectory(Path destinationArchive, Path sourceDirectory) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            String message = String.format("%s is not a directory.", sourceDirectory);
            throw new RuntimeException(message);
        }

        ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(destinationArchive));
        zipDirectory(sourceDirectory, zipOutputStream, sourceDirectory);
        zipOutputStream.close();
    }

    private static void zipDirectory(Path zipDirectory, ZipOutputStream zipOutputStream, Path archiveSourceDirectory)
            throws IOException {
        // get a listing of the directory content
        List<Path> directoryList = Utils.listFiles(zipDirectory);
        final int maximumByteSize = 40960;
        byte[] readBuffer = new byte[maximumByteSize];
        int bytesIn;

        // loop through directoryList, and zip the files
        for (Path aDirectoryItem : directoryList) {
            InputStream fileInputStream = null;

            try {
                Path file = Paths.get(zipDirectory.toString(), aDirectoryItem.getFileName().toString());
                // place the zip entry in the ZipOutputStream object
                zipOutputStream.putNextEntry(new ZipEntry(getZipEntryPath(file, archiveSourceDirectory)));
                if (Files.isDirectory(file)) {
                    /*
                        if the File object is a directory, call this
                        function again to add its content recursively
                    */
                    zipDirectory(file, zipOutputStream, archiveSourceDirectory);
                    // loop again
                    continue;
                }

                /*
                    if we reached here, the File object file was not a directory
                    create an InputStream on top of file
                */
                fileInputStream = Files.newInputStream(file);
                // now write the content of the file to the ZipOutputStream
                while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                    zipOutputStream.write(readBuffer, 0, bytesIn);
                }
            } finally {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            }
        }
    }

    private static String getZipEntryPath(Path file, Path archiveSourceDir) {
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

    public static boolean deleteDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            List<Path> children = Utils.listFiles(directory);
            if (children.size() > 0) {
                for (Path aChild : children) {
                    boolean success = deleteDirectory(aChild);
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        Files.deleteIfExists(directory);
        return true;
    }

    public static List<Path> listFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                files.add(path);
            }
        } catch (IOException e) {
            String message = "Could not load the directory's content.";
            throw new IOException(message, e);
        }
        return files;
    }

    /**
     * returns a list of Java package names within the jar file, separated by a comma
     *
     * @param jarFile the jar file of which the package name list is to be returned
     * @return a list of Java package names within the jar file, separated by a comma
     */
    public static String parseJar(Path jarFile) throws IOException {
        List<String> exportedPackagesList;
        List<ZipEntry> entries;
        ZipInputStream zipInputStream = null;
        try {
            exportedPackagesList = new ArrayList<>();
            zipInputStream = new ZipInputStream(Files.newInputStream(jarFile));
            entries = Utils.populateList(zipInputStream);
        } finally {
            if (zipInputStream != null) {
                zipInputStream.close();
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
     */
    private static List<ZipEntry> populateList(ZipInputStream zipInputStream) throws IOException {
        List<ZipEntry> listEntry;
        listEntry = new ArrayList<>();
        while (zipInputStream.available() == 1) {
            ZipEntry entry = zipInputStream.getNextEntry();
            if (entry == null) {
                break;
            }
            listEntry.add(entry);
        }
        return listEntry;
    }

}
