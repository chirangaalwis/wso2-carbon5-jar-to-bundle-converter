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
package org.wso2.carbon.tool.components;

import org.wso2.carbon.tool.exceptions.JarToBundleConverterException;
import org.wso2.carbon.tool.util.BundleGeneratorUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * the Java executor file for the Jar-to-OSGi-Bundle converter
 */
public class BundleGenerator {
    private static final Logger LOGGER = Logger.getLogger(BundleGenerator.class.getName());

    /**
     * Executes the JAR to OSGi bundle conversion process
     *
     * @param args a {@link String} array providing the source and destination {@link String} path values
     */
    public static void main(String[] args) {
        int sourceIndex = 0;
        int destinationIndex = 1;

        if (args.length == 2) {
            Path source = getPath(args[sourceIndex]);
            Path destination = getPath(args[destinationIndex]);

            if ((source != null) && (destination != null)) {
                if (Files.isDirectory(destination)) {
                    try {
                        if (!Files.isDirectory(source)) {
                            BundleGeneratorUtils.convertFromJarToBundle(source, destination, new Manifest(), "");
                        } else {
                            List<Path> directoryContent = BundleGeneratorUtils.listFiles(source);
                            for (Path aDirectoryItem : directoryContent) {
                                if (aDirectoryItem.toString().endsWith(".jar")) {
                                    BundleGeneratorUtils
                                            .convertFromJarToBundle(aDirectoryItem, destination, new Manifest(), "");
                                }
                            }
                        }
                    } catch (IOException | JarToBundleConverterException e) {
                        String message = "An error has occurred during the conversion. Please try again.";
                        LOGGER.info(message);
                        LOGGER.fine(e.getMessage());
                    }
                } else {
                    String message = "The destination file path is not a directory.";
                    LOGGER.info(message);
                }
            } else {
                String message = "Invalid file path(s). Please try again.";
                LOGGER.info(message);
            }
        }
    }

    /**
     * Returns a {@code Path} instance if the {@code String pathValue} is valid
     *
     * @param pathValue a {@link String} value of the file path
     * @return a {@link Path} instance, if the {@code String pathValue} is valid, else {@code null}
     */
    private static Path getPath(String pathValue) {
        Path path;
        if (pathValue != null) {
            path = Paths.get(pathValue);
            if (Files.exists(path)) {
                return path;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
