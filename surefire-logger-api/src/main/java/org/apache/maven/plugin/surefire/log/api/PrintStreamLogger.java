/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire.log.api;

import java.io.PrintStream;

/**
 * Console logger for {@link PrintStream}.
 */
public class PrintStreamLogger implements ConsoleLogger {
    private final PrintStream stream;

    public PrintStreamLogger(PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String message) {
        stream.println(message);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String message) {
        stream.println(message);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warning(String message) {
        stream.println(message);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String message) {
        stream.println(message);
    }

    @Override
    public void error(String message, Throwable t) {
        error(ConsoleLoggerUtils.toString(message, t));
    }

    @Override
    public void error(Throwable t) {
        error(null, t);
    }
}
