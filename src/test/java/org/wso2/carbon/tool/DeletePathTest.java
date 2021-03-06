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
package org.wso2.carbon.tool;

import org.junit.Test;
import org.wso2.carbon.tool.util.BundleGeneratorUtils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DeletePathTest {

    private static final Path DIRECTORY_ONE = Paths.get(TestConstants.TEST_DIRECTORY_ONE);
    private static final Path DIRECTORY_TWO = Paths.get(TestConstants.TEST_DIRECTORY_TWO);

    static {
        TestUtils.createDirectory(DIRECTORY_ONE);
        TestUtils.createDirectoryWithChildren(DIRECTORY_TWO);
    }

    @Test public void deleteChildlessDirectoryTest() {
        boolean deleted;
        if (Files.exists(DIRECTORY_ONE)) {
            try {
                deleted = BundleGeneratorUtils.delete(DIRECTORY_ONE);
            } catch (IOException e) {
                deleted = false;
            }
        } else {
            deleted = false;
        }
        assertTrue(deleted);
    }

    @Test public void deleteDirectoryWithChildrenTest() {
        boolean deleted;
        if (Files.exists(DIRECTORY_TWO)) {
            try {
                deleted = BundleGeneratorUtils.delete(DIRECTORY_TWO);
            } catch (IOException e) {
                deleted = false;
            }
        } else {
            deleted = false;
        }
        assertTrue(deleted);
    }

}
