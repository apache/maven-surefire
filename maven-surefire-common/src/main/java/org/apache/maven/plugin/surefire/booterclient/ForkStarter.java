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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer;
import org.apache.maven.plugin.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.StartupReportConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.booter.SurefireReflector;
import org.apache.maven.surefire.booter.SurefireStarter;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.suite.RunResult;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineTimeOutException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;


/**
 * Starts the fork or runs in-process.
 * <p/>
 * Lives only on the plugin-side (not present in remote vms)
 * <p/>
 * Knows how to fork new vms and also how to delegate non-forking invocation to SurefireStarter directly
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Brett Porter
 * @author Dan Fabulich
 * @author Carlos Sanchez
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class ForkStarter
{
    private final int forkedProcessTimeoutInSeconds;

    private final ProviderConfiguration providerConfiguration;

    private final StartupConfiguration startupConfiguration;

    private final ForkConfiguration forkConfiguration;

    private final StartupReportConfiguration startupReportConfiguration;

    public ForkStarter( ProviderConfiguration providerConfiguration, StartupConfiguration startupConfiguration,
                        ForkConfiguration forkConfiguration, int forkedProcessTimeoutInSeconds,
                        StartupReportConfiguration startupReportConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
        this.providerConfiguration = providerConfiguration;
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
        this.startupConfiguration = startupConfiguration;
        this.startupReportConfiguration = startupReportConfiguration;
    }

    public RunResult run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        final RunResult result;

        final String requestedForkMode = forkConfiguration.getForkMode();
        if ( ForkConfiguration.FORK_NEVER.equals( requestedForkMode ) )
        {
            SurefireStarter surefireStarter = new SurefireStarter( startupConfiguration, providerConfiguration, this.startupReportConfiguration );
            result = surefireStarter.runSuitesInProcess();
        }
        else if ( ForkConfiguration.FORK_ONCE.equals( requestedForkMode ) )
        {
            result = runSuitesForkOnce();
        }
        else if ( ForkConfiguration.FORK_ALWAYS.equals( requestedForkMode ) )
        {
            result = runSuitesForkPerTestSet();
        }
        else
        {
            throw new SurefireExecutionException( "Unknown forkmode: " + requestedForkMode, null );
        }
        return result;
    }

    private RunResult runSuitesForkOnce()
        throws SurefireBooterForkException
    {
        final ReporterManagerFactory testSetReporterFactory =
            new ReporterManagerFactory( Thread.currentThread().getContextClassLoader(), startupReportConfiguration );
        try
        {
            return fork( null, providerConfiguration.getProviderProperties(), testSetReporterFactory );
        }
        finally
        {
            testSetReporterFactory.close();
        }
    }

    private RunResult runSuitesForkPerTestSet()
        throws SurefireBooterForkException
    {
        RunResult globalResult = new RunResult( 0, 0, 0, 0 );

        ClassLoader testsClassLoader;
        ClassLoader surefireClassLoader;
        try
        {
            testsClassLoader = startupConfiguration.getClasspathConfiguration().createTestClassLoader( false );
            // TODO: assertions = true shouldn't be required if we had proper separation (see TestNG)
            surefireClassLoader =
                startupConfiguration.getClasspathConfiguration().createSurefireClassLoader( testsClassLoader );
        }
        catch ( SurefireExecutionException e )
        {
            throw new SurefireBooterForkException( "Unable to create classloader to find test suites", e );
        }

        final Iterator suites = getSuitesIterator( testsClassLoader, surefireClassLoader );

        Properties properties = new Properties();

        final ReporterManagerFactory testSetReporterFactory =
            new ReporterManagerFactory( Thread.currentThread().getContextClassLoader(), startupReportConfiguration );
        try
        {
            while ( suites.hasNext() )
            {
                Object testSet = suites.next();
                RunResult runResult = fork( testSet, properties, testSetReporterFactory );

                globalResult = globalResult.aggregate( runResult );
            }
            // At this place, show aggregated results ?
            return globalResult;
        }
        finally
        {
            testSetReporterFactory.close();
        }
    }

    private Iterator getSuitesIterator( ClassLoader testsClassLoader, ClassLoader surefireClassLoader )
    {
        SurefireReflector surefireReflector = new SurefireReflector( surefireClassLoader );
        Object reporterFactory =
            surefireReflector.createReportingReporterFactory( startupReportConfiguration );

        final ProviderFactory providerFactory =
            new ProviderFactory( startupConfiguration, providerConfiguration, surefireClassLoader, testsClassLoader,
                                 reporterFactory );
        SurefireProvider surefireProvider = providerFactory.createProvider();
        return surefireProvider.getSuites();
    }


    private RunResult fork( Object testSet, Properties properties, ReporterFactory testSetReporterFactory )
        throws SurefireBooterForkException
    {
        File surefireProperties;
        File systemProperties = null;
        try
        {
            BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration, properties );

            surefireProperties = booterSerializer.serialize( providerConfiguration, startupConfiguration, testSet,
                                                             forkConfiguration.getForkMode() );

            if ( forkConfiguration.getSystemProperties() != null )
            {
                systemProperties = SystemPropertyManager.writePropertiesFile( forkConfiguration.getSystemProperties(),
                                                                              forkConfiguration.getTempDirectory(),
                                                                              "surefire", forkConfiguration.isDebug() );
            }
        }
        catch ( IOException e )
        {
            throw new SurefireBooterForkException( "Error creating properties files for forking", e );
        }

        final Classpath bootClasspathConfiguration = forkConfiguration.getBootClasspath();
        final Classpath additionlClassPathUrls = startupConfiguration.useSystemClassLoader()
            ? startupConfiguration.getClasspathConfiguration().getTestClasspath()
            : null;

        Classpath bootClasspath = Classpath.join( bootClasspathConfiguration, additionlClassPathUrls );

        Commandline cli = forkConfiguration.createCommandLine( bootClasspath.getClassPath(),
                                                               startupConfiguration.getClassLoaderConfiguration(),
                                                               startupConfiguration.isShadefire() );

        cli.createArg().setFile( surefireProperties );

        if ( systemProperties != null )
        {
            cli.createArg().setFile( systemProperties );
        }

        ForkClient out = new ForkClient( testSetReporterFactory,
                                         startupReportConfiguration.getTestVmSystemProperties() );
        ThreadedStreamConsumer threadedStreamConsumer2 = new ThreadedStreamConsumer( out );

        if ( forkConfiguration.isDebug() )
        {
            System.out.println( "Forking command line: " + cli );
        }

        RunResult runResult;

        try
        {
            final int timeout = forkedProcessTimeoutInSeconds > 0 ? forkedProcessTimeoutInSeconds : 0;
            CommandLineUtils.executeCommandLine( cli, threadedStreamConsumer2, threadedStreamConsumer2, timeout );

            threadedStreamConsumer2.close();
            out.close();

            final RunStatistics globalRunStatistics = testSetReporterFactory.getGlobalRunStatistics();

            runResult = globalRunStatistics.getRunResult();
        }
        catch ( CommandLineTimeOutException e )
        {
            runResult = RunResult.Timeout;
        }
        catch ( CommandLineException e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e.getCause() );
        }

        return runResult;
    }
}
