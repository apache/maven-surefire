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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.maven.plugin.surefire.CommonReflector;
import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer;
import org.apache.maven.plugin.surefire.report.FileReporterFactory;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.providerapi.SurefireProvider;
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

    private final FileReporterFactory fileReporterFactory;

    private static volatile int systemPropertiesFileCounter = 0;

    private static final Object lock = new Object();


    public ForkStarter( ProviderConfiguration providerConfiguration, StartupConfiguration startupConfiguration,
                        ForkConfiguration forkConfiguration, int forkedProcessTimeoutInSeconds,
                        StartupReportConfiguration startupReportConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
        this.providerConfiguration = providerConfiguration;
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
        this.startupConfiguration = startupConfiguration;
        this.startupReportConfiguration = startupReportConfiguration;
        fileReporterFactory = new FileReporterFactory( startupReportConfiguration );
    }

    public RunResult run()
        throws SurefireBooterForkException, SurefireExecutionException
    {
        final RunResult result;
        final String requestedForkMode = forkConfiguration.getForkMode();
        try
        {
            if ( ForkConfiguration.FORK_ONCE.equals( requestedForkMode ) )
            {
                final ForkClient forkClient =
                    new ForkClient( fileReporterFactory, startupReportConfiguration.getTestVmSystemProperties() );
                result = fork( null, providerConfiguration.getProviderProperties(), forkClient,
                               fileReporterFactory.getGlobalRunStatistics() );
            }
            else if ( ForkConfiguration.FORK_ALWAYS.equals( requestedForkMode ) )
            {
                result = runSuitesForkPerTestSet( providerConfiguration.getProviderProperties(), 1 );
            }
            else if ( ForkConfiguration.FORK_PERTHREAD.equals( requestedForkMode ) )
            {
                result = runSuitesForkPerTestSet( providerConfiguration.getProviderProperties(),
                                                  forkConfiguration.getThreadCount() );
            }
            else
            {
                throw new SurefireExecutionException( "Unknown forkmode: " + requestedForkMode, null );
            }
        }
        finally
        {
            fileReporterFactory.close();
        }
        return result;
    }

    private RunResult runSuitesForkPerTestSet( final Properties properties, int forkCount )
        throws SurefireBooterForkException
    {

        ArrayList<Future<RunResult>> results = new ArrayList<Future<RunResult>>( 500 );
        ExecutorService executorService = new ThreadPoolExecutor( forkCount, forkCount, 60, TimeUnit.SECONDS,
                                                                  new ArrayBlockingQueue<Runnable>( 500 ) );

        try
        {
            // Ask to the executorService to run all tasks
            RunResult globalResult = new RunResult( 0, 0, 0, 0 );
            final Iterator suites = getSuitesIterator();
            while ( suites.hasNext() )
            {
                final Object testSet = suites.next();
                final ForkClient forkClient =
                    new ForkClient( fileReporterFactory, startupReportConfiguration.getTestVmSystemProperties() );
                Callable<RunResult> pf = new Callable<RunResult>()
                {
                    public RunResult call()
                        throws Exception
                    {
                        return fork( testSet, (Properties) properties.clone(), forkClient,
                                     fileReporterFactory.getGlobalRunStatistics() );
                    }
                };
                results.add( executorService.submit( pf ) );

            }

            for ( Future<RunResult> result : results )
            {
                try
                {
                    RunResult cur = result.get();
                    if ( cur != null )
                    {
                        globalResult = globalResult.aggregate( cur );
                    }
                    else
                    {
                        throw new SurefireBooterForkException( "No results for " + result.toString() );
                    }
                }
                catch ( InterruptedException e )
                {
                    throw new SurefireBooterForkException( "Interrupted", e );
                }
                catch ( ExecutionException e )
                {
                    throw new SurefireBooterForkException( "ExecutionException", e );
                }
            }
            return globalResult;

        }
        finally
        {
            closeExecutor( executorService );
        }

    }

    private void closeExecutor( ExecutorService executorService )
        throws SurefireBooterForkException
    {
        executorService.shutdown();
        try
        {
            // Should stop immediately, as we got all the results if we are here
            executorService.awaitTermination( 60 * 60, TimeUnit.SECONDS );
        }
        catch ( InterruptedException e )
        {
            throw new SurefireBooterForkException( "Interrupted", e );
        }
    }


    private RunResult fork( Object testSet, Properties properties, ForkClient forkClient,
                            RunStatistics globalRunStatistics )
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
                                                                              "surefire_"
                                                                                  + systemPropertiesFileCounter++,
                                                                              forkConfiguration.isDebug() );
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

        // Surefire-booter + all test classes if "useSystemClassloader"
        // Surefire-booter if !useSystemClassLoader
        Classpath bootClasspath = Classpath.join( bootClasspathConfiguration, additionlClassPathUrls );

        Commandline cli = forkConfiguration.createCommandLine( bootClasspath.getClassPath(),
                                                               startupConfiguration.getClassLoaderConfiguration(),
                                                               startupConfiguration.isShadefire() );

        cli.createArg().setFile( surefireProperties );

        if ( systemProperties != null )
        {
            cli.createArg().setFile( systemProperties );
        }

        ThreadedStreamConsumer threadedStreamConsumer = new ThreadedStreamConsumer( forkClient );

        if ( forkConfiguration.isDebug() )
        {
            System.out.println( "Forking command line: " + cli );
        }

        RunResult runResult;

        try
        {
            final int timeout = forkedProcessTimeoutInSeconds > 0 ? forkedProcessTimeoutInSeconds : 0;
            final int result =
                CommandLineUtils.executeCommandLine( cli, threadedStreamConsumer, threadedStreamConsumer, timeout );

            if ( result != RunResult.SUCCESS )
            {
                throw new SurefireBooterForkException( "Error occurred in starting fork, check output in log" );
            }
            threadedStreamConsumer.close();
            forkClient.close();

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

    private Iterator getSuitesIterator()
        throws SurefireBooterForkException
    {
        try
        {
            final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
            ClassLoader testsClassLoader = classpathConfiguration.createTestClassLoader( false );
            ClassLoader surefireClassLoader =
                classpathConfiguration.createInprocSurefireClassLoader( testsClassLoader );

            CommonReflector commonReflector = new CommonReflector( surefireClassLoader );
            Object reporterFactory = commonReflector.createReportingReporterFactory( startupReportConfiguration );

            final ProviderFactory providerFactory =
                new ProviderFactory( startupConfiguration, providerConfiguration, surefireClassLoader, testsClassLoader,
                                     reporterFactory );
            SurefireProvider surefireProvider = providerFactory.createProvider( false );
            return surefireProvider.getSuites();
        }
        catch ( SurefireExecutionException e )
        {
            throw new SurefireBooterForkException( "Unable to create classloader to find test suites", e );
        }
    }

}
