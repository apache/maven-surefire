package org.apache.maven.plugin.surefire.report;

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

import java.util.List;

import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.report.ReportEntry;

/**
 * ConsoleReporter doing nothing rather than using null.
 *
 * @author <a href="mailto:britter@apache.org">Benedikt Ritter</a>
 * @since 2.20
 */
class NullConsoleReporter
    extends ConsoleReporter
{

    static final NullConsoleReporter INSTANCE = new NullConsoleReporter();

    private NullConsoleReporter()
    {
        super( new NullConsoleLogger() );
    }

    @Override
    public void testSetStarting( ReportEntry report )
    {
    }

    @Override
    public void testSetCompleted( WrappedReportEntry report, TestSetStats testSetStats, List<String> testResults )
    {
    }

    @Override
    public void reset()
    {
    }
}
