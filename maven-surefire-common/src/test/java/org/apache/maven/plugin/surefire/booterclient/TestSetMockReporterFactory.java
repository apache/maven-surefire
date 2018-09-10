package org.apache.maven.plugin.surefire.booterclient;

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

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.report.RunListener;

import java.io.File;

/**
 * Internal tests use only.
 *
 * @author Kristian Rosenvold
 */
public class TestSetMockReporterFactory
    extends DefaultReporterFactory
{
    public TestSetMockReporterFactory()
    {
        super( defaultValue(), new NullConsoleLogger() );
    }

    @Override
    public RunListener createReporter()
    {
        return new MockReporter();
    }


    /**
     * For testing purposes only.
     *
     * @return StartupReportConfiguration fo testing purposes
     */
    private static StartupReportConfiguration defaultValue()
    {
        File target = new File( "./target" );
        File statisticsFile = new File( target, "TESTHASH" );
        return new StartupReportConfiguration( true, true, "PLAIN", false, false, target, false, null, statisticsFile,
                false, 0, null, null, true );
    }
}
