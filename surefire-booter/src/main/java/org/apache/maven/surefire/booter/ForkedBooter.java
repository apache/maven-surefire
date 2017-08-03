package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.max;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.lang.System.setErr;
import static java.lang.System.setOut;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.booter.CommandReader.getReader;
import static org.apache.maven.surefire.booter.SystemPropertyManager.setSystemProperties;
import static org.apache.maven.surefire.util.ReflectionUtils.instantiateOneArg;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThreadFactory;

/**
 * The part of the booter that is unique to a forked vm.
 * <p/>
 * Deals with deserialization of the booter wire-level protocol
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 */
public final class ForkedBooter
{
    private static final long DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS = 30;
    private static final long PING_TIMEOUT_IN_SECONDS = 20;
    private static final long ONE_SECOND_IN_MILLIS = 1000;
    private static final ScheduledExecutorService JVM_PING = createPingScheduler();

    private static volatile ScheduledThreadPoolExecutor jvmTerminator;
    private static volatile long systemExitTimeoutInSeconds = DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS;

    /**
     * This method is invoked when Surefire is forked - this method parses and organizes the arguments passed to it and
     * then calls the Surefire class' run method. <p/> The system exit code will be 1 if an exception is thrown.
     *
     * @param args Commandline arguments
     */
    public static void main( String... args )
    {
        final CommandReader reader = startupMasterProcessReader();
        final ScheduledFuture<?> pingScheduler = listenToShutdownCommands( reader );
        final ForkedChannelEncoder eventChannel = new ForkedChannelEncoder( out );
        try
        {
            final String tmpDir = args[0];
            final String dumpFileName = args[1];
            final String surefirePropsFileName = args[2];

            BooterDeserializer booterDeserializer =
                    new BooterDeserializer( createSurefirePropertiesIfFileExists( tmpDir, surefirePropsFileName ) );
            if ( args.length > 3 )
            {
                final String effectiveSystemPropertiesFileName = args[3];
                setSystemProperties( new File( tmpDir, effectiveSystemPropertiesFileName ) );
            }

            final ProviderConfiguration providerConfiguration = booterDeserializer.deserialize();
            DumpErrorSingleton.getSingleton().init( dumpFileName, providerConfiguration.getReporterConfiguration() );

            final StartupConfiguration startupConfiguration = booterDeserializer.getProviderConfiguration();
            systemExitTimeoutInSeconds =
                    providerConfiguration.systemExitTimeout( DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS );
            final TypeEncodedValue forkedTestSet = providerConfiguration.getTestForFork();
            final boolean readTestsFromInputStream = providerConfiguration.isReadTestsFromInStream();

            final ClasspathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();
            if ( startupConfiguration.isManifestOnlyJarRequestedAndUsable() )
            {
                classpathConfiguration.trickClassPathWhenManifestOnlyClasspath();
            }

            final ClassLoader classLoader = currentThread().getContextClassLoader();
            classLoader.setDefaultAssertionStatus( classpathConfiguration.isEnableAssertions() );
            startupConfiguration.writeSurefireTestClasspathProperty();

            final Object testSet;
            if ( forkedTestSet != null )
            {
                testSet = forkedTestSet.getDecodedValue( classLoader );
            }
            else if ( readTestsFromInputStream )
            {
                testSet = new LazyTestsToRun( eventChannel );
            }
            else
            {
                testSet = null;
            }

            try
            {
                runSuitesInProcess( testSet, startupConfiguration, providerConfiguration, eventChannel );
            }
            catch ( InvocationTargetException t )
            {
                Throwable e = t.getTargetException();
                DumpErrorSingleton.getSingleton().dumpException( t );
                eventChannel.error( new LegacyPojoStackTraceWriter( "test subsystem", "no method", e ), false );
            }
            catch ( Throwable t )
            {
                DumpErrorSingleton.getSingleton().dumpException( t );
                eventChannel.error( new LegacyPojoStackTraceWriter( "test subsystem", "no method", t ), false );
            }
            acknowledgedExit( reader, eventChannel );
        }
        catch ( Throwable t )
        {
            DumpErrorSingleton.getSingleton().dumpException( t );
            // Just throwing does getMessage() and a local trace - we want to call printStackTrace for a full trace
            // noinspection UseOfSystemOutOrSystemErr
            t.printStackTrace( err );
            // noinspection ProhibitedExceptionThrown,CallToSystemExit
            exit( 1 );
        }
        finally
        {
            pingScheduler.cancel( true );
        }
    }

