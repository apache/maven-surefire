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
import java.util.Iterator;
import java.util.List;
import org.apache.maven.surefire.booter.StartupReportConfiguration;
import org.apache.maven.surefire.report.AbstractConsoleReporter;
import org.apache.maven.surefire.report.AbstractFileReporter;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.DirectConsoleReporter;
import org.apache.maven.surefire.report.MulticastingReporter;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.report.TestSetRunListener;
import org.apache.maven.surefire.report.XMLReporter;
import org.apache.maven.surefire.suite.RunResult;

/**
 * Provides RunListener implementations to the providers.
 * <p/>
 * Keeps a centralized count of test run results.
 *
 * @author Kristian Rosenvold
 */
public class ReporterManagerFactory
    implements ReporterFactory
{

    private final ReporterConfiguration reporterConfiguration;

    private final RunStatistics globalStats = new RunStatistics();

    private final MulticastingReporter multicastingReporter;

    private final StartupReportConfiguration reportConfiguration;

    public ReporterManagerFactory( StartupReportConfiguration reportConfiguration )
    {
        this.reportConfiguration = reportConfiguration;
        this.reporterConfiguration = getReporterConfiguration(  );
        multicastingReporter = new MulticastingReporter( instantiateReports() );
        runStarting();
    }

    private ReporterConfiguration getReporterConfiguration( )
    {
        //noinspection BooleanConstructorCall
        return new ReporterConfiguration( reportConfiguration.getReportsDirectory(), new Boolean( reportConfiguration.isTrimStackTrace() ));
    }

    public RunListener createReporter()
    {
        return new TestSetRunListener( instantiateConsoleReporter(), instantiateFileReporter(),
                                       instantiateXmlReporter(), instantiateConsoleOutputFileReporter(), globalStats );
    }

    private AbstractConsoleReporter instantiateConsoleReporter()
    {
        return reportConfiguration.instantiateConsoleReporter();
    }

    private AbstractFileReporter instantiateFileReporter()
    {
        return reportConfiguration.instantiateFileReporter();
    }

    private XMLReporter instantiateXmlReporter()
    {
        return reportConfiguration.instantiateXmlReporter();
    }

    private Reporter instantiateConsoleOutputFileReporter()
    {
        return reportConfiguration.instantiateConsoleOutputFileReporterName(
            reporterConfiguration.getOriginalSystemOut() );
    }

    private List instantiateReports()
    {
        List result = new ArrayList( );
        addIfNotNull( result, instantiateConsoleReporter() );
        addIfNotNull( result, instantiateFileReporter() );
        addIfNotNull( result, instantiateXmlReporter() );
        addIfNotNull( result, instantiateConsoleOutputFileReporter() );
        return result;
    }

    private void addIfNotNull( List result, Reporter reporter){
        if (reporter != null) result.add(  reporter );
    }

    public RunResult close()
    {
        runCompleted();
        return globalStats.getRunResult();
    }

    public DirectConsoleReporter createConsoleReporter()
    {
        return new DefaultDirectConsoleReporter( reporterConfiguration.getOriginalSystemOut() );
    }

    public void runStarting()
    {
        final DirectConsoleReporter consoleReporter = createConsoleReporter();
        consoleReporter.writeMessage( "" );
        consoleReporter.writeMessage( "-------------------------------------------------------" );
        consoleReporter.writeMessage( " T E S T S" );
        consoleReporter.writeMessage( "-------------------------------------------------------" );
    }

    private void runCompleted()
    {
        final DirectConsoleReporter consoleReporter = createConsoleReporter();
        consoleReporter.writeMessage( "" );
        consoleReporter.writeMessage( "Results :" );
        consoleReporter.writeMessage( "" );
        if ( globalStats.hadFailures() )
        {
            multicastingReporter.writeMessage( "Failed tests: " );
            for ( Iterator iterator = this.globalStats.getFailureSources().iterator(); iterator.hasNext(); )
            {
                consoleReporter.writeMessage( "  " + iterator.next() );
            }
            consoleReporter.writeMessage( "" );
        }
        if ( globalStats.hadErrors() )
        {
            consoleReporter.writeMessage( "Tests in error: " );
            for ( Iterator iterator = this.globalStats.getErrorSources().iterator(); iterator.hasNext(); )
            {
                consoleReporter.writeMessage( "  " + iterator.next() );
            }
            consoleReporter.writeMessage( "" );
        }
        consoleReporter.writeMessage( globalStats.getSummary() );
        consoleReporter.writeMessage( "" );
    }

    public RunStatistics getGlobalRunStatistics()
    {
        return globalStats;
    }
}
