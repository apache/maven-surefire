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

import java.io.File;
import java.io.PrintStream;

/**
 * Bits and pieces of reporting configuration that seem to be necessary on the provider side.
 * <p/>
 * Todo: Consider moving these fields elsewhere, this concept does not smell too good
 *
 * @author Kristian Rosenvold
 */
public class ReporterConfiguration
{
    private final File reportsDirectory;

    private final PrintStream originalSystemOut;

    /**
     * A non-null Boolean value
     */
    private final boolean trimStackTrace;

    public ReporterConfiguration( File reportsDirectory, boolean trimStackTrace )
    {
        this.reportsDirectory = reportsDirectory;
        this.trimStackTrace = trimStackTrace;

        /*
        * While this may seem slightly odd, when this object is constructed no user code has been run
        * (including classloading), and we can be guaranteed that no-one has modified System.out/System.err.
        * As soon as we start loading user code, all h*ll breaks loose in this respect.
         */
        this.originalSystemOut = System.out;
    }

    /**
     * The directory where reports will be created, normally ${project.build.directory}/surefire-reports
     *
     * @return A file pointing at the specified directory
     */
    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    /**
     * Indicates if reporting should trim the stack traces.
     *
     * @return true if stacktraces should be trimmed in reporting
     */
    public boolean isTrimStackTrace()
    {
        return trimStackTrace;
    }

    /**
     * The original system out belonging to the (possibly forked) surefire process.
     * Note that users of Reporter/ReporterFactory should normally not be using this.
     *
     * @return A printstream.
     */
    public PrintStream getOriginalSystemOut()
    {
        return originalSystemOut;
    }
}