    private static CommandReader startupMasterProcessReader()
    {
        return getReader();
    }

    private static ScheduledFuture<?> listenToShutdownCommands( CommandReader reader )
    {
        reader.addShutdownListener( createExitHandler() );
        AtomicBoolean pingDone = new AtomicBoolean( true );
        reader.addNoopListener( createPingHandler( pingDone ) );
        Runnable pingJob = createPingJob( pingDone );
        return JVM_PING.scheduleAtFixedRate( pingJob, 0, PING_TIMEOUT_IN_SECONDS, SECONDS );
    }

    private static CommandListener createPingHandler( final AtomicBoolean pingDone )
    {
        return new CommandListener()
        {
            public void update( Command command )
            {
                pingDone.set( true );
            }
        };
    }

    private static CommandListener createExitHandler()
    {
        return new CommandListener()
        {
            public void update( Command command )
            {
                Shutdown shutdown = command.toShutdownData();
                if ( shutdown.isKill() )
                {
                    kill();
                }
                else if ( shutdown.isExit() )
                {
                    exit( 1 );
                }
                // else refers to shutdown=testset, but not used now, keeping reader open
            }
        };
    }

    private static Runnable createPingJob( final AtomicBoolean pingDone  )
    {
        return new Runnable()
        {
            public void run()
            {
                boolean hasPing = pingDone.getAndSet( false );
                if ( !hasPing )
                {
                    kill();
                }
            }
        };
    }

    private static void kill()
    {
        Runtime.getRuntime().halt( 1 );
    }

    private static void exit( int returnCode )
    {
        launchLastDitchDaemonShutdownThread( returnCode );
        System.exit( returnCode );
    }

    private static void acknowledgedExit( CommandReader reader, ForkedChannelEncoder eventChannel )
    {
        final Semaphore barrier = new Semaphore( 0 );
        reader.addByeAckListener( new CommandListener()
                                  {
                                      @Override
                                      public void update( Command command )
                                      {
                                          barrier.release();
                                      }
                                  }
        );
        eventChannel.bye();
        launchLastDitchDaemonShutdownThread( 0 );
        long timeoutMillis = max( systemExitTimeoutInSeconds * ONE_SECOND_IN_MILLIS, ONE_SECOND_IN_MILLIS );
        acquireOnePermit( barrier, timeoutMillis );
        System.exit( 0 );
    }

    private static boolean acquireOnePermit( Semaphore barrier, long timeoutMillis )
    {
        try
        {
            return barrier.tryAcquire( timeoutMillis, MILLISECONDS );
        }
        catch ( InterruptedException e )
        {
            return true;
        }
    }

    private static RunResult runSuitesInProcess( Object testSet, StartupConfiguration startupConfiguration,
                                                 ProviderConfiguration providerConfiguration,
                                                 ForkedChannelEncoder eventChannel )
        throws SurefireExecutionException, TestSetFailedException, InvocationTargetException
    {
        final ReporterFactory factory = createForkingReporterFactory( providerConfiguration, eventChannel );

        return invokeProviderInSameClassLoader( testSet, factory, providerConfiguration, true, startupConfiguration,
                                                      false, eventChannel );
    }

    private static ReporterFactory createForkingReporterFactory( ProviderConfiguration providerConfiguration,
                                                                 ForkedChannelEncoder eventChannel )
    {
        final boolean trimStackTrace = providerConfiguration.getReporterConfiguration().isTrimStackTrace();
        return new ForkingReporterFactory( trimStackTrace, eventChannel );
    }

