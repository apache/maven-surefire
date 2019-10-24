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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
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
    private static final long ONE_SECOND_IN_MILLIS = 1_000L;
    private static final String LAST_DITCH_SHUTDOWN_THREAD = "surefire-forkedjvm-last-ditch-daemon-shutdown-thread-";
    private static final String PING_THREAD = "surefire-forkedjvm-ping-";

    private final CommandReader commandReader = CommandReader.getReader();
    private final ForkedChannelEncoder eventChannel = new ForkedChannelEncoder( System.out );
    private final Semaphore exitBarrier = new Semaphore( 0 );

    private volatile long systemExitTimeoutInSeconds = DEFAULT_SYSTEM_EXIT_TIMEOUT_IN_SECONDS;
    private volatile PingScheduler pingScheduler;

    private ScheduledThreadPoolExecutor jvmTerminator;
    private ProviderConfiguration providerConfiguration;
    private ForkingReporterFactory forkingReporterFactory;
    private StartupConfiguration startupConfiguration;
    private Object testSet;

    private void setupBooter( String tmpDir, String dumpFileName, String surefirePropsFileName,
                              String effectiveSystemPropertiesFileName )
            throws IOException
    {
        BooterDeserializer booterDeserializer =
                new BooterDeserializer( createSurefirePropertiesIfFileExists( tmpDir, surefirePropsFileName ) );
        setSystemProperties( new File( tmpDir, effectiveSystemPropertiesFileName ) );

        providerConfiguration = booterDeserializer.deserialize();
        DumpErrorSingleton.getSingleton()
                .init( providerConfiguration.getReporterConfiguration().getReportsDirectory(), dumpFileName );

        if ( isDebugging() )
        {
            DumpErrorSingleton.getSingleton()
                    .dumpText( "Found Maven process ID " + booterDeserializer.getPluginPid() );
        }

        startupConfiguration = booterDeserializer.getProviderConfiguration();

        forkingReporterFactory = createForkingReporterFactory();

        ConsoleLogger logger = (ConsoleLogger) forkingReporterFactory.createReporter();
        pingScheduler = isDebugging() ? null : listenToShutdownCommands( booterDeserializer.getPluginPid(), logger );

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
        catch ( InvocationTargetException e )
        {
            Throwable t = e.getTargetException();
            DumpErrorSingleton.getSingleton().dumpException( t );
            eventChannel.consoleErrorLog( new LegacyPojoStackTraceWriter( "test subsystem", "no method", t ), false );
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

    private PingScheduler listenToShutdownCommands( String ppid, ConsoleLogger logger )
    {
        PpidChecker ppidChecker = ppid == null ? null : new PpidChecker( ppid );
        commandReader.addShutdownListener( createExitHandler( ppidChecker ) );
        AtomicBoolean pingDone = new AtomicBoolean( true );
        commandReader.addNoopListener( createPingHandler( pingDone ) );
        PingScheduler pingMechanisms = new PingScheduler( createPingScheduler(), ppidChecker );
        if ( ppidChecker != null )
        {
            logger.debug( ppidChecker.toString() );
        }

        if ( pingMechanisms.pluginProcessChecker != null )
        {
            Runnable checkerJob = processCheckerJob( pingMechanisms );
            pingMechanisms.pingScheduler.scheduleWithFixedDelay( checkerJob, 0L, 1L, SECONDS );
        }
        Runnable pingJob = createPingJob( pingDone, pingMechanisms.pluginProcessChecker );
        pingMechanisms.pingScheduler.scheduleAtFixedRate( pingJob, 0L, PING_TIMEOUT_IN_SECONDS, SECONDS );

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
                                .dumpText( "Killing self fork JVM. Maven process died."
                                        + NL
                                        + "Thread dump before killing the process (" + getProcessName() + "):"
                                        + NL
                                        + generateThreadDump() );

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

    private CommandListener createPingHandler( final AtomicBoolean pingDone )
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

    private CommandListener createExitHandler( final PpidChecker ppidChecker )
    {
        return new CommandListener()
        {
            @Override
            public void update( Command command )
            {
                Shutdown shutdown = command.toShutdownData();
                if ( shutdown.isKill() )
                {
                    ppidChecker.stop();
                    DumpErrorSingleton.getSingleton()
                            .dumpText( "Killing self fork JVM. Received SHUTDOWN command from Maven shutdown hook."
                                    + NL
                                    + "Thread dump before killing the process (" + getProcessName() + "):"
                                    + NL
                                    + generateThreadDump() );
                    kill();
                }
                else if ( shutdown.isExit() )
                {
                    ppidChecker.stop();
                    cancelPingScheduler();
                    DumpErrorSingleton.getSingleton()
                            .dumpText( "Exiting self fork JVM. Received SHUTDOWN command from Maven shutdown hook."
                                    + NL
                                    + "Thread dump before exiting the process (" + getProcessName() + "):"
                                    + NL
                                    + generateThreadDump() );
                    exitBarrier.release();
                    exit1();
                }
                else
                {
                    // else refers to shutdown=testset, but not used now, keeping reader open
                    DumpErrorSingleton.getSingleton()
                            .dumpText( "Thread dump for process (" + getProcessName() + "):"
                                    + NL
                                    + generateThreadDump() );
                }
            }
        };
    }

    private Runnable createPingJob( final AtomicBoolean pingDone, final PpidChecker pluginProcessChecker  )
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                if ( !canUseNewPingMechanism( pluginProcessChecker ) )
                {
                    boolean hasPing = pingDone.getAndSet( false );
                    if ( !hasPing )
                    {
                        DumpErrorSingleton.getSingleton()
                                .dumpText( "Killing self fork JVM. PING timeout elapsed."
                                        + NL
                                        + "Thread dump before killing the process (" + getProcessName() + "):"
                                        + NL
                                        + generateThreadDump() );

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
        commandReader.addByeAckListener( new CommandListener()
                                          {
                                              @Override
                                              public void update( Command command )
                                              {
                                                  exitBarrier.release();
                                              }
                                          }
        );
        eventChannel.bye();
        launchLastDitchDaemonShutdownThread( 0 );
        long timeoutMillis = max( systemExitTimeoutInSeconds * ONE_SECOND_IN_MILLIS, ONE_SECOND_IN_MILLIS );
        acquireOnePermit( exitBarrier, timeoutMillis );
        cancelPingScheduler();
        commandReader.stop();
        System.exit( 0 );
    }

    private void runSuitesInProcess()
        throws TestSetFailedException, InvocationTargetException
    {
        createProviderInCurrentClassloader( forkingReporterFactory ).invoke( testSet );
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
        getJvmTerminator()
                .schedule( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        DumpErrorSingleton.getSingleton()
                                .dumpText( "Thread dump for process ("
                                        + getProcessName()
                                        + ") after "
                                        + systemExitTimeoutInSeconds
                                        + " seconds shutdown timeout:"
                                        + NL
                                        + generateThreadDump() );

                        kill( returnCode );
                    }
                }, systemExitTimeoutInSeconds, SECONDS
        );
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
    public static void main( String[] args )
    {
        ForkedBooter booter = new ForkedBooter();
        run( booter, args );
    }

    /**
     * created for testing purposes.
     *
     * @param booter booter in JVM
     * @param args arguments passed to JVM
     */
    private static void run( ForkedBooter booter, String[] args )
    {
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

    private static String generateThreadDump()
    {
        StringBuilder dump = new StringBuilder();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo( threadMXBean.getAllThreadIds(), 100 );
        for ( ThreadInfo threadInfo : threadInfos )
        {
            dump.append( '"' );
            dump.append( threadInfo.getThreadName() );
            dump.append( "\" " );
            Thread.State state = threadInfo.getThreadState();
            dump.append( "\n   java.lang.Thread.State: " );
            dump.append( state );
            StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            for ( StackTraceElement stackTraceElement : stackTraceElements )
            {
                dump.append( "\n        at " );
                dump.append( stackTraceElement );
            }
            dump.append( "\n\n" );
        }
        return dump.toString();
    }

    private static String getProcessName()
    {
        return ManagementFactory.getRuntimeMXBean()
                .getName();
    }
}
