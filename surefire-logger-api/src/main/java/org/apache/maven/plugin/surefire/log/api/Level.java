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

/**
 * Test result levels {@link #FAILURE}, {@link #UNSTABLE}, {@link #SUCCESS}.
 * Writing to console without color via {@link #NO_COLOR}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public enum Level {
    /**
     * Direct println.
     */
    NO_COLOR,

    /**
     * Defaults to bold, green.
     */
    FAILURE,

    /**
     * Defaults to bold, yellow.
     */
    UNSTABLE,

    /**
     * Defaults to bold, red.
     */
    SUCCESS;

    public static Level resolveLevel(
            boolean hasSuccessful, boolean hasFailure, boolean hasError, boolean hasSkipped, boolean hasFlake) {
        if (hasFailure || hasError) {
            return FAILURE;
        }
        if (hasFlake) {
            return UNSTABLE;
        }
        if (hasSkipped) {
            return NO_COLOR;
        }
        if (hasSuccessful) {
            return SUCCESS;
        }
        return NO_COLOR;
    }
}
