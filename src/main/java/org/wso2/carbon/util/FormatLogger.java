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

/**
 * a custom Java Logger class for logging
 */
public class FormatLogger {

    private final Logger log;

    /**
     * Constructs a new FormatLogger with the specified {@code Logger} instance
     *
     * @param log the {@link Logger} instance
     */
    public FormatLogger(Logger log) {
        this.log = log;
    }

    /**
     * Logs a formatted {@code String} value at the INFO Log level
     *
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void info(String formatter, Object... args) {
        log(Level.INFO, formatter, args);
    }

    /**
     * Logs a formatted {@code String} value at the DEBUG Log level
     *
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void debug(String formatter, Object... args) {
        log(Level.DEBUG, formatter, args);
    }

    /**
     * Logs a formatted {@code String} value at the WARN Log level
     *
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void warn(String formatter, Object... args) {
        log(Level.WARN, formatter, args);
    }

    /**
     * Logs a formatted {@code String} value at the ERROR Log level
     *
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void error(String formatter, Object... args) {
        log(Level.ERROR, formatter, args);
    }

    /**
     * Logs a formatted {@code String} value and {@code Throwable} instance at the WARN Log level
     *
     * @param throwable the {@link Throwable} object
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void warn(Throwable throwable, String formatter, Object... args) {
        log(Level.WARN, throwable, formatter, args);
    }

    /**
     * Logs a formatted {@code String} value and {@code Throwable} instance at the ERROR Log level
     *
     * @param throwable the {@link Throwable} object
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void error(Throwable throwable, String formatter, Object... args) {
        log(Level.ERROR, throwable, formatter, args);
    }

    /**
     * Logs a formatted {@code String} value at the specified Log level
     *
     * @param level     the Log level
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void log(Level level, String formatter, Object... args) {
        if (log.isEnabled(level)) {
            log.log(level, String.format(formatter, args));
        }
    }

    /**
     * Logs a formatted {@code String} value and a {@code Throwable} at the specified Log level
     *
     * @param level     the Log level
     * @param throwable the {@link Throwable} object
     * @param formatter the {@link String} to be formatted
     * @param args      the formatting arguments
     */
    public void log(Level level, Throwable throwable, String formatter, Object... args) {
        if (log.isEnabled(level)) {
            log.log(level, String.format(formatter, args), throwable);
        }
    }

}