    private static synchronized ScheduledThreadPoolExecutor getJvmTerminator()
    {
        if ( jvmTerminator == null )
        {
            ThreadFactory threadFactory =
                    newDaemonThreadFactory( "last-ditch-daemon-shutdown-thread-" + systemExitTimeoutInSeconds + "s" );
            jvmTerminator = new ScheduledThreadPoolExecutor( 1, threadFactory );
            jvmTerminator.setMaximumPoolSize( 1 );
            return jvmTerminator;
        }
        else
        {
            return jvmTerminator;
        }
    }

    private static ScheduledExecutorService createPingScheduler()
    {
        ThreadFactory threadFactory = newDaemonThreadFactory( "ping-" + PING_TIMEOUT_IN_SECONDS + "s" );
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( 1, threadFactory );
        executor.setMaximumPoolSize( 1 );
        executor.prestartCoreThread();
        return executor;
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    private static void launchLastDitchDaemonShutdownThread( final int returnCode )
    {
        getJvmTerminator().schedule( new Runnable()
                                        {
                                            public void run()
                                            {
                                                Runtime.getRuntime().halt( returnCode );
                                            }
                                        }, systemExitTimeoutInSeconds, SECONDS
        );
    }

    private static RunResult invokeProviderInSameClassLoader( Object testSet, Object factory,
                                                              ProviderConfiguration providerConfig,
                                                              boolean insideFork,
                                                              StartupConfiguration startupConfig,
                                                              boolean restoreStreams,
                                                              ForkedChannelEncoder forkedChannelEncoder )
        throws TestSetFailedException, InvocationTargetException
    {
        final PrintStream orgSystemOut = out;
        final PrintStream orgSystemErr = err;
        // Note that System.out/System.err are also read in the "ReporterConfiguration" instantiation
        // in createProvider below. These are the same values as here.

        try
        {
            return createProviderInCurrentClassloader( startupConfig, insideFork, providerConfig, factory,
                    forkedChannelEncoder ).invoke( testSet );
        }
        finally
        {
            if ( restoreStreams && System.getSecurityManager() == null )
            {
                setOut( orgSystemOut );
                setErr( orgSystemErr );
            }
        }
    }

    private static SurefireProvider createProviderInCurrentClassloader( StartupConfiguration startupConfiguration,
                                                                        boolean isInsideFork,
                                                                        ProviderConfiguration providerConfiguration,
                                                                        Object reporterManagerFactory,
                                                                        ForkedChannelEncoder forkedChannelEncoder )
    {
        BaseProviderFactory bpf = new BaseProviderFactory( (ReporterFactory) reporterManagerFactory, isInsideFork );
        bpf.setTestRequest( providerConfiguration.getTestSuiteDefinition() );
        bpf.setReporterConfiguration( providerConfiguration.getReporterConfiguration() );
        bpf.setForkedChannelEncoder( forkedChannelEncoder );
        ClassLoader classLoader = currentThread().getContextClassLoader();
        bpf.setClassLoaders( classLoader );
        bpf.setTestArtifactInfo( providerConfiguration.getTestArtifact() );
        bpf.setProviderProperties( providerConfiguration.getProviderProperties() );
        bpf.setRunOrderParameters( providerConfiguration.getRunOrderParameters() );
        bpf.setDirectoryScannerParameters( providerConfiguration.getDirScannerParams() );
        bpf.setMainCliOptions( providerConfiguration.getMainCliOptions() );
        bpf.setSkipAfterFailureCount( providerConfiguration.getSkipAfterFailureCount() );
        bpf.setShutdown( providerConfiguration.getShutdown() );
        bpf.setSystemExitTimeout( providerConfiguration.getSystemExitTimeout() );
        String providerClass = startupConfiguration.getActualClassName();
        return (SurefireProvider) instantiateOneArg( classLoader, providerClass, ProviderParameters.class, bpf );
    }

    private static InputStream createSurefirePropertiesIfFileExists( String tmpDir, String propFileName )
            throws FileNotFoundException
    {
        File surefirePropertiesFile = new File( tmpDir, propFileName );
        return surefirePropertiesFile.exists() ? new FileInputStream( surefirePropertiesFile ) : null;
    }
}
