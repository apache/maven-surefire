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
package org.apache.maven.surefire.api.fork;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import java.io.File;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;

/**
 * The properties related to the current JVM.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public interface ForkNodeArguments {
    @Nonnull
    String getSessionId();

    /**
     * The index of the forked JVM, from 1 to N.
     *
     * @return index of the forked JVM
     */
    @Nonnegative
    int getForkChannelId();

    @Nonnull
    File dumpStreamText(@Nonnull String text);

    @Nonnull
    File dumpStreamException(@Nonnull Throwable t);

    void logWarningAtEnd(@Nonnull String text);

    @Nonnull
    ConsoleLogger getConsoleLogger();

    @Nonnull
    Object getConsoleLock();

    File getEventStreamBinaryFile();

    File getCommandStreamBinaryFile();
}
