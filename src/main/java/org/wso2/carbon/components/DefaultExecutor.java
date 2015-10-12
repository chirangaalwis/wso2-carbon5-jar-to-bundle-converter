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
package org.wso2.carbon.components;

import org.apache.logging.log4j.LogManager;
import org.wso2.carbon.components.interfaces.IJarToBundleConverter;
import org.wso2.carbon.util.FormatLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Scanner;

public class DefaultExecutor {

    private static final Scanner SCANNER = new Scanner(System.in, "UTF-8");
    private static final FormatLogger LOG = new FormatLogger(LogManager.getLogger(DefaultExecutor.class));

    public static void main(String[] args) {
        final IJarToBundleConverter jarToBundleConverter = new DefaultJarToBundleConverter();

        boolean loopAgain;
        String source, destination;
        Path sourcePath, destinationPath;
        do {
            try {
                System.out.print("Enter source path: ");
                source = SCANNER.nextLine();
                sourcePath = getUserInputPath(source);

                System.out.print("Enter destination path: ");
                destination = SCANNER.nextLine();
                destinationPath = getUserInputPath(destination);

                if (!((sourcePath == null) || (destinationPath == null))) {
                    if ((Files.isReadable(sourcePath)) && (Files.isDirectory(destinationPath)) && (Files
                            .isWritable(destinationPath))) {
                        jarToBundleConverter.convert(sourcePath, destinationPath);
                        loopAgain = continueProgram();
                    } else if (!Files.isReadable(sourcePath)) {
                        System.out.println(
                                "The user does not have permission to read from the source path. Please try again.");
                        loopAgain = continueProgram();
                    } else if (!Files.isDirectory(destinationPath)) {
                        System.out.println("The destination path has to be a directory. Please try again.");
                        loopAgain = continueProgram();
                    } else if (!Files.isWritable(destinationPath)) {
                        System.out.println(
                                "The user does not have permission to write to the destination path. Please try again.");
                        loopAgain = continueProgram();
                    } else {
                        loopAgain = continueProgram();
                    }
                } else {
                    System.out.println("Invalid path(s). Please try again.");
                    loopAgain = continueProgram();
                }
            } catch (RuntimeException e) {
                LOG.error(e.getMessage());
                System.out.println("An error has occurred during the application runtime.");
                loopAgain = continueProgram();
            } catch (Exception e) {
                LOG.error(e.getMessage());
                loopAgain = false;
                System.exit(1);
            }
        } while (loopAgain);
    }

    /**
     * Returns the file path input by the user
     *
     * @param input the user input {@code String} value
     * @return the file path input by the user if exists, else returns null
     */
    private static Path getUserInputPath(String input) {
        Path path = Paths.get(input);
        if (Files.exists(path)) {
            return path;
        } else {
            return null;
        }
    }

    /**
     * Returns the user response on whether to continue the program or exit
     *
     * @return true if user prefers to continue, else false
     */
    private static boolean continueProgram() {
        System.out.print("Do you want to continue? (Y/N) ");
        String input = (SCANNER.next());
        SCANNER.nextLine();
        // using the default system Locale
        Locale defaultLocale = Locale.getDefault();
        input = input.toLowerCase(defaultLocale);
        final int firstLetterIndex = 0;
        return (input.charAt(firstLetterIndex) == 'y');
    }

}
