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
import java.io.InputStream;
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

import org.apache.maven.plugin.logging.Log;
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
import org.apache.maven.shared.utils.cli.ShutdownHookUtils;
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
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.util.DefaultScanResult;

import static org.apache.maven.surefire.booter.Classpath.join;

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
    /**
     * Closes an InputStream
     */
    private final class InputStreamCloser
        implements Runnable
    {
        private InputStream testProvidingInputStream;

        public InputStreamCloser( InputStream testProvidingInputStream )
        {
            this.testProvidingInputStream = testProvidingInputStream;
        }

        public synchronized void run()
        {
            if ( testProvidingInputStream != null )
            {
                try
                {
                    testProvidingInputStream.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
                testProvidingInputStream = null;
            }
        }
    }

    private final int forkedProcessTimeoutInSeconds;

    private final ProviderConfiguration providerConfiguration;

    private final StartupConfiguration startupConfiguration;

    private final ForkConfiguration forkConfiguration;

    private final StartupReportConfiguration startupReportConfiguration;

    private Log log;

    private final DefaultReporterFactory defaultReporterFactory;

    private static volatile int systemPropertiesFileCounter = 0;

    public ForkStarter( ProviderConfiguration providerConfiguration, StartupConfiguration startupConfiguration,
                        ForkConfiguration forkConfiguration, int forkedProcessTimeoutInSeconds,
                        StartupReportConfiguration startupReportConfiguration, Log log )
    {
        this.forkConfiguration = forkConfiguration;
        this.providerConfiguration = providerConfiguration;
        this.forkedProcessTimeoutInSeconds = forkedProcessTimeoutInSeconds;
        this.startupConfiguration = startupConfiguration;
        this.startupReportConfiguration = startupReportConfiguration;
        this.log = log;
        defaultReporterFactory = new DefaultReporterFactory( startupReportConfiguration );
    }

    public RunResult run( SurefireProperties effectiveSystemProperties, DefaultScanResult scanResult )
        throws SurefireBooterForkException, SurefireExecutionException
    {
        final RunResult result;
        try
        {
            Properties providerProperties = providerConfiguration.getProviderProperties();
            scanResult.writeTo( providerProperties );
            if ( isForkOnce() )
            {
                final ForkClient forkClient =
                    new ForkClient( defaultReporterFactory, startupReportConfiguration.getTestVmSystemProperties() );
                result = fork( null, new PropertiesWrapper( providerProperties ), forkClient, effectiveSystemProperties,
                               null );
            }
            else
            {
                if ( forkConfiguration.isReuseForks() )
                {
                    result = runSuitesForkOnceMultiple( effectiveSystemProperties, forkConfiguration.getForkCount() );
                }
                else
                {
                    result = runSuitesForkPerTestSet( effectiveSystemProperties, forkConfiguration.getForkCount() );
                }
            }
        }
        finally
        {
            defaultReporterFactory.close();
        }
        return result;
    }

    private boolean isForkOnce()
    {
        return forkConfiguration.isReuseForks() && 1 == forkConfiguration.getForkCount();
    }

    private RunResult runSuitesForkOnceMultiple( final SurefireProperties effectiveSystemProperties, int forkCount )
        throws SurefireBooterForkException
    {

        ArrayList<Future<RunResult>> results = new ArrayList<Future<RunResult>>( forkCount );
        ExecutorService executorService = new ThreadPoolExecutor( forkCount, forkCount, 60, TimeUnit.SECONDS,
                                                                  new ArrayBlockingQueue<Runnable>( forkCount ) );

        try
        {
            // Ask to the executorService to run all tasks
            RunResult globalResult = new RunResult( 0, 0, 0, 0 );

            List<Class<?>> suites = new ArrayList<Class<?>>();
            Iterator<Class<?>> suitesIterator = getSuitesIterator();
            while ( suitesIterator.hasNext() )
            {
                suites.add( suitesIterator.next() );
            }
            final Queue<String> messageQueue = new ConcurrentLinkedQueue<String>();
            for ( Class<?> clazz : suites )
            {
                messageQueue.add( clazz.getName() );
            }

            for ( int forkNum = 0; forkNum < forkCount && forkNum < suites.size(); forkNum++ )
            {
                Callable<RunResult> pf = new Callable<RunResult>()
                {
                    public RunResult call()
                        throws Exception
                    {
                        TestProvidingInputStream testProvidingInputStream =
                            new TestProvidingInputStream( messageQueue );

                        ForkClient forkClient = new ForkClient( defaultReporterFactory,
                                                                startupReportConfiguration.getTestVmSystemProperties(),
                                                                testProvidingInputStream );

                        return fork( null, new PropertiesWrapper( providerConfiguration.getProviderProperties() ),
                                     forkClient, effectiveSystemProperties, testProvidingInputStream );
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
                        ForkClient forkClient = new ForkClient( defaultReporterFactory,
                                                                startupReportConfiguration.getTestVmSystemProperties() );
                        return fork( testSet, new PropertiesWrapper( providerConfiguration.getProviderProperties() ),
                                     forkClient, effectiveSystemProperties, null );
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
                            SurefireProperties effectiveSystemProperties,
                            TestProvidingInputStream testProvidingInputStream )
        throws SurefireBooterForkException
    {
        int forkNumber = ForkNumberBucket.drawNumber();
        try
        {
            return fork( testSet, providerProperties, forkClient, effectiveSystemProperties, forkNumber,
                         testProvidingInputStream );
        }
        finally
        {
            ForkNumberBucket.returnNumber( forkNumber );
        }
    }

    private RunResult fork( Object testSet, KeyValueSource providerProperties, ForkClient forkClient,
                            SurefireProperties effectiveSystemProperties, int forkNumber,
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
                    AbstractSurefireMojo.createCopyAndReplaceForkNumPlaceholder( effectiveSystemProperties,
                                                                                 forkNumber );
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

        // this could probably be simplified further
        final Classpath bootClasspathConfiguration = startupConfiguration.isProviderMainClass()
            ? startupConfiguration.getClasspathConfiguration().getProviderClasspath()
            : forkConfiguration.getBootClasspath();

        Classpath bootClasspath = join(
            join( bootClasspathConfiguration, startupConfiguration.getClasspathConfiguration().getTestClasspath() ),
            startupConfiguration.getClasspathConfiguration().getProviderClasspath() );

        if ( log.isDebugEnabled() )
        {
            log.debug( bootClasspath.getLogMessage( "boot" ) );
            log.debug( bootClasspath.getCompactLogMessage( "boot(compact)" ) );
        }
        OutputStreamFlushableCommandline cli =
            forkConfiguration.createCommandLine( bootClasspath.getClassPath(), startupConfiguration, forkNumber );

        final InputStreamCloser inputStreamCloser;
        final Thread inputStreamCloserHook;
        if ( testProvidingInputStream != null )
        {
            testProvidingInputStream.setFlushReceiverProvider( cli );
            inputStreamCloser = new InputStreamCloser( testProvidingInputStream );
            inputStreamCloserHook = new Thread( inputStreamCloser );
            ShutdownHookUtils.addShutDownHook( inputStreamCloserHook );
        }
        else
        {
            inputStreamCloser = null;
            inputStreamCloserHook = null;
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
                                                     threadedStreamConsumer, timeout, inputStreamCloser );
            if ( result != RunResult.SUCCESS )
            {
                throw new SurefireBooterForkException( "Error occurred in starting fork, check output in log" );
            }

        }
        catch ( CommandLineTimeOutException e )
        {
            runResult = RunResult.timeout( defaultReporterFactory.getGlobalRunStatistics().getRunResult() );
        }
        catch ( CommandLineException e )
        {
            runResult = RunResult.failure( defaultReporterFactory.getGlobalRunStatistics().getRunResult(), e );
            throw new SurefireBooterForkException( "Error while executing forked tests.", e.getCause() );
        }
        finally
        {
            threadedStreamConsumer.close();
            if ( inputStreamCloser != null )
            {
                inputStreamCloser.run();
                ShutdownHookUtils.removeShutdownHook( inputStreamCloserHook );
            }
            if ( runResult == null )
            {
                runResult = defaultReporterFactory.getGlobalRunStatistics().getRunResult();
            }
            if ( !runResult.isTimeout() )
            {
                StackTraceWriter errorInFork = forkClient.getErrorInFork();
                if ( errorInFork != null )
                {
                    // noinspection ThrowFromFinallyBlock
                    throw new RuntimeException(
                        "There was an error in the forked process\n" + errorInFork.writeTraceToString() );
                }
                if ( !forkClient.isSaidGoodBye() )
                {
                    // noinspection ThrowFromFinallyBlock
                    throw new RuntimeException(
                        "The forked VM terminated without properly saying goodbye. VM crash or System.exit called?"
                            + "\nCommand was " + cli.toString() );
                }

            }
            forkClient.close( runResult.isTimeout() );
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
            ClassLoader unifiedClassLoader = classpathConfiguration.createMergedClassLoader();

            CommonReflector commonReflector = new CommonReflector( unifiedClassLoader );
            Object reporterFactory = commonReflector.createReportingReporterFactory( startupReportConfiguration );

            final ProviderFactory providerFactory =
                new ProviderFactory( startupConfiguration, providerConfiguration, unifiedClassLoader,
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
