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
import java.nio.charset.Charset;
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

/**
 * a Java class which contains utility methods utilized during the process of
 * converting a JAR file to an OSGi bundle
 */
public class Utils {

    private static final FormatLogger LOG = new FormatLogger(Utils.class);

    public static final Path JAR_TO_BUNDLE_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"), "jarsToBundles");

    /**
     * if exists, deletes the temporary directory which holds the unarchived bundle directories during the
     * conversion from JAR files to OSGi bundles
     */
    static {
        try {
            if (Files.exists(JAR_TO_BUNDLE_DIRECTORY)) {
                deleteDirectory(JAR_TO_BUNDLE_DIRECTORY);
            }
        } catch (IOException e) {
            String message = String.format("Failed to delete %s", JAR_TO_BUNDLE_DIRECTORY);
            throw new RuntimeException(message);
        }
    }

    /**
     * Creates an OSGi bundle out of a JAR file
     *
     * @param jarFile         the JAR file to be bundled
     * @param targetDirectory the directory into which the created OSGi bundle needs to be placed
     * @param manifest        the OSGi bundle manifest file
     * @param extensionPrefix prefix, if any, for the bundle
     * @throws IOException if an I/O error occurs while reading the JAR or generating the bundle
     */
    public static void createBundle(Path jarFile, Path targetDirectory, Manifest manifest, String extensionPrefix)
            throws IOException {
        if (manifest == null) {
            manifest = new Manifest();
        }
        String exportedPackages = Utils.parseJar(jarFile);

        Path tempJarFilePathHolder = jarFile.getFileName();
        String fileName;
        if (tempJarFilePathHolder != null) {
            fileName = tempJarFilePathHolder.toString();
            fileName = fileName.replaceAll("-", "_");
            if (fileName.endsWith(".jar")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            String symbolicName = extensionPrefix + fileName;
            String pluginName = extensionPrefix + fileName + "_1.0.0.jar";
            Path extensionBundle = Paths.get(targetDirectory.toString(), pluginName);

            LOG.debug("Setting Manifest attributes.");
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue(Constants.MANIFEST_VERSION, "1.0");
            attributes.putValue(Constants.BUNDLE_MANIFEST_VERSION, "2");
            attributes.putValue(Constants.BUNDLE_NAME, fileName);
            attributes.putValue(Constants.BUNDLE_SYMBOLIC_NAME, symbolicName);
            attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
            attributes.putValue(Constants.EXPORT_PACKAGE, exportedPackages);
            attributes.putValue(Constants.BUNDLE_CLASSPATH, ".," + tempJarFilePathHolder.toString());
            LOG.debug("Finished setting Manifest attributes\n%s[%s], %s[%s], %s[%s]\n, %s[%s], %s[%s], %s[%s], %s[%s]",
                    Constants.MANIFEST_VERSION, attributes.getValue(Constants.MANIFEST_VERSION),
                    Constants.BUNDLE_MANIFEST_VERSION, attributes.getValue(Constants.BUNDLE_MANIFEST_VERSION),
                    Constants.BUNDLE_NAME, attributes.getValue(Constants.BUNDLE_NAME), Constants.BUNDLE_SYMBOLIC_NAME,
                    attributes.getValue(Constants.BUNDLE_SYMBOLIC_NAME), Constants.BUNDLE_VERSION,
                    attributes.getValue(Constants.BUNDLE_VERSION), Constants.EXPORT_PACKAGE,
                    attributes.getValue(Constants.EXPORT_PACKAGE), Constants.BUNDLE_CLASSPATH,
                    attributes.getValue(Constants.BUNDLE_CLASSPATH));

            LOG.debug("Creating an OSGi bundle for JAR file[%s], at target directory[%s].",
                    tempJarFilePathHolder.toString(), extensionBundle.toString());
            Utils.createBundle(jarFile, extensionBundle, manifest);
            LOG.debug("Created an OSGi bundle for JAR file[%s], at target directory[%s].",
                    tempJarFilePathHolder.toString(), extensionBundle.toString());
        } else {
            String message = "Path representing the JAR file name has zero elements.";
            throw new RuntimeException(message);
        }
    }

    /**
     * Creates an OSGi bundle out of a JAR file
     *
     * @param jarFile  the JAR file to be bundled
     * @param bundle   the directory into which the created OSGi bundle needs to be placed into
     * @param manifest the OSGi bundle manifest file
     * @throws IOException if an I/O error occurs while reading the JAR or generating the bundle
     */
    public static void createBundle(Path jarFile, Path bundle, Manifest manifest) throws IOException {
        Path extractedDirectory = Paths
                .get(JAR_TO_BUNDLE_DIRECTORY.toString(), ("" + System.currentTimeMillis() + Math.random()));
        if (!Files.exists(extractedDirectory)) {
            Files.createDirectories(extractedDirectory);
        }

        Utils.copyFileToDirectory(jarFile, extractedDirectory);
        LOG.info("Copied the JAR file[%s] to the OSGi bundle.", jarFile.toString());
        Path manifestDirectory = Paths.get(extractedDirectory.toString(), "META-INF");
        if (!Files.exists(manifestDirectory)) {
            Files.createDirectories(manifestDirectory);
        }
        Path manifestFile = Paths.get(extractedDirectory.toString(), "META-INF", "MANIFEST.MF");
        Path p2InfFile = Paths.get(extractedDirectory.toString(), "META-INF", "p2.inf");
        if (!Files.exists(p2InfFile)) {
            Files.createFile(p2InfFile);
        }

        try (OutputStream manifestOutputStream = Files.newOutputStream(manifestFile);
                OutputStream p2InfOutputStream = Files.newOutputStream(p2InfFile)) {
            manifest.write(manifestOutputStream);
            LOG.info("Generated the OSGi bundle MANIFEST.MF for the JAR file[%s]", jarFile.toString());
            p2InfOutputStream
                    .write("instructions.configure=markStarted(started:true);".getBytes(Charset.forName("UTF-8")));
            p2InfOutputStream.flush();
            LOG.info("Generated the OSGi bundle p2.inf for the JAR file[%s]", jarFile.toString());

            Utils.archiveDirectory(bundle, extractedDirectory);
            LOG.info("The JAR file[%s] has been archived as an OSGi bundle in the destination.", jarFile.toString());

            LOG.debug(
                    "Deleting the temporary directory[%s] used to hold unarchived OSGi directories during the conversion.",
                    extractedDirectory.toString());
            Utils.deleteDirectory(extractedDirectory);
            LOG.debug(
                    "Deleted the temporary directory[%s] used to hold unarchived OSGi directories during the conversion.",
                    extractedDirectory.toString());
        }
    }

    /**
     * Copies the {@code source} file to the specified {@code destination}
     *
     * @param source      the file path of the source
     * @param destination the file path of the destination
     * @throws IOException if an I/O error occurs during the copying of the source file to the destination
     */
    public static void copyFileToDirectory(Path source, Path destination) throws IOException {
        Path file;
        Path sourceFile;

        if ((source != null) && (destination != null)) {
            if (!Files.exists(destination)) {
                if (Files.isDirectory(destination)) {
                    // if the destination points to a non-existing directory
                    Files.createDirectories(destination);
                    sourceFile = source.getFileName();
                    if (sourceFile != null) {
                        file = Paths.get(destination.toString(), sourceFile.toString());
                    } else {
                        String message = "Path instance source has no elements.";
                        throw new RuntimeException(message);
                    }
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
                    sourceFile = source.getFileName();
                    if (sourceFile != null) {
                        file = Paths.get(destination.toString(), sourceFile.toString());
                    } else {
                        String message = "Path instance source source has no elements.";
                        throw new RuntimeException(message);
                    }
                } else {
                    // if the destination points to an existing file
                    String message = String
                            .format("Path instance destination points to the file %s. Path instance destination cannot point to a file.",
                                    destination.toString());
                    throw new RuntimeException(message);
                }
            }

            LOG.debug("Copying the source file[%s] to the destination[%s].", source.toString(), destination.toString());
            Files.copy(source, file);
            LOG.debug("Copied the source file[%s] to the destination[%s]. Path to the copied file[%s].",
                    source.toString(), destination.toString(), file.toString());
        } else {
            String message = "Path instances source and destination cannot refer to null values.";
            throw new RuntimeException(message);
        }
    }

    /**
     * Archives the specified source directory in the specified {@code destination} file path
     *
     * @param destinationArchive the file path to which the source directory should be archived
     * @param sourceDirectory    the source directory to be archived
     * @throws IOException if an I/O error occurs
     */
    public static void archiveDirectory(Path destinationArchive, Path sourceDirectory) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            String message = String.format("%s is not a directory.", sourceDirectory);
            throw new RuntimeException(message);
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(destinationArchive))) {
            LOG.debug("Zipping the source directory[%s] content to the destination[%s].", sourceDirectory.toString(),
                    destinationArchive.toString());
            zipDirectory(sourceDirectory, zipOutputStream, sourceDirectory);
            LOG.debug("Zipped the source directory[%s] content to the destination[%s].", sourceDirectory.toString(),
                    destinationArchive.toString());
        }
    }

    /**
     * Zips the content of the specified file/directory in to the destination specified by the {@code ZipOutputStream}
     *
     * @param zipDirectory           the file/directory which is to be zipped
     * @param zipOutputStream        the ZipOutputStream instance
     * @param archiveSourceDirectory the source directory whose content are to be archived
     * @throws IOException if an I/O error occurs
     */
    private static void zipDirectory(Path zipDirectory, ZipOutputStream zipOutputStream, Path archiveSourceDirectory)
            throws IOException {
        // get a listing of the directory content
        List<Path> directoryList = Utils.listFiles(zipDirectory);
        final int maximumByteSize = 40960;
        byte[] readBuffer = new byte[maximumByteSize];
        int bytesIn;
        Path directoryItemFile;

        // loop through directoryList, and zip the files
        LOG.debug("Started looping through the content in the directory[%s].", zipDirectory.toString());
        for (Path aDirectoryItem : directoryList) {
            directoryItemFile = aDirectoryItem.getFileName();
            if (directoryItemFile != null) {
                Path file = Paths.get(zipDirectory.toString(), directoryItemFile.toString());
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
                try (InputStream fileInputStream = Files.newInputStream(file)) {
                    // now write the content of the file to the ZipOutputStream
                    LOG.debug("Writing the directory item[%s] to the destination and zipping the content.",
                            aDirectoryItem.toString());
                    while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                        zipOutputStream.write(readBuffer, 0, bytesIn);
                    }
                    LOG.debug("Wrote the directory item[%s] to the destination and zipped the content.",
                            aDirectoryItem.toString());
                }
            } else {
                String message = "Path instance aDirectoryItem has no elements.";
                throw new RuntimeException(message);
            }
        }
        LOG.debug("Finished looping through the content in the directory[%s].", zipDirectory.toString());
    }

    /**
     * Returns a {@code String} file path of {@code file} relative to the {@code archiveSourceDir}
     *
     * @param file             the file of which the file path relative to the {@code archiveSourceDir} is to be returned
     * @param archiveSourceDir the source directory to be archived which contains all the file content
     * @return a {@code String} file path of {@code file} relative to the {@code archiveSourceDir}
     */
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

    /**
     * Deletes the directory and its child content
     *
     * @param directory the file path of the directory to be deleted
     * @return true if successfully deleted, else false
     * @throws IOException if an I/O error occurs during the directory deletion
     */
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
        LOG.debug(String.format("Deleting %s.", directory));
        Files.deleteIfExists(directory);
        LOG.debug(String.format("Deleted %s.", directory));
        return true;
    }

    /**
     * Returns a {@code List} of file paths of the child elements of the specified directory
     *
     * @param directory the directory whose child elements are to be returned
     * @return a {@link List} of {@link Path} instances of the child elements of the specified directory
     * @throws IOException if an I/O error occurs
     */
    public static List<Path> listFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                files.add(path);
            }
        }
        return files;
    }

    /**
     * returns a {@code List} of Java package names within the JAR file, separated by a commas
     *
     * @param jarFile the JAR file of which the package name list is to be returned
     * @return a list of Java package names within the JAR file, separated by a comma
     * @throws IOException if an I/O error occurs
     */
    public static String parseJar(Path jarFile) throws IOException {
        List<String> exportedPackagesList = new ArrayList<>();
        List<ZipEntry> entries;
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(jarFile))) {
            entries = Utils.populateList(zipInputStream);
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
        StringBuilder exportedPackages = new StringBuilder();
        for (int i = 0; i < packageArray.length; i++) {
            exportedPackages.append(packageArray[i]);
            if (i != (packageArray.length - 1)) {
                exportedPackages.append(",");
            }
        }

        LOG.debug("Returning a comma separated list of exported packages from the JAR file[%s].", jarFile.toString());
        return exportedPackages.toString();
    }

    /**
     * returns a list of content in the zip file corresponding to the {@code ZipInputStream} instance in the
     * form of {@code ZipEntry} instances
     *
     * @param zipInputStream the {@link ZipInputStream} instance
     * @return a list of content in the zip file corresponding to the {@link ZipInputStream} instance in the
     * form of {@link ZipEntry} instances
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
