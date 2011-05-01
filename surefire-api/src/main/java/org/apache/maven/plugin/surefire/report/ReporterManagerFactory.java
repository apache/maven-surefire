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

import java.lang.reflect.Constructor;
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
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.SurefireReflectionException;

/**
 * Provides RunListener implementations to the providers.
 * <p/>
 * Keeps a centralized count of test run results.
 *
 * TODO: Move out of API module
 *
 * @author Kristian Rosenvold
 */
public class ReporterManagerFactory
    implements ReporterFactory
{

    private ClassLoader surefireClassLoader;

    private ReporterConfiguration reporterConfiguration;

    private final RunStatistics globalStats = new RunStatistics();

    private final MulticastingReporter multicastingReporter;

    private final StartupReportConfiguration reportConfiguration;

    public ReporterManagerFactory( ClassLoader surefireClassLoader, StartupReportConfiguration reportConfiguration )
    {
        this.reportConfiguration = reportConfiguration;
        this.reporterConfiguration = getReporterConfiguration(  );
        this.surefireClassLoader = surefireClassLoader;
        multicastingReporter = new MulticastingReporter( instantiateReports() );
        runStarting();
    }

    private ReporterConfiguration getReporterConfiguration( )
    {
        return new ReporterConfiguration( reportConfiguration.getReportsDirectory(), new Boolean( reportConfiguration.isTrimStackTrace() ));
    }

    public RunListener createReporter()
    {
        return new TestSetRunListener( instantiateConsoleReporter(), instantiateFileReporter(),
                                       instantiateXmlReporter(), instantiateConsoleOutputFileReporter(), globalStats );
    }

    private AbstractConsoleReporter instantiateConsoleReporter()
    {
        return (AbstractConsoleReporter) instantiateReport( reportConfiguration.getConsoleReporter() );
    }

    private AbstractFileReporter instantiateFileReporter()
    {
        return (AbstractFileReporter) instantiateReport( reportConfiguration.getFileReporter() );
    }

    private XMLReporter instantiateXmlReporter()
    {
        return (XMLReporter) instantiateReport( reportConfiguration.getXmlReporterName() );
    }

    private Reporter instantiateConsoleOutputFileReporter()
    {
        return instantiateReport( reportConfiguration.getConsoleOutputFileReporterName() );
    }

    private List instantiateReports()
    {
        return instantiateReportsNewStyle( reportConfiguration.getReports(), reporterConfiguration, surefireClassLoader );
    }

    public RunResult close()
    {
        runCompleted();
        return globalStats.getRunResult();
    }

    private List instantiateReportsNewStyle( List reportDefinitions, ReporterConfiguration reporterConfiguration,
                                             ClassLoader classLoader )
    {
        List reports = new ArrayList();

        for ( Iterator i = reportDefinitions.iterator(); i.hasNext(); )
        {

            String className = (String) i.next();

            Reporter report = instantiateReportNewStyle( className, reporterConfiguration, classLoader );

            reports.add( report );
        }

        return reports;
    }

    public Reporter instantiateReport( String reportName )
    {
        if ( reportName == null )
        {
            return null;
        }
        return instantiateReportNewStyle( reportName, reporterConfiguration, surefireClassLoader );
    }

    private static Reporter instantiateReportNewStyle( String className, ReporterConfiguration params,
                                                       ClassLoader classLoader )
    {
        Class clazz = ReflectionUtils.loadClass( classLoader, className );

        if ( params != null )
        {
            Class[] paramTypes = new Class[1];
            paramTypes[0] = ReflectionUtils.loadClass( classLoader, ReporterConfiguration.class.getName() );
            Constructor constructor = ReflectionUtils.getConstructor( clazz, paramTypes );
            return (Reporter) ReflectionUtils.newInstance( constructor, new Object[]{ params } );
        }
        else
        {
            try
            {
                return (Reporter) clazz.newInstance();
            }
            catch ( IllegalAccessException e )
            {
                throw new SurefireReflectionException( e );
            }
            catch ( InstantiationException e )
            {
                throw new SurefireReflectionException( e );
            }
        }

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
