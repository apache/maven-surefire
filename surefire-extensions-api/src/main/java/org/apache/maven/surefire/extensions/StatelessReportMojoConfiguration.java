package org.apache.maven.surefire.extensions;

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
 * Configuration passed to the constructor of default reporter
 * <em>org.apache.maven.plugin.surefire.report.StatelessXmlReporter</em>.
 * Signatures can be changed between major, minor versions or milestones.
 */
public class StatelessReportMojoConfiguration
{
    private final File reportsDirectory;

    private final String reportNameSuffix;

    private final boolean trimStackTrace;

    private final int rerunFailingTestsCount;

    private final String xsdSchemaLocation;

    public StatelessReportMojoConfiguration( File reportsDirectory, String reportNameSuffix, boolean trimStackTrace,
                                             int rerunFailingTestsCount, String xsdSchemaLocation )
    {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
        this.trimStackTrace = trimStackTrace;
        this.rerunFailingTestsCount = rerunFailingTestsCount;
        this.xsdSchemaLocation = xsdSchemaLocation;
    }

    public File getReportsDirectory()
    {
        return reportsDirectory;
    }

    public String getReportNameSuffix()
    {
        return reportNameSuffix;
    }

    public boolean isTrimStackTrace()
    {
        return trimStackTrace;
    }

    public int getRerunFailingTestsCount()
    {
        return rerunFailingTestsCount;
    }

    public String getXsdSchemaLocation()
    {
        return xsdSchemaLocation;
    }
}
