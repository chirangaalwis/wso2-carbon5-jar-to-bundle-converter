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
package org.wso2.carbon.tool.util;

import org.wso2.carbon.tool.components.exceptions.JarToBundleConverterException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * a Java class which contains utility methods utilized during the process of
 * converting a JAR file to an OSGi bundle
 */
public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
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
     * @throws IOException                   if an I/O error occurs while reading the JAR or generating the bundle
     * @throws JarToBundleConverterException if the {@link Path} representing the JAR file has no elements or if an
     *                                       error occurs when generating the bundle
     */
    public static void convertFromJarToBundle(Path jarFile, Path targetDirectory, Manifest manifest,
            String extensionPrefix) throws IOException, JarToBundleConverterException {
        LOGGER.info(String.format("Creating the OSGi bundle for JAR file[%s]", jarFile.toString()));

        if (manifest == null) {
            manifest = new Manifest();
        }
        String exportedPackages = Utils.generateExportPackageList(Utils.listPackages(jarFile));

        Path tempJarFilePathHolder = jarFile.getFileName();
        String fileName;
        if (tempJarFilePathHolder != null) {
            fileName = tempJarFilePathHolder.toString();
            fileName = fileName.replaceAll("-", "_");
            if (fileName.endsWith(".jar")) {
                fileName = fileName.substring(0, fileName.length() - 4);
                String symbolicName = extensionPrefix + fileName;
                String pluginName = extensionPrefix + fileName + "_1.0.0.jar";
                Path extensionBundle = Paths.get(targetDirectory.toString(), pluginName);

                LOGGER.finest("Setting Manifest attributes.");
                Attributes attributes = manifest.getMainAttributes();
                attributes.putValue(Constants.MANIFEST_VERSION, "1.0");
                attributes.putValue(Constants.BUNDLE_MANIFEST_VERSION, "2");
                attributes.putValue(Constants.BUNDLE_NAME, fileName);
                attributes.putValue(Constants.BUNDLE_SYMBOLIC_NAME, symbolicName);
                attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
                attributes.putValue(Constants.EXPORT_PACKAGE, exportedPackages);
                attributes.putValue(Constants.BUNDLE_CLASSPATH, ".," + tempJarFilePathHolder.toString());
                attributes.putValue(Constants.DYNAMIC_IMPORT_PACKAGE, "*");
                LOGGER.finest(String.format("Finished setting Manifest attributes%n%s[%s], %s[%s], %s[%s]%n, %s[%s], "
                                + "%s[%s], %s[%s], %s[%s], %s[%s]", Constants.MANIFEST_VERSION,
                        attributes.getValue(Constants.MANIFEST_VERSION), Constants.BUNDLE_MANIFEST_VERSION,
                        attributes.getValue(Constants.BUNDLE_MANIFEST_VERSION), Constants.BUNDLE_NAME,
                        attributes.getValue(Constants.BUNDLE_NAME), Constants.BUNDLE_SYMBOLIC_NAME,
                        attributes.getValue(Constants.BUNDLE_SYMBOLIC_NAME), Constants.BUNDLE_VERSION,
                        attributes.getValue(Constants.BUNDLE_VERSION), Constants.EXPORT_PACKAGE,
                        attributes.getValue(Constants.EXPORT_PACKAGE), Constants.BUNDLE_CLASSPATH,
                        attributes.getValue(Constants.BUNDLE_CLASSPATH), Constants.DYNAMIC_IMPORT_PACKAGE,
                        attributes.getValue(Constants.DYNAMIC_IMPORT_PACKAGE)));

                LOGGER.fine(String.format("Creating an OSGi bundle for JAR file[%s], at target directory[%s].",
                        tempJarFilePathHolder.toString(), extensionBundle.toString()));
                Utils.createBundle(jarFile, extensionBundle, manifest);
                LOGGER.fine(String.format("Created an OSGi bundle for JAR file[%s], at target directory[%s].",
                        tempJarFilePathHolder.toString(), extensionBundle.toString()));

                LOGGER.info(
                        String.format("Created the OSGi bundle[%s] for JAR file[%s]", pluginName, jarFile.toString()));
            }
        } else {
            String message = "Path representing the JAR file name has zero elements.";
            throw new JarToBundleConverterException(message);
        }
    }

    /**
     * Returns a comma separated {@code String} value of the concatenated package names from the {@code List<String>}
     *
     * @param packageNames a {@link List<String>} whose {@link String} package name values are to be concatenated
     * @return a comma separated {@link String} value of the concatenated package names from the {@code List<String>}
     */
    private static String generateExportPackageList(List<String> packageNames) {
        StringBuilder exportedPackages = new StringBuilder();
        for (int packageCount = 0; packageCount < packageNames.size(); packageCount++) {
            exportedPackages.append(packageNames.get(packageCount));
            if (packageCount != (packageNames.size() - 1)) {
                exportedPackages.append(",");
            }
        }
        return exportedPackages.toString();
    }

    /**
     * Creates an OSGi bundlePath out of a JAR file
     *
     * @param jarFile    the JAR file to be bundled
     * @param bundlePath the directory into which the created OSGi bundlePath needs to be placed into
     * @param manifest   the OSGi bundlePath manifest file
     * @throws IOException                   if an I/O error occurs while reading the JAR or generating the bundlePath
     * @throws JarToBundleConverterException if JAR file cannot be copied to the temporary directory or if an error
     *                                       occurs when archiving the final bundlePath directory
     */
    public static void createBundle(Path jarFile, Path bundlePath, Manifest manifest)
            throws IOException, JarToBundleConverterException {
        Path extractedDirectory = Paths
                .get(JAR_TO_BUNDLE_DIRECTORY.toString(), ("" + System.currentTimeMillis() + Math.random()));
        if (!Files.exists(extractedDirectory)) {
            Files.createDirectories(extractedDirectory);
        }

        Utils.copyFileToDirectory(jarFile, extractedDirectory);
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
            LOGGER.fine(String.format("Generated the OSGi bundlePath MANIFEST.MF for the JAR file[%s]",
                    jarFile.toString()));
            p2InfOutputStream
                    .write("instructions.configure=markStarted(started:true);".getBytes(Charset.forName("UTF-8")));
            p2InfOutputStream.flush();
            LOGGER.fine(String.format("Generated the OSGi bundlePath p2.inf for the JAR file[%s]", jarFile.toString()));

            Utils.archiveDirectory(extractedDirectory, bundlePath);
            LOGGER.fine(String.format("The JAR file[%s] has been archived as an OSGi bundlePath in the destination.",
                    jarFile.toString()));

            LOGGER.fine(String.format(
                    "Deleting the temporary directory[%s] used to hold unarchived OSGi directories during the "
                            + "conversion.", extractedDirectory.toString()));
            Utils.deleteDirectory(extractedDirectory);
            LOGGER.fine(String.format(
                    "Deleted the temporary directory[%s] used to hold unarchived OSGi directories during the "
                            + "conversion.", extractedDirectory.toString()));
        }
    }

    /**
     * Copies the {@code source} file to the specified {@code destination} directory
     *
     * @param source      the {@link Path} of the source file
     * @param destination the {@link Path} of the destination directory
     * @throws IOException                   if an I/O error occurs during the copying of the source file to the
     *                                       destination
     * @throws JarToBundleConverterException if the specified {@link Path} instances have no elements or if the
     *                                       {@code source} and {@code destination} {@link Path} instances point to
     *                                       invalid arguments
     */
    public static void copyFileToDirectory(Path source, Path destination)
            throws IOException, JarToBundleConverterException {
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
                        throw new JarToBundleConverterException(message);
                    }
                } else {
                    // if the destination points to a non-existing file
                    String message = String
                            .format("Path instance destination points to the file %s. Path instance destination cannot "
                                    + "point to a file.", destination.toString());
                    throw new JarToBundleConverterException(message);
                }
            } else {
                if (Files.isDirectory(destination)) {
                    // if the destination points to an existing directory
                    sourceFile = source.getFileName();
                    if (sourceFile != null) {
                        file = Paths.get(destination.toString(), sourceFile.toString());
                    } else {
                        String message = "Path instance source source has no elements.";
                        throw new JarToBundleConverterException(message);
                    }
                } else {
                    // if the destination points to an existing file
                    String message = String
                            .format("Path instance destination points to the file %s. Path instance destination cannot "
                                    + "point to a file.", destination.toString());
                    throw new JarToBundleConverterException(message);
                }
            }

            LOGGER.fine(String.format("Copying the source file[%s] to the destination[%s].", source.toString(),
                    destination.toString()));
            Files.copy(source, file);
            LOGGER.fine(String.format("Copied the source file[%s] to the destination[%s]. Path to the copied file[%s].",
                    source.toString(), destination.toString(), file.toString()));
        } else {
            String message = "Path instances source and destination cannot refer to null values.";
            throw new JarToBundleConverterException(message);
        }
    }

    /**
     * Archives the specified {@code sourceDirectory} in the specified {@code destination} file path
     *
     * @param sourceDirectory    the source directory to be archived
     * @param destinationArchive the {@link Path} to which the source directory should be archived
     * @throws IOException                   if an I/O error occurs
     * @throws JarToBundleConverterException if the {@code sourceDirectory} {@link Path} is not a directory
     */
    public static void archiveDirectory(Path sourceDirectory, Path destinationArchive)
            throws IOException, JarToBundleConverterException {
        if (!Files.isDirectory(sourceDirectory)) {
            String message = String.format("%s is not a directory.", sourceDirectory);
            throw new JarToBundleConverterException(message);
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(destinationArchive))) {
            LOGGER.fine(String.format("Zipping the source directory[%s] content to the destination[%s].",
                    sourceDirectory.toString(), destinationArchive.toString()));
            zipDirectory(sourceDirectory, zipOutputStream, sourceDirectory);
            LOGGER.fine(String.format("Zipped the source directory[%s] content to the destination[%s].",
                    sourceDirectory.toString(), destinationArchive.toString()));
        }
    }

    /**
     * Zips the content of the specified file/directory in to the destination specified by the {@code ZipOutputStream}
     *
     * @param zipDirectory    the file/directory which is to be zipped
     * @param zipOutputStream the ZipOutputStream instance
     * @param sourceDirectory the source directory whose content are to be archived
     * @throws IOException                   if an I/O error occurs
     * @throws JarToBundleConverterException if {@link Path} instance(s) has no elements
     */
    private static void zipDirectory(Path zipDirectory, ZipOutputStream zipOutputStream, Path sourceDirectory)
            throws IOException, JarToBundleConverterException {
        // get a listing of the directory content
        List<Path> directoryList = Utils.listFiles(zipDirectory);
        final int maximumByteSize = 40960;
        byte[] readBuffer = new byte[maximumByteSize];
        int bytesIn;
        Path directoryItemFile;
        // loop through directoryList, and zip the files
        LOGGER.fine(
                String.format("Started looping through the content in the directory[%s].", zipDirectory.toString()));

        for (Path aDirectoryItem : directoryList) {
            directoryItemFile = aDirectoryItem.getFileName();

            if (directoryItemFile != null) {
                Path file = Paths.get(zipDirectory.toString(), directoryItemFile.toString());
                // place the zip entry in the ZipOutputStream object
                zipOutputStream.putNextEntry(new ZipEntry(getZipEntryPath(file, sourceDirectory)));
                if (Files.isDirectory(file)) {
                        /*
                            if the File object is a directory, call this
                            function again to add its content recursively
                        */
                    zipDirectory(file, zipOutputStream, sourceDirectory);
                } else {
                        /*
                            if we reached here, the File object file was not a directory
                            create an InputStream on top of file
                        */
                    try (InputStream fileInputStream = Files.newInputStream(file)) {
                        // now write the content of the file to the ZipOutputStream
                        LOGGER.fine(String.format(
                                "Writing the directory item[%s] to the destination and zipping the content.",
                                aDirectoryItem.toString()));
                        while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                            zipOutputStream.write(readBuffer, 0, bytesIn);
                        }
                        LOGGER.fine(
                                String.format("Wrote the directory item[%s] to the destination and zipped the content.",
                                        aDirectoryItem.toString()));
                    }
                }
            } else {
                String message = "Path instance aDirectoryItem has no elements.";
                throw new JarToBundleConverterException(message);
            }
        }
        LOGGER.fine(
                String.format("Finished looping through the content in the directory[%s].", zipDirectory.toString()));
    }

    /**
     * Returns a {@code String} file path relative to the {@code sourceDirectory}
     *
     * @param file            the file of which the path relative to the {@code sourceDirectory} is to be returned
     * @param sourceDirectory the source directory to be archived, which contains all the file content
     * @return a {@link String} file path of {@code file} relative to the {@code sourceDirectory}
     */
    private static String getZipEntryPath(Path file, Path sourceDirectory) {
        String entryPath = file.toString();
        entryPath = entryPath.substring(sourceDirectory.toString().length() + 1);
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
     * @param directory the {@link Path} to the directory to be deleted
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
        LOGGER.fine(String.format("Deleting %s.", directory));
        return Files.deleteIfExists(directory);
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
            directoryStream.forEach(files::add);
        }
        return files;
    }

    /**
     * Returns a {@code List} of {@code String} Java package names within the JAR file
     *
     * @param jarFile the JAR file of which the package name list is to be returned
     * @return a {@link List} of {@link String} Java package names within the JAR file
     * @throws IOException if an I/O error occurs
     */
    public static List<String> listPackages(Path jarFile) throws IOException {
        List<String> exportedPackagesList = new ArrayList<>();
        List<ZipEntry> entries;
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(jarFile))) {
            entries = Utils.populateList(zipInputStream);
        }

        entries.forEach(zipEntry -> {
            String path = zipEntry.getName();
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
        });

        LOGGER.fine(String.format("Returning a List<String> of packages from the JAR file[%s].", jarFile.toString()));
        return exportedPackagesList;
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
