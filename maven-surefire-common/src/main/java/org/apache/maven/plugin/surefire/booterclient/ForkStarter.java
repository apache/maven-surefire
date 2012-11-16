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
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo;
import org.apache.maven.plugin.surefire.CommonReflector;
import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.SurefireProperties;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.OutputStreamFlushableCommandline;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestProvidingInputStream;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineTimeOutException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.KeyValueSource;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.booter.SurefireExecutionException;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.codehaus.plexus.util.CollectionUtils;


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
 */
public class ForkStarter
{
    private final int forkedProcessTimeoutInSeconds;

    private final ProviderConfiguration providerConfiguration;

    private final StartupConfiguration startupConfiguration;

    private final ForkConfiguration forkConfiguration;

    private final StartupReportConfiguration startupReportConfiguration;

    private final DefaultReporterFactory fileReporterFactory;

    private static volatile int systemPropertiesFileCounter = 0;

    private final ThreadLocal<Integer> threadNumber = new ThreadLocal<Integer>()
    {
        private final AtomicInteger nextThreadNumber = new AtomicInteger( 1 );

        @Override
        protected Integer initialValue()
        {
            return nextThreadNumber.getAndIncrement();
        }
    };


    public ForkStarter( ProviderConfiguration providerConfiguration, StartupConfiguration startupConfiguration,
                        ForkConfiguration forkConfiguration, int forkedProcessTimeoutInSeconds,
                        StartupReportConfiguration startupReportConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
        this.providerConfiguration = providerConfiguration;
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
        this.startupConfiguration = startupConfiguration;
        this.startupReportConfiguration = startupReportConfiguration;
        fileReporterFactory = new DefaultReporterFactory( startupReportConfiguration );
    }

