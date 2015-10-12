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
package org.wso2.carbon.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class FormatLogger {

    private final Logger log;

    public FormatLogger(Logger log) {
        this.log = log;
    }

    public void info(String formatter, Object... args) {
        log(Level.INFO, formatter, args);
    }

    public void debug(String formatter, Object... args) {
        log(Level.DEBUG, formatter, args);
    }

    public void warn(String formatter, Object... args) {
        log(Level.WARN, formatter, args);
    }

    public void warn(Throwable throwable, String formatter, Object... args) {
        log(Level.WARN, throwable, formatter, args);
    }

    public void error(String formatter, Object... args) {
        log(Level.ERROR, formatter, args);
    }

    public void error(Throwable throwable, String formatter, Object... args) {
        log(Level.ERROR, throwable, formatter, args);
    }

    private void log(Level level, String formatter, Object... args) {
        if (log.isEnabled(level)) {
            log.log(level, String.format(formatter, args));
        }
    }

    private void log(Level level, Throwable throwable, String formatter, Object... args) {
        if (log.isEnabled(level)) {
            log.log(level, String.format(formatter, args), throwable);
        }
    }

}
