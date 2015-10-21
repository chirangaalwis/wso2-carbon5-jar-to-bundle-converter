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
import org.wso2.carbon.tool.util.Utils;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ListFilesTest {

    private static final Path DIRECTORY_ONE = Paths.get(TestConstants.TEST_DIRECTORY_ONE);

    static {
        TestUtils.createDirectoryWithChildren(DIRECTORY_ONE);
    }

    @Test public void listDirectoryContentTest() {
        try {
            List<Path> expectedPaths = TestUtils.getChildPaths(DIRECTORY_ONE);
            List<Path> actualPaths = Utils.listFiles(DIRECTORY_ONE);
            assertArrayEquals(expectedPaths.toArray(), actualPaths.toArray());
        } catch (IOException e) {
            assert false;
        }
    }

}
