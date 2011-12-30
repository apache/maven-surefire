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

import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.booter.StartupReportConfiguration;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.MulticastingReporter;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.report.TestSetRunListener;
import org.apache.maven.surefire.suite.RunResult;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides RunListener implementations to the providers.
 * <p/>
 * Keeps a centralized count of test run results.
 *
 * @author Kristian Rosenvold
 */
public class FileReporterFactory
    implements ReporterFactory
{

    private final ReporterConfiguration reporterConfiguration;

    private final RunStatistics globalStats = new RunStatistics();

    private final MulticastingReporter multicastingReporter;

    private final StartupReportConfiguration reportConfiguration;

    private final StatisticsReporter statisticsReporter;

    public FileReporterFactory( StartupReportConfiguration reportConfiguration )
    {
        this.reportConfiguration = reportConfiguration;
        this.reporterConfiguration = getReporterConfiguration();
        multicastingReporter = new MulticastingReporter( instantiateReports() );
        this.statisticsReporter = reportConfiguration.instantiateStatisticsReporter();
        runStarting();
    }

    private ReporterConfiguration getReporterConfiguration()
    {
        //noinspection BooleanConstructorCall
        return new ReporterConfiguration( reportConfiguration.getReportsDirectory(),
                                          reportConfiguration.isTrimStackTrace() );
    }

    public RunListener createReporter()
    {
        final PrintStream sout = reporterConfiguration.getOriginalSystemOut();
        return new TestSetRunListener( reportConfiguration.instantiateConsoleReporter(),
                                       reportConfiguration.instantiateFileReporter(),
                                       reportConfiguration.instantiateXmlReporter(),
                                       reportConfiguration.instantiateConsoleOutputFileReporter( sout ),
                                       statisticsReporter, globalStats );
    }

    private List<Reporter> instantiateReports()
    {
        final PrintStream sout = reporterConfiguration.getOriginalSystemOut();
        List<Reporter> result = new ArrayList<Reporter>();
        addIfNotNull( result, reportConfiguration.instantiateConsoleReporter() );
        addIfNotNull( result, reportConfiguration.instantiateFileReporter() );
        addIfNotNull( result, reportConfiguration.instantiateXmlReporter() );
        addIfNotNull( result, reportConfiguration.instantiateConsoleOutputFileReporter( sout ) );
        addIfNotNull( result, statisticsReporter );
        return result;
    }

    private void addIfNotNull( List<Reporter> result, Reporter reporter )
    {
        if ( reporter != null )
        {
            result.add( reporter );
        }
    }

    public RunResult close()
    {
        runCompleted();
        return globalStats.getRunResult();
    }

    private ConsoleLogger createConsoleLogger()
    {
        return new DefaultDirectConsoleReporter( reporterConfiguration.getOriginalSystemOut() );
    }

    public void runStarting()
    {
        final ConsoleLogger consoleReporter = createConsoleLogger();
        consoleReporter.info( "" );
        consoleReporter.info( "-------------------------------------------------------" );
        consoleReporter.info( " T E S T S" );
        consoleReporter.info( "-------------------------------------------------------" );
    }

    private void runCompleted()
    {
        final ConsoleLogger logger = createConsoleLogger();
        logger.info( "" );
        logger.info( "Results :" );
        logger.info( "" );
        if ( globalStats.hadFailures() )
        {
            multicastingReporter.writeMessage( "Failed tests: " );
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

    public static FileReporterFactory defaultNoXml()
    {
        return new FileReporterFactory( StartupReportConfiguration.defaultNoXml() );
    }
}
