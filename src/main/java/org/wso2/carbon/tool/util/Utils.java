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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * a Java class which contains utility methods utilized during the process of
 * converting a JAR file to an OSGi bundle
 */
public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    public static final Path JAR_TO_BUNDLE_DIRECTORY = Paths.get("jarsToBundles");

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

                if ((!Files.exists(extensionBundle)) && (!jarFile.getFileName().toString()
                        .equals(extensionBundle.getFileName().toString()))) {
                    LOGGER.info(String.format("Creating the OSGi bundle for JAR file[%s]", jarFile.toString()));
                    LOGGER.fine(String.format("Creating an OSGi bundle for JAR file[%s], at target directory[%s].",
                            tempJarFilePathHolder.toString(), extensionBundle.toString()));
                    Utils.createBundle(jarFile, extensionBundle, manifest);
                    LOGGER.fine(String.format("Created an OSGi bundle for JAR file[%s], at target directory[%s].",
                            tempJarFilePathHolder.toString(), extensionBundle.toString()));
                    LOGGER.info(String.format("Created the OSGi bundle[%s] for JAR file[%s]", pluginName,
                            jarFile.toString()));
                } else {
                    LOGGER.info(String.format("OSGi bundle[%s] already exists in the target directory.", pluginName));
                }
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
        if (manifest != null) {
            Map<String, String> bundleJarProperties = new HashMap<>();
            bundleJarProperties.put("create", "true");
            bundleJarProperties.put("encoding", "UTF-8");
            URI destinationBundleJar = URI.create(String.format("jar:file:%s", bundlePath.toString()));

            Path tempBundleHolder = Paths
                    .get(JAR_TO_BUNDLE_DIRECTORY.toString(), ("" + System.currentTimeMillis() + Math.random()));
            if (!Files.exists(tempBundleHolder)) {
                Files.createDirectories(tempBundleHolder);
            }
            Path manifestFile = Paths.get(tempBundleHolder.toString(), "MANIFEST.MF");
            Path p2InfFile = Paths.get(tempBundleHolder.toString(), "p2.inf");
            if (!Files.exists(p2InfFile)) {
                Files.createFile(p2InfFile);
            }
            try (OutputStream manifestOutputStream = Files.newOutputStream(manifestFile);
                    OutputStream p2InfOutputStream = Files.newOutputStream(p2InfFile);
                    FileSystem zipFileSystem = FileSystems.newFileSystem(destinationBundleJar, bundleJarProperties)) {
                manifest.write(manifestOutputStream);
                LOGGER.fine(String.format("Generated the OSGi bundlePath MANIFEST.MF for the JAR file[%s]",
                        jarFile.toString()));
                p2InfOutputStream
                        .write("instructions.configure=markStarted(started:true);".getBytes(Charset.forName("UTF-8")));
                p2InfOutputStream.flush();
                LOGGER.fine(
                        String.format("Generated the OSGi bundlePath p2.inf for the JAR file[%s]", jarFile.toString()));

                Path manifestFolderPath = zipFileSystem.getPath("META-INF");
                if (!Files.exists(manifestFolderPath)) {
                    Files.createDirectories(manifestFolderPath);
                }
                Path manifestPathInBundle = zipFileSystem.getPath("META-INF", "MANIFEST.MF");
                Path p2InfPathInBundle = zipFileSystem.getPath("META-INF", "p2.inf");
                Files.copy(jarFile, zipFileSystem.getPath(jarFile.getFileName().toString()));
                Files.copy(manifestFile, manifestPathInBundle);
                Files.copy(p2InfFile, p2InfPathInBundle);
                deleteDirectory(tempBundleHolder.getParent());
            }
        } else {
            String message = "Manifest cannot refer to null.";
            throw new JarToBundleConverterException(message);
        }
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
