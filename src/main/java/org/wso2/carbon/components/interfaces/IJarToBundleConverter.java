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
package org.wso2.carbon.components.interfaces;

import org.wso2.carbon.components.exceptions.JarToBundleConverterException;

import java.nio.file.Path;

public interface IJarToBundleConverter {

    /**
     * converts a given source jar file or jar files in the source
     * directory to OSGi bundles in a location specified by the destination directory
     *
     * @param source      source jar file or directory containing jar files to be converted to OSGi bundles
     * @param destination destination directory of the created OSGi bundle(s)
     * @throws JarToBundleConverterException
     */
    void convert(Path source, Path destination) throws JarToBundleConverterException;

}
