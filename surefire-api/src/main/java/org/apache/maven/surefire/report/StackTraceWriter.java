package org.apache.maven.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Ability to write a stack trace, filtered to omit locations inside Surefire and Maven.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface StackTraceWriter
{
    /**
     * Write the throwable to a string, without trimming.
     *
     * @return the trace
     */
    String writeTraceToString();

    /**
     * Write the throwable to a string, trimming extra locations.
     *
     * @return the trace
     */
    String writeTrimmedTraceToString();

    /**
     * Get the "smart" trimmed (1-2 lines) stacktrace.
     *
     * @return the trace
     */
    String smartTrimmedStackTrace();

    /**
     * Retrieve the throwable for this writer.
     *
     * @return the throwable
     */
    SafeThrowable getThrowable();
}
