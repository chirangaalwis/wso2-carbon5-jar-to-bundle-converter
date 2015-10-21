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
package org.wso2.carbon.tool.exceptions;

/**
 * a custom Java class which extends the Java {@code Exception} class
 */
public class JarToBundleConverterException extends Exception {

    private String message;

    /**
     * Constructs a new exception with the specified detail message
     *
     * @param message the detail message of the exception
     */
    public JarToBundleConverterException(String message) {
        super(message);
        this.message = message;
    }

    /**
     * Returns the specified detail message
     *
     * @return the specified detail message
     */
    @Override public String getMessage() {
        return message;
    }

}
