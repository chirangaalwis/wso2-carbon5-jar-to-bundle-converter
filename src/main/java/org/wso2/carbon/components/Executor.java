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

import org.wso2.carbon.components.interfaces.IJarToBundleConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Executor {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            final IJarToBundleConverter jarToBundleConverter = new DefaultJarToBundleConverter();

            boolean loopAgain;
            String source, destination;
            Path sourcePath, destinationPath;
            while (true) {
                do {
                    System.out.print("Enter source path: ");
                    source = SCANNER.nextLine();
                    sourcePath = getUserInputPath(source);

                    System.out.print("Enter destination path: ");
                    destination = SCANNER.nextLine();
                    destinationPath = getUserInputPath(destination);

                    if (!((sourcePath == null) || (destinationPath == null))) {
                        jarToBundleConverter.convert(sourcePath, destinationPath);
                        System.out.print("Do you want to continue? (Y/N)");
                        loopAgain = continueProgram(SCANNER.next());
                        System.out.println();
                    } else {
                        System.out.println("Invalid path(s). Please try again.");
                        loopAgain = true;
                    }
                } while (loopAgain);
            }
        } catch (Exception e) {
            System.out.println("An error has occurred during program execution.");
            System.exit(1);
        }
    }

    private static Path getUserInputPath(String input) {
        Path path = Paths.get(input);
        if (Files.exists(path)) {
            return path;
        } else {
            return null;
        }
    }

    private static boolean continueProgram(String input) {
        input = input.toLowerCase();
        final int firstLetterIndex = 0;
        return (input.charAt(firstLetterIndex) == 'y');
    }

}
