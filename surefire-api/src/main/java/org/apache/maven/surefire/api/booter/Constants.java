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
package org.apache.maven.surefire.api.booter;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 */
public final class Constants {
    private static final String MAGIC_NUMBER_FOR_EVENTS = "maven-surefire-event";
    public static final String MAGIC_NUMBER_FOR_COMMANDS = "maven-surefire-command";
    public static final byte[] MAGIC_NUMBER_FOR_EVENTS_BYTES = MAGIC_NUMBER_FOR_EVENTS.getBytes(US_ASCII);
    public static final byte[] MAGIC_NUMBER_FOR_COMMANDS_BYTES = MAGIC_NUMBER_FOR_COMMANDS.getBytes(US_ASCII);
    public static final Charset DEFAULT_STREAM_ENCODING = UTF_8;
    public static final byte[] DEFAULT_STREAM_ENCODING_BYTES = UTF_8.name().getBytes(US_ASCII);
}
