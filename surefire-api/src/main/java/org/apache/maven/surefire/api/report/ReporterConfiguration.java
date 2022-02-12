package org.apache.maven.surefire.api.report;

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

/**
 * Bits and pieces of reporting configuration that seem to be necessary on the provider side.
 * <br>
 * Todo: Consider moving these fields elsewhere, this concept does not smell too good
 *
 * @author Kristian Rosenvold
 */
public class ReporterConfiguration
{
    private final File reportsDirectory;
    private final boolean trimStackTrace;

    public ReporterConfiguration( File reportsDirectory, boolean trimStackTrace )
    {
        this.reportsDirectory = reportsDirectory;
        this.trimStackTrace = trimStackTrace;
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
}
