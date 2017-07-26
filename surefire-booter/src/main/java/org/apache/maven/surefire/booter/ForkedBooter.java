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
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
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
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_ERROR;
import static org.apache.maven.surefire.booter.ForkingRunListener.encode;
import static org.apache.maven.surefire.booter.SystemPropertyManager.setSystemProperties;
import static org.apache.maven.surefire.util.ReflectionUtils.instantiateOneArg;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThreadFactory;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;

/**
 * The part of the booter that is unique to a forked vm.
 * <br>
 * Deals with deserialization of the booter wire-level protocol
 * <br>
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
    private static final CommandReader COMMAND_READER = startupMasterProcessReader();

    private static volatile ScheduledThreadPoolExecutor jvmTerminator;
    private static volatile long systemExitTimeoutInSeconds = DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS;

    /**
     * This method is invoked when Surefire is forked - this method parses and organizes the arguments passed to it and
     * then calls the Surefire class' run method. <br> The system exit code will be 1 if an exception is thrown.
     *
     * @param args Commandline arguments
     */
    public static void main( String... args )
    {
        final ExecutorService pingScheduler = isDebugging() ? null : listenToShutdownCommands();
        final PrintStream originalOut = out;
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
                testSet = new LazyTestsToRun( originalOut );
            }
            else
            {
                testSet = null;
            }

            try
            {
                runSuitesInProcess( testSet, startupConfiguration, providerConfiguration, originalOut );
            }
            catch ( InvocationTargetException t )
            {
                DumpErrorSingleton.getSingleton().dumpException( t );
                StackTraceWriter stackTraceWriter =
                    new LegacyPojoStackTraceWriter( "test subsystem", "no method", t.getTargetException() );
                StringBuilder stringBuilder = new StringBuilder();
                encode( stringBuilder, stackTraceWriter, false );
                encodeAndWriteToOutput( ( (char) BOOTERCODE_ERROR ) + ",0," + stringBuilder + "\n" , originalOut );
            }
            catch ( Throwable t )
            {
                DumpErrorSingleton.getSingleton().dumpException( t );
                StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( "test subsystem", "no method", t );
                StringBuilder stringBuilder = new StringBuilder();
                encode( stringBuilder, stackTraceWriter, false );
                encodeAndWriteToOutput( ( (char) BOOTERCODE_ERROR ) + ",0," + stringBuilder + "\n", originalOut );
            }
            acknowledgedExit( originalOut, pingScheduler );
        }
        catch ( Throwable t )
        {
            DumpErrorSingleton.getSingleton().dumpException( t );
            // Just throwing does getMessage() and a local trace - we want to call printStackTrace for a full trace
            // noinspection UseOfSystemOutOrSystemErr
            t.printStackTrace( err );
            cancelPingScheduler( pingScheduler );
            // noinspection ProhibitedExceptionThrown,CallToSystemExit
            exit( 1 );
        }
    }

    private static void cancelPingScheduler( final ExecutorService pingScheduler )
    {
        if ( pingScheduler != null )
        {
            try
            {
                AccessController.doPrivileged( new PrivilegedAction<Object>()
                                               {
                                                   @Override
                                                   public Object run()
                                                   {
                                                       pingScheduler.shutdown();
                                                       return null;
                                                   }
                                               }
                );
            }
            catch ( AccessControlException e )
            {
                // ignore
            }
        }
    }

    private static CommandReader startupMasterProcessReader()
    {
        return getReader();
    }

    private static ExecutorService listenToShutdownCommands()
    {
        COMMAND_READER.addShutdownListener( createExitHandler() );
        AtomicBoolean pingDone = new AtomicBoolean( true );
        COMMAND_READER.addNoopListener( createPingHandler( pingDone ) );
        Runnable pingJob = createPingJob( pingDone );
        ScheduledExecutorService pingScheduler = createPingScheduler();
        pingScheduler.scheduleAtFixedRate( pingJob, 0, PING_TIMEOUT_IN_SECONDS, SECONDS );
        return pingScheduler;
    }

    private static CommandListener createPingHandler( final AtomicBoolean pingDone )
    {
        return new CommandListener()
        {
            @Override
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
            @Override
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
            @Override
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

    private static void encodeAndWriteToOutput( String string, PrintStream out )
    {
        byte[] encodeBytes = encodeStringForForkCommunication( string );
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized ( out )
        {
            out.write( encodeBytes, 0, encodeBytes.length );
            out.flush();
        }
    }

    private static void kill()
    {
        COMMAND_READER.stop();
        Runtime.getRuntime().halt( 1 );
    }

    private static void exit( int returnCode )
    {
        launchLastDitchDaemonShutdownThread( returnCode );
        COMMAND_READER.stop();
        System.exit( returnCode );
    }

    private static void acknowledgedExit( PrintStream originalOut, ExecutorService pingScheduler )
    {
        final Semaphore barrier = new Semaphore( 0 );
        COMMAND_READER.addByeAckListener( new CommandListener()
                                  {
                                      @Override
                                      public void update( Command command )
                                      {
                                          barrier.release();
                                      }
                                  }
        );
        encodeAndWriteToOutput( ( (char) BOOTERCODE_BYE ) + ",0,BYE!\n", originalOut );
        launchLastDitchDaemonShutdownThread( 0 );
        long timeoutMillis = max( systemExitTimeoutInSeconds * ONE_SECOND_IN_MILLIS, ONE_SECOND_IN_MILLIS );
        acquireOnePermit( barrier, timeoutMillis );
        cancelPingScheduler( pingScheduler );
        COMMAND_READER.stop();
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
                                                 PrintStream originalSystemOut )
        throws SurefireExecutionException, TestSetFailedException, InvocationTargetException
    {
        final ReporterFactory factory = createForkingReporterFactory( providerConfiguration, originalSystemOut );

        return invokeProviderInSameClassLoader( testSet, factory, providerConfiguration, true, startupConfiguration,
                                                      false );
    }

    private static ReporterFactory createForkingReporterFactory( ProviderConfiguration providerConfiguration,
                                                                 PrintStream originalSystemOut )
    {
        final boolean trimStackTrace = providerConfiguration.getReporterConfiguration().isTrimStackTrace();
        return new ForkingReporterFactory( trimStackTrace, originalSystemOut );
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
                                            @Override
                                            public void run()
                                            {
                                                COMMAND_READER.stop();
                                                Runtime.getRuntime().halt( returnCode );
                                            }
                                        }, systemExitTimeoutInSeconds, SECONDS
        );
    }

    private static RunResult invokeProviderInSameClassLoader( Object testSet, Object factory,
                                                              ProviderConfiguration providerConfig,
                                                              boolean insideFork,
                                                              StartupConfiguration startupConfig,
                                                              boolean restoreStreams )
        throws TestSetFailedException, InvocationTargetException
    {
        final PrintStream orgSystemOut = out;
        final PrintStream orgSystemErr = err;
        // Note that System.out/System.err are also read in the "ReporterConfiguration" instantiation
        // in createProvider below. These are the same values as here.

        try
        {
            return createProviderInCurrentClassloader( startupConfig, insideFork, providerConfig, factory )
                           .invoke( testSet );
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
                                                                       Object reporterManagerFactory )
    {
        BaseProviderFactory bpf = new BaseProviderFactory( (ReporterFactory) reporterManagerFactory, isInsideFork );
        bpf.setTestRequest( providerConfiguration.getTestSuiteDefinition() );
        bpf.setReporterConfiguration( providerConfiguration.getReporterConfiguration() );
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

    private static boolean isDebugging()
    {
        for ( String argument : ManagementFactory.getRuntimeMXBean().getInputArguments() )
        {
            if ( "-Xdebug".equals( argument ) || argument.startsWith( "-agentlib:jdwp" ) )
            {
                return true;
            }
        }
        return false;
    }
}
