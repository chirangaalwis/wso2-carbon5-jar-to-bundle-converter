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

import org.wso2.carbon.components.exceptions.JarToBundleConverterException;
import org.wso2.carbon.components.interfaces.IJarToBundleConverter;
import org.wso2.carbon.util.Constants;
import org.wso2.carbon.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class DefaultJarToBundleConverter implements IJarToBundleConverter {

    @Override public void convert(Path source, Path destination) throws JarToBundleConverterException {
        if (Files.isDirectory(destination)) {
            try {
                if (!Files.isDirectory(source)) {
                    Utils.createBundle(source, destination, getManifest(), "");
                } else {
                    List<Path> directoryContent = Utils.listFiles(source);
                    for (Path aDirectoryItem : directoryContent) {
                        if (aDirectoryItem.toString().endsWith(".jar")) {
                            Utils.createBundle(aDirectoryItem, destination, getManifest(), "");
                        }
                    }
                }
            } catch (IOException e) {
                String message = String.format("Could not convert the file %s to a OSGi bundle.", source);
                throw new JarToBundleConverterException(message);
            }
        } else {
            String message = String.format("Path instance destination must refer to a directory.[%s]", source);
            throw new JarToBundleConverterException(message);
        }
    }

    private Manifest getManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(Constants.DYNAMIC_IMPORT_PACKAGE, "*");

        return manifest;
    }

}
