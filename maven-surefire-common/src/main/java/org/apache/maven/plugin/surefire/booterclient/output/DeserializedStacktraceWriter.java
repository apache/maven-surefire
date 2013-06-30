package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;

/**
 * Represents a deserialize stacktracewriter that has been
 * marshalled across to the plugin from the fork.
 * <p/>
 * Might be better to represent this whole thing differently
 *
 * @author Kristian Rosenvold
 */
public class DeserializedStacktraceWriter
    implements StackTraceWriter
{
    private final String message;

    private final String stackTrace;

    private final String smartTrimmed;

    public DeserializedStacktraceWriter( String message, String smartTrimmed, String stackTrace )
    {
        this.message = message;
        this.stackTrace = stackTrace;
        this.smartTrimmed = smartTrimmed;
    }

    public String smartTrimmedStackTrace()
    {
        return smartTrimmed;
    }

    // Trimming or not is decided on the forking side
    public String writeTraceToString()
    {
        return stackTrace;
    }

    public String writeTrimmedTraceToString()
    {
        return stackTrace;
    }

    public SafeThrowable getThrowable()
    {
        return new SafeThrowable( new Throwable( message ) );
    }
}
