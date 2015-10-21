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
import org.wso2.carbon.tool.components.exceptions.JarToBundleConverterException;
import org.wso2.carbon.tool.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ListPackagesTest {

    @Test(expected = JarToBundleConverterException.class) public void zipFileExistTest()
            throws JarToBundleConverterException, IOException {
        Path nonExistingJarFile = Paths.get("test.jar");
        Utils.listZipFileContent(nonExistingJarFile);
    }

    @Test public void listJarPackages() {
        List<String> expected = sampleJarPackages();

        ClassLoader classLoader = getClass().getClassLoader();
        Path jarFile = Paths.get(classLoader.getResource("jar-to-bundle-converter-1.0-SNAPSHOT.jar").getFile());
        List<String> actual = null;
        try {
            actual = Utils.listPackages(jarFile);
        } catch (IOException | JarToBundleConverterException e) {
            e.printStackTrace();
        }

        if ((actual != null) && (expected.size() == actual.size())) {
            for (String packageName : expected) {
                boolean exists = exists(packageName, actual);
                if (!exists) {
                    assert false;
                }
            }
            assert true;
        } else {
            assert false;
        }
    }

    private List<String> sampleJarPackages() {
        List<String> packages = new ArrayList<>();
        packages.add("org.wso2.carbon.tool.components");
        packages.add("org.wso2.carbon.tool.components.exceptions");
        packages.add("org.wso2.carbon.tool.components.interfaces");
        packages.add("org.wso2.carbon.tool.util");

        return packages;
    }

    private boolean exists(String value, List<String> list) {
        return ((list != null) && (list.contains(value)));
    }
}
