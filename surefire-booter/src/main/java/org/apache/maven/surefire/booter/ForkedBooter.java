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
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.max;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.booter.SystemPropertyManager.setSystemProperties;
import static org.apache.maven.surefire.util.ReflectionUtils.instantiateOneArg;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThreadFactory;
import static org.apache.maven.surefire.util.internal.StringUtils.NL;

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
    private static final long DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS = 30L;
    private static final long PING_TIMEOUT_IN_SECONDS = 30L;
    private static final long ONE_SECOND_IN_MILLIS = 1000L;
    private static final String LAST_DITCH_SHUTDOWN_THREAD = "surefire-forkedjvm-last-ditch-daemon-shutdown-thread-";
    private static final String PING_THREAD = "surefire-forkedjvm-ping-";
    private static final double GC_FACTOR = 0.75d;

    private final CommandReader commandReader = CommandReader.getReader();
    private final ForkedChannelEncoder eventChannel = new ForkedChannelEncoder( System.out );

    private volatile long systemExitTimeoutInSeconds = DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS;
    private volatile PingScheduler pingScheduler;

    private ScheduledThreadPoolExecutor jvmTerminator;
    private ProviderConfiguration providerConfiguration;
    private StartupConfiguration startupConfiguration;
    private Object testSet;

    private void setupBooter( String tmpDir, String dumpFileName, String surefirePropsFileName,
                              String effectiveSystemPropertiesFileName )
            throws IOException
    {
        BooterDeserializer booterDeserializer =
                new BooterDeserializer( createSurefirePropertiesIfFileExists( tmpDir, surefirePropsFileName ) );
        // todo: print PID in debug console logger in version 2.21.2
        pingScheduler = isDebugging() ? null : listenToShutdownCommands( booterDeserializer.getPluginPid() );
        setSystemProperties( new File( tmpDir, effectiveSystemPropertiesFileName ) );

        providerConfiguration = booterDeserializer.deserialize();
        DumpErrorSingleton.getSingleton()
                .init( providerConfiguration.getReporterConfiguration().getReportsDirectory(), dumpFileName );

        startupConfiguration = booterDeserializer.getProviderConfiguration();
        systemExitTimeoutInSeconds = providerConfiguration.systemExitTimeout( DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS );

        AbstractPathConfiguration classpathConfiguration = startupConfiguration.getClasspathConfiguration();

        if ( classpathConfiguration.isClassPathConfig() )
        {
            if ( startupConfiguration.isManifestOnlyJarRequestedAndUsable() )
            {
                classpathConfiguration.toRealPath( ClasspathConfiguration.class )
                        .trickClassPathWhenManifestOnlyClasspath();
            }
            startupConfiguration.writeSurefireTestClasspathProperty();
        }

        ClassLoader classLoader = currentThread().getContextClassLoader();
        classLoader.setDefaultAssertionStatus( classpathConfiguration.isEnableAssertions() );
        boolean readTestsFromCommandReader = providerConfiguration.isReadTestsFromInStream();
        testSet = createTestSet( providerConfiguration.getTestForFork(), readTestsFromCommandReader, classLoader );
    }

    private void execute()
    {
        try
        {
            runSuitesInProcess();
        }
        catch ( InvocationTargetException t )
        {
            Throwable e = t.getTargetException();
            DumpErrorSingleton.getSingleton().dumpException( e );
            eventChannel.consoleErrorLog( new LegacyPojoStackTraceWriter( "test subsystem", "no method", e ), false );
        }
        catch ( Throwable t )
        {
            DumpErrorSingleton.getSingleton().dumpException( t );
            eventChannel.consoleErrorLog( new LegacyPojoStackTraceWriter( "test subsystem", "no method", t ), false );
        }
        finally
        {
            acknowledgedExit();
        }
    }

    private Object createTestSet( TypeEncodedValue forkedTestSet, boolean readTestsFromCommandReader, ClassLoader cl )
    {
        if ( forkedTestSet != null )
        {
            return forkedTestSet.getDecodedValue( cl );
        }
        else if ( readTestsFromCommandReader )
        {
            return new LazyTestsToRun( eventChannel );
        }
        return null;
    }

    private void cancelPingScheduler()
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

    private PingScheduler listenToShutdownCommands( Long ppid )
    {
        commandReader.addShutdownListener( createExitHandler() );
        PingPeriod pingPeriod = new PingPeriod();
        commandReader.addNoopListener( createPingHandler( pingPeriod ) );

        PingScheduler pingMechanisms = new PingScheduler( createPingScheduler(),
                                                          ppid == null ? null : new PpidChecker( ppid ) );
        if ( pingMechanisms.pluginProcessChecker != null )
        {
            Runnable checkerJob = processCheckerJob( pingMechanisms );
            pingMechanisms.pingScheduler.scheduleWithFixedDelay( checkerJob, 0L, 1L, SECONDS );
        }
        Runnable pingJob = createPingJob( pingPeriod, pingMechanisms.pluginProcessChecker );
        pingMechanisms.pingScheduler.scheduleWithFixedDelay( pingJob, 0L, PING_TIMEOUT_IN_SECONDS, SECONDS );

        return pingMechanisms;
    }

    private Runnable processCheckerJob( final PingScheduler pingMechanism )
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if ( pingMechanism.pluginProcessChecker.canUse()
                                 && !pingMechanism.pluginProcessChecker.isProcessAlive()
                                 && !pingMechanism.pingScheduler.isShutdown() )
                    {
                        DumpErrorSingleton.getSingleton()
                                .dumpText( "Killing self fork JVM. Maven process died." );

                        kill();
                    }
                }
                catch ( RuntimeException e )
                {
                    DumpErrorSingleton.getSingleton()
                            .dumpException( e, "System.exit() or native command error interrupted process checker." );
                }
            }
        };
    }

    private CommandListener createPingHandler( final PingPeriod pingPeriod )
    {
        return new CommandListener()
        {
            @Override
            public void update( Command command )
            {
                pingPeriod.pingDone.set( true );
            }
        };
    }

    private CommandListener createExitHandler()
    {
        return new CommandListener()
        {
            @Override
            public void update( Command command )
            {
                Shutdown shutdown = command.toShutdownData();
                if ( shutdown.isKill() )
                {
                    DumpErrorSingleton.getSingleton()
                            .dumpText( "Killing self fork JVM. Received SHUTDOWN command from Maven shutdown hook." );
                    kill();
                }
                else if ( shutdown.isExit() )
                {
                    cancelPingScheduler();
                    DumpErrorSingleton.getSingleton()
                            .dumpText( "Exiting self fork JVM. Received SHUTDOWN command from Maven shutdown hook." );
                    exit1();
                }
                // else refers to shutdown=testset, but not used now, keeping reader open
            }
        };
    }

    private Runnable createPingJob( final PingPeriod pingPeriod, final PpidChecker pluginProcessChecker  )
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                if ( !canUseNewPingMechanism( pluginProcessChecker ) )
                {
                    final long lastGcPeriod = pingPeriod.lastGcPeriod();
                    final boolean longGcPausesDetected = lastGcPeriod > GC_FACTOR * PING_TIMEOUT_IN_SECONDS;
                    final boolean hasPing = pingPeriod.pingDone.getAndSet( false );
                    if ( !longGcPausesDetected && hasPing )
                    {
                        DumpErrorSingleton.getSingleton()
                                .dumpText( "Killing self fork JVM. PING timeout elapsed."
                                        + NL
                                        + "lastGcPeriod = " + lastGcPeriod + " millis" );

                        kill();
                    }
                }
            }
        };
    }

    private void kill()
    {
        kill( 1 );
    }

    private void kill( int returnCode )
    {
        commandReader.stop();
        Runtime.getRuntime().halt( returnCode );
    }

    private void exit1()
    {
        launchLastDitchDaemonShutdownThread( 1 );
        System.exit( 1 );
    }

    private void acknowledgedExit()
    {
        final Semaphore barrier = new Semaphore( 0 );
        commandReader.addByeAckListener( new CommandListener()
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
        cancelPingScheduler();
        commandReader.stop();
        System.exit( 0 );
    }

    private void runSuitesInProcess()
        throws TestSetFailedException, InvocationTargetException
    {
        ForkingReporterFactory factory = createForkingReporterFactory();
        invokeProviderInSameClassLoader( factory );
    }

    private ForkingReporterFactory createForkingReporterFactory()
    {
        final boolean trimStackTrace = providerConfiguration.getReporterConfiguration().isTrimStackTrace();
        return new ForkingReporterFactory( trimStackTrace, eventChannel );
    }

    private synchronized ScheduledThreadPoolExecutor getJvmTerminator()
    {
        if ( jvmTerminator == null )
        {
            ThreadFactory threadFactory =
                    newDaemonThreadFactory( LAST_DITCH_SHUTDOWN_THREAD + systemExitTimeoutInSeconds + "s" );
            jvmTerminator = new ScheduledThreadPoolExecutor( 1, threadFactory );
            jvmTerminator.setMaximumPoolSize( 1 );
        }
        return jvmTerminator;
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    private void launchLastDitchDaemonShutdownThread( final int returnCode )
    {
        getJvmTerminator().schedule( new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                kill( returnCode );
                                            }
                                        }, systemExitTimeoutInSeconds, SECONDS
        );
    }

    private void invokeProviderInSameClassLoader( ForkingReporterFactory factory )
        throws TestSetFailedException, InvocationTargetException
    {
        createProviderInCurrentClassloader( factory ).invoke( testSet );
    }

    private SurefireProvider createProviderInCurrentClassloader( ForkingReporterFactory reporterManagerFactory )
    {
        BaseProviderFactory bpf = new BaseProviderFactory( reporterManagerFactory, true );
        bpf.setTestRequest( providerConfiguration.getTestSuiteDefinition() );
        bpf.setReporterConfiguration( providerConfiguration.getReporterConfiguration() );
        bpf.setForkedChannelEncoder( eventChannel );
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

    /**
     * This method is invoked when Surefire is forked - this method parses and organizes the arguments passed to it and
     * then calls the Surefire class' run method. <br> The system exit code will be 1 if an exception is thrown.
     *
     * @param args Commandline arguments
     */
    public static void main( String... args )
    {
        ForkedBooter booter = new ForkedBooter();
        try
        {
            booter.setupBooter( args[0], args[1], args[2], args.length > 3 ? args[3] : null );
            booter.execute();
        }
        catch ( Throwable t )
        {
            DumpErrorSingleton.getSingleton().dumpException( t );
            t.printStackTrace();
            booter.cancelPingScheduler();
            booter.exit1();
        }
    }

    private static boolean canUseNewPingMechanism( PpidChecker pluginProcessChecker )
    {
        return pluginProcessChecker != null && pluginProcessChecker.canUse();
    }

    private static void acquireOnePermit( Semaphore barrier, long timeoutMillis )
    {
        try
        {
            barrier.tryAcquire( timeoutMillis, MILLISECONDS );
        }
        catch ( InterruptedException e )
        {
            // cancel schedulers, stop the command reader and exit 0
        }
    }

    private static ScheduledExecutorService createPingScheduler()
    {
        ThreadFactory threadFactory = newDaemonThreadFactory( PING_THREAD + PING_TIMEOUT_IN_SECONDS + "s" );
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( 1, threadFactory );
        executor.setKeepAliveTime( 3L, SECONDS );
        executor.setMaximumPoolSize( 2 );
        return executor;
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

    private static class PingScheduler
    {
        private final ScheduledExecutorService pingScheduler;
        private final PpidChecker pluginProcessChecker;

        PingScheduler( ScheduledExecutorService pingScheduler, PpidChecker pluginProcessChecker )
        {
            this.pingScheduler = pingScheduler;
            this.pluginProcessChecker = pluginProcessChecker;
        }

        void shutdown()
        {
            pingScheduler.shutdown();
            if ( pluginProcessChecker != null )
            {
                pluginProcessChecker.destroyActiveCommands();
            }
        }

        boolean isShutdown()
        {
            return pingScheduler.isShutdown();
        }
    }

    private static class PingPeriod
    {
        private final AtomicBoolean pingDone = new AtomicBoolean();
        private volatile long accumulatedCollectionElapsedTime;

        private PingPeriod()
        {
            lastGcPeriod();
        }

        private long lastGcPeriod()
        {
            final long lastAccumulatedGcTime = accumulatedCollectionElapsedTime;
            long currentAccumulatedGcTime = 0L;
            for ( GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans() )
            {
                currentAccumulatedGcTime = max( currentAccumulatedGcTime, gc.getCollectionTime() );
            }
            accumulatedCollectionElapsedTime = currentAccumulatedGcTime;
            return currentAccumulatedGcTime - lastAccumulatedGcTime;
        }
    }
}
