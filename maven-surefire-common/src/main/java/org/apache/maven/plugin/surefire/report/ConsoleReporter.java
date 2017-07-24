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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.plugin.surefire.log.api.Level;

import static org.apache.maven.plugin.surefire.log.api.Level.resolveLevel;
import static org.apache.maven.plugin.surefire.report.TestSetStats.concatenateWithTestGroup;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Base class for console reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Kristian Rosenvold
 */
public class ConsoleReporter
{
    public static final String BRIEF = "brief";

    public static final String PLAIN = "plain";

    private static final String TEST_SET_STARTING_PREFIX = "Running ";

    private final ConsoleLogger logger;

    public ConsoleReporter( ConsoleLogger logger )
    {
        this.logger = logger;
    }

    public ConsoleLogger getConsoleLogger()
    {
        return logger;
    }

    public void testSetStarting( ReportEntry report )
    {
        MessageBuilder builder = buffer();
        logger.info( concatenateWithTestGroup( builder.a( TEST_SET_STARTING_PREFIX ), report ) );
    }

    public void testSetCompleted( WrappedReportEntry report, TestSetStats testSetStats, List<String> testResults )
    {
        boolean success = testSetStats.getCompletedCount() > 0;
        boolean failures = testSetStats.getFailures() > 0;
        boolean errors = testSetStats.getErrors() > 0;
        boolean skipped = testSetStats.getSkipped() > 0;
        boolean flakes = testSetStats.getSkipped() > 0;
        Level level = resolveLevel( success, failures, errors, skipped, flakes );

        println( testSetStats.getColoredTestSetSummary( report ), level );
        for ( String testResult : testResults )
        {
            println( testResult, level );
        }
    }

    public void reset()
    {
    }

    private void println( String message, Level level )
    {
        switch ( level )
        {
            case FAILURE:
                logger.error( message );
                break;
            case UNSTABLE:
                logger.warning( message );
                break;
            default:
                logger.info( message );
        }
    }
}