    public RunResult run( SurefireProperties effectiveSystemProperties, DefaultScanResult scanResult,
                          String requestedForkMode )
        throws SurefireBooterForkException, SurefireExecutionException
    {
        final RunResult result;
        try
        {
            Properties providerProperties = providerConfiguration.getProviderProperties();
            scanResult.writeTo( providerProperties );
            if ( ForkConfiguration.FORK_ONCE.equals( requestedForkMode ) )
            {
                final ForkClient forkClient =
                    new ForkClient( fileReporterFactory, startupReportConfiguration.getTestVmSystemProperties() );
                result =
                    fork( null, new PropertiesWrapper( providerProperties ), forkClient, effectiveSystemProperties, 1,
                          null );
            }
            else if ( ForkConfiguration.FORK_ALWAYS.equals( requestedForkMode ) )
            {
                result = runSuitesForkPerTestSet( effectiveSystemProperties, 1 );
            }
            else if ( ForkConfiguration.FORK_PERTHREAD.equals( requestedForkMode ) )
            {
                result = runSuitesForkPerTestSet( effectiveSystemProperties, forkConfiguration.getForkCount() );
            }
            else if ( ForkConfiguration.FORK_ONCE_PERTHREAD.equals( requestedForkMode ) )
            {
                result = runSuitesForkOncePerThread( effectiveSystemProperties, forkConfiguration.getForkCount() );
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

    private RunResult runSuitesForkOncePerThread( final SurefireProperties effectiveSystemProperties, int forkCount )
        throws SurefireBooterForkException
    {

        ArrayList<Future<RunResult>> results = new ArrayList<Future<RunResult>>( forkCount );
        ExecutorService executorService = new ThreadPoolExecutor( forkCount, forkCount, 60, TimeUnit.SECONDS,
                                                                  new ArrayBlockingQueue<Runnable>( forkCount ) );

        try
        {
            // Ask to the executorService to run all tasks
            RunResult globalResult = new RunResult( 0, 0, 0, 0 );

            List<Class<?>> suites = CollectionUtils.iteratorToList( getSuitesIterator() );
            final Queue<String> messageQueue = new ConcurrentLinkedQueue<String>();
            for ( Class<?> clazz : suites )
            {
                messageQueue.add( clazz.getName() );
            }

            for ( int threadNum = 0; threadNum < forkCount && threadNum < suites.size(); threadNum++ )
            {
                final int finalThreadNumber = threadNum + 1;

                Callable<RunResult> pf = new Callable<RunResult>()
                {
                    public RunResult call()
                        throws Exception
                    {
                        TestProvidingInputStream testProvidingInputStream =
                            new TestProvidingInputStream( messageQueue );

                        ForkClient forkClient =
                            new ForkClient( fileReporterFactory, startupReportConfiguration.getTestVmSystemProperties(),
                                            testProvidingInputStream );

                        return fork( null, new PropertiesWrapper( providerConfiguration.getProviderProperties() ),
                                     forkClient, effectiveSystemProperties, finalThreadNumber,
                                     testProvidingInputStream );
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

    private RunResult runSuitesForkPerTestSet( final SurefireProperties effectiveSystemProperties, final int forkCount )
        throws SurefireBooterForkException
    {

        ArrayList<Future<RunResult>> results = new ArrayList<Future<RunResult>>( 500 );
        ExecutorService executorService =
            new ThreadPoolExecutor( forkCount, forkCount, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() );

        try
        {
            // Ask to the executorService to run all tasks
            RunResult globalResult = new RunResult( 0, 0, 0, 0 );
            final Iterator<Class<?>> suites = getSuitesIterator();
            while ( suites.hasNext() )
            {
                final Object testSet = suites.next();
                Callable<RunResult> pf = new Callable<RunResult>()
                {
                    public RunResult call()
                        throws Exception
                    {
                        int thisThreadsThreadNumber = threadNumber.get();
                        if ( thisThreadsThreadNumber > forkCount )
                        {
                            // this would be a bug in the ThreadPoolExecutor
                            throw new IllegalStateException(
                                "More threads than " + forkCount + " have been created by the ThreadPoolExecutor." );
                        }

                        ForkClient forkClient = new ForkClient( fileReporterFactory,
                                                                startupReportConfiguration.getTestVmSystemProperties() );
                        return fork( testSet, new PropertiesWrapper( providerConfiguration.getProviderProperties() ),
                                     forkClient, effectiveSystemProperties, thisThreadsThreadNumber, null );
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

    private RunResult fork( Object testSet, KeyValueSource providerProperties, ForkClient forkClient,
                            SurefireProperties effectiveSystemProperties, int threadNumber,
                            TestProvidingInputStream testProvidingInputStream )
        throws SurefireBooterForkException
    {
        File surefireProperties;
        File systPropsFile = null;
        try
        {
            BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration );

            surefireProperties =
                booterSerializer.serialize( providerProperties, providerConfiguration, startupConfiguration, testSet,
                                            null != testProvidingInputStream );

            if ( effectiveSystemProperties != null )
            {
                SurefireProperties filteredProperties =
                    AbstractSurefireMojo.createCopyAndReplaceThreadNumPlaceholder( effectiveSystemProperties,
                                                                                   threadNumber );
                systPropsFile =
                    SystemPropertyManager.writePropertiesFile( filteredProperties, forkConfiguration.getTempDirectory(),
                                                               "surefire_" + systemPropertiesFileCounter++,
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

        @SuppressWarnings( "unchecked" ) OutputStreamFlushableCommandline cli =
            forkConfiguration.createCommandLine( bootClasspath.getClassPath(),
                                                 startupConfiguration.getClassLoaderConfiguration(),
                                                 startupConfiguration.isShadefire(), threadNumber );

        if ( testProvidingInputStream != null )
        {
            testProvidingInputStream.setFlushReceiverProvider( cli );
        }

        cli.createArg().setFile( surefireProperties );

        if ( systPropsFile != null )
        {
            cli.createArg().setFile( systPropsFile );
        }

        ThreadedStreamConsumer threadedStreamConsumer = new ThreadedStreamConsumer( forkClient );

        if ( forkConfiguration.isDebug() )
        {
            System.out.println( "Forking command line: " + cli );
        }

        RunResult runResult = null;

        try
        {
            final int timeout = forkedProcessTimeoutInSeconds > 0 ? forkedProcessTimeoutInSeconds : 0;
            final int result =
                CommandLineUtils.executeCommandLine( cli, testProvidingInputStream, threadedStreamConsumer,
                                                     threadedStreamConsumer, timeout );
            if ( result != RunResult.SUCCESS )
            {
                throw new SurefireBooterForkException( "Error occurred in starting fork, check output in log" );
            }


        }
        catch ( CommandLineTimeOutException e )
        {
            runResult = RunResult.Timeout;
        }
        catch ( CommandLineException e )
        {
            runResult = RunResult.Failure;
            throw new SurefireBooterForkException( "Error while executing forked tests.", e.getCause() );
        }
        finally
        {
            threadedStreamConsumer.close();
            forkClient.close( runResult == RunResult.Timeout );
            if ( runResult == null )
            {
                runResult = fileReporterFactory.getGlobalRunStatistics().getRunResult();
            }
        }

        return runResult;
    }

    @SuppressWarnings( "unchecked" )
    private Iterator<Class<?>> getSuitesIterator()
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
