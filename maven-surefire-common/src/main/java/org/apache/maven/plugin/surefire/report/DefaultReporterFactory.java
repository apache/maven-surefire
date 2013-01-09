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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.suite.RunResult;

/**
 * Provides reporting modules on the plugin side.
 * <p/>
 * Keeps a centralized count of test run results.
 *
 * @author Kristian Rosenvold
 */
public class DefaultReporterFactory
    implements ReporterFactory
{

    private final RunStatistics globalStats = new RunStatistics();

    private final StartupReportConfiguration reportConfiguration;

    private final StatisticsReporter statisticsReporter;

    private final List<TestSetRunListener> listeners =
        Collections.synchronizedList( new ArrayList<TestSetRunListener>() );

    public DefaultReporterFactory( StartupReportConfiguration reportConfiguration )
    {
        this.reportConfiguration = reportConfiguration;
        this.statisticsReporter = reportConfiguration.instantiateStatisticsReporter();
        runStarting();
    }

    public RunListener createReporter()
    {
        return createTestSetRunListener();
    }

    public RunListener createTestSetRunListener()
    {
        TestSetRunListener testSetRunListener =
            new TestSetRunListener( reportConfiguration.instantiateConsoleReporter(),
                                    reportConfiguration.instantiateFileReporter(),
                                    reportConfiguration.instantiateStatelessXmlReporter(),
                                    reportConfiguration.instantiateConsoleOutputFileReporter(), statisticsReporter,
                                    globalStats, reportConfiguration.isTrimStackTrace(),
                                    ConsoleReporter.PLAIN.equals( reportConfiguration.getReportFormat() ),
                                    reportConfiguration.isBriefOrPlainFormat() );
        listeners.add( testSetRunListener );
        return testSetRunListener;
    }

    public RunResult close()
    {
        runCompleted();
        for ( TestSetRunListener listener : listeners )
        {
            listener.close();
        }
        return globalStats.getRunResult();
    }

    private DefaultDirectConsoleReporter createConsoleLogger()
    {
        return new DefaultDirectConsoleReporter( reportConfiguration.getOriginalSystemOut() );
    }

    public void runStarting()
    {
        final DefaultDirectConsoleReporter consoleReporter = createConsoleLogger();
        consoleReporter.info( "" );
        consoleReporter.info( "-------------------------------------------------------" );
        consoleReporter.info( " T E S T S" );
        consoleReporter.info( "-------------------------------------------------------" );
    }

    private void runCompleted()
    {
        final DefaultDirectConsoleReporter logger = createConsoleLogger();
        if ( reportConfiguration.isPrintSummary() )
        {
            logger.info( "" );
            logger.info( "Results :" );
            logger.info( "" );
        }
        if ( globalStats.hadFailures() )
        {
            logger.info( "Failed tests: " );
            for ( Object o : this.globalStats.getFailureSources() )
            {
                logger.info( "  " + o );
            }
            logger.info( "" );
        }
        if ( globalStats.hadErrors() )
        {
            logger.info( "Tests in error: " );
            for ( Object o : this.globalStats.getErrorSources() )
            {
                logger.info( "  " + o );
            }
            logger.info( "" );
        }
        logger.info( globalStats.getSummary() );
        logger.info( "" );
    }

    public RunStatistics getGlobalRunStatistics()
    {
        return globalStats;
    }

    public static DefaultReporterFactory defaultNoXml()
    {
        return new DefaultReporterFactory( StartupReportConfiguration.defaultNoXml() );
    }
}
