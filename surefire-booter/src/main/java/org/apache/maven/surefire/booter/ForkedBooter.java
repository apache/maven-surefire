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
import org.apache.maven.surefire.api.booter.BaseProviderFactory;
import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.booter.ForkingReporterFactory;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.provider.CommandListener;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.provider.SurefireProvider;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.booter.spi.SurefireMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils;
import org.apache.maven.surefire.spi.MasterProcessChannelProcessorFactory;

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

import static java.lang.Thread.currentThread;
import static java.util.ServiceLoader.load;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.api.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.api.util.ReflectionUtils.instantiateOneArg;
import static org.apache.maven.surefire.api.util.internal.DaemonThreadFactory.newDaemonThreadFactory;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.apache.maven.surefire.booter.ProcessCheckerType.NATIVE;
import static org.apache.maven.surefire.booter.ProcessCheckerType.PING;
import static org.apache.maven.surefire.booter.SystemPropertyManager.setSystemProperties;

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
    private static final String LAST_DITCH_SHUTDOWN_THREAD = "surefire-forkedjvm-last-ditch-daemon-shutdown-thread-";
    private static final String PING_THREAD = "surefire-forkedjvm-ping-";
    private static final String PROCESS_CHECKER_THREAD = "surefire-process-checker";
    private static final String PROCESS_PIPES_ERROR =
        "The channel (std/out or TCP/IP) failed to send a stream from this subprocess.";

    private final Semaphore exitBarrier = new Semaphore( 0 );

    private volatile MasterProcessChannelEncoder eventChannel;
    private volatile ConsoleLogger logger;
    private volatile MasterProcessChannelProcessorFactory channelProcessorFactory;
    private volatile CommandReader commandReader;
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

        int forkNumber = booterDeserializer.getForkNumber();

        if ( isDebugging() )
        {
            DumpErrorSingleton.getSingleton()
                    .dumpText( "Found Maven process ID " + booterDeserializer.getPluginPid()
                        + " for the fork " + forkNumber + "." );
        }

        startupConfiguration = booterDeserializer.getStartupConfiguration();

        String channelConfig = booterDeserializer.getConnectionString();
        channelProcessorFactory = lookupDecoderFactory( channelConfig );
        channelProcessorFactory.connect( channelConfig );
        boolean isDebugging = isDebugging();
        boolean debug = isDebugging || providerConfiguration.getMainCliOptions().contains( LOGGING_LEVEL_DEBUG );
        ForkNodeArguments args = new ForkedNodeArg( forkNumber, debug );
        eventChannel = channelProcessorFactory.createEncoder( args );
        MasterProcessChannelDecoder decoder = channelProcessorFactory.createDecoder( args );

        flushEventChannelOnExit();

        forkingReporterFactory = createForkingReporterFactory();
        logger = forkingReporterFactory.createTestReportListener();
        commandReader = new CommandReader( decoder, providerConfiguration.getShutdown(), logger );

        pingScheduler = isDebugging ? null : listenToShutdownCommands( booterDeserializer.getPluginPid() );

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
        catch ( Throwable t )
        {
            Throwable e =
                t instanceof InvocationTargetException ? ( (InvocationTargetException) t ).getTargetException() : t;
            DumpErrorSingleton.getSingleton().dumpException( e );
            logger.error( e.getLocalizedMessage(), e );
        }
        finally
        {
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();

            if ( eventChannel.checkError() )
            {
                DumpErrorSingleton.getSingleton().dumpText( PROCESS_PIPES_ERROR );
                logger.error( PROCESS_PIPES_ERROR );
            }

            // process pipes are closed far here
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
            return new LazyTestsToRun( eventChannel, commandReader );
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

    private void closeForkChannel()
    {
        if ( channelProcessorFactory != null )
        {
            try
            {
                channelProcessorFactory.close();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }

    private PingScheduler listenToShutdownCommands( String ppid )
    {
        PpidChecker ppidChecker = ppid == null ? null : new PpidChecker( ppid );
        commandReader.addShutdownListener( createExitHandler( ppidChecker ) );
        AtomicBoolean pingDone = new AtomicBoolean( true );
        commandReader.addNoopListener( createPingHandler( pingDone ) );
        PingScheduler pingMechanisms = new PingScheduler(
            createScheduler( PING_THREAD + PING_TIMEOUT_IN_SECONDS + "s" ),
            createScheduler( PROCESS_CHECKER_THREAD ),
            ppidChecker );

        ProcessCheckerType checkerType = startupConfiguration.getProcessChecker();

        if ( ( checkerType == ALL || checkerType == NATIVE ) && pingMechanisms.processChecker != null )
        {
            logger.debug( pingMechanisms.processChecker.toString() );
            if ( pingMechanisms.processChecker.canUse() )
            {
                Runnable checkerJob = processCheckerJob( pingMechanisms );
                pingMechanisms.processCheckerScheduler.scheduleWithFixedDelay( checkerJob, 0L, 1L, SECONDS );
            }
            else if ( !pingMechanisms.processChecker.isStopped() )
            {
                logger.warning( "Cannot use process checker with configuration " + checkerType
                    + ". Platform not supported." );
            }
        }

        if ( checkerType == ALL || checkerType == PING )
        {
            Runnable pingJob = createPingJob( pingDone, pingMechanisms.processChecker );
            pingMechanisms.pingScheduler.scheduleWithFixedDelay( pingJob, 0L, PING_TIMEOUT_IN_SECONDS, SECONDS );
        }

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
                    if ( pingMechanism.processChecker.canUse()
                                 && !pingMechanism.processChecker.isProcessAlive()
                                 && !pingMechanism.pingScheduler.isShutdown() )
                    {
                        logger.error( "Surefire is going to kill self fork JVM. Maven process died." );
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
                    if ( ppidChecker != null )
                    {
                        ppidChecker.stop();
                    }

                    logger.error( "Surefire is going to kill self fork JVM. "
                        + "Received SHUTDOWN {" + shutdown + "} command from Maven shutdown hook." );
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
                    if ( ppidChecker != null )
                    {
                        ppidChecker.stop();
                    }
                    cancelPingScheduler();
                    logger.error( "Surefire is going to exit self fork JVM. "
                        + "Received SHUTDOWN {" + shutdown + "} command from Maven shutdown hook." );
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
                        logger.error( "Killing self fork JVM. PING timeout elapsed." );
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
        closeForkChannel();
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
        boolean byeAckReceived = acquireOnePermit( exitBarrier );
        if ( !byeAckReceived && !eventChannel.checkError() )
        {
            eventChannel.sendExitError( null, false );
        }
        cancelPingScheduler();
        commandReader.stop();
        closeForkChannel();
        System.exit( 0 );
    }

    private void runSuitesInProcess()
        throws TestSetFailedException, InvocationTargetException
    {
        createProviderInCurrentClassloader().invoke( testSet );
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
                        if ( logger != null )
                        {
                            logger.error( "Surefire is going to kill self fork JVM. The exit has elapsed "
                                + systemExitTimeoutInSeconds + " seconds after System.exit(" + returnCode + ")." );
                        }

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

    private SurefireProvider createProviderInCurrentClassloader()
    {
        BaseProviderFactory bpf = new BaseProviderFactory( true );
        bpf.setReporterFactory( forkingReporterFactory );
        bpf.setCommandReader( commandReader );
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
        bpf.setSystemExitTimeout( providerConfiguration.getSystemExitTimeout() );
        String providerClass = startupConfiguration.getActualClassName();
        return instantiateOneArg( classLoader, providerClass, ProviderParameters.class, bpf );
    }

    /**
     * Necessary for the Surefire817SystemExitIT.
     */
    private void flushEventChannelOnExit()
    {
        Runnable target = new Runnable()
        {
            @Override
            public void run()
            {
                eventChannel.onJvmExit();
            }
        };
        Thread t = new Thread( target );
        t.setDaemon( true );
        ShutdownHookUtils.addShutDownHook( t );
    }

    private static MasterProcessChannelProcessorFactory lookupDecoderFactory( String channelConfig )
    {
        MasterProcessChannelProcessorFactory defaultFactory = null;
        MasterProcessChannelProcessorFactory customFactory = null;
        for ( MasterProcessChannelProcessorFactory factory : load( MasterProcessChannelProcessorFactory.class ) )
        {
            Class<?> cls = factory.getClass();

            boolean isSurefireFactory =
                cls == LegacyMasterProcessChannelProcessorFactory.class
                    || cls == SurefireMasterProcessChannelProcessorFactory.class;

            if ( isSurefireFactory )
            {
                if ( factory.canUse( channelConfig ) )
                {
                    defaultFactory = factory;
                }
            }
            else
            {
                customFactory = factory;
            }
        }
        return customFactory != null ? customFactory : defaultFactory;
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
            if ( booter.logger != null )
            {
                booter.logger.error( t.getLocalizedMessage(), t );
            }
            booter.cancelPingScheduler();
            booter.exit1();
        }
    }

    private static boolean canUseNewPingMechanism( PpidChecker pluginProcessChecker )
    {
        return pluginProcessChecker != null && pluginProcessChecker.canUse();
    }

    private static boolean acquireOnePermit( Semaphore barrier )
    {
        try
        {
            return barrier.tryAcquire( Integer.MAX_VALUE, SECONDS );
        }
        catch ( InterruptedException e )
        {
            // cancel schedulers, stop the command reader and exit 0
            return false;
        }
    }

    private static ScheduledExecutorService createScheduler( String threadName )
    {
        ThreadFactory threadFactory = newDaemonThreadFactory( threadName );
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor( 1, threadFactory );
        executor.setMaximumPoolSize( executor.getCorePoolSize() );
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
        private final ScheduledExecutorService processCheckerScheduler;
        private final PpidChecker processChecker;

        PingScheduler( ScheduledExecutorService pingScheduler, ScheduledExecutorService processCheckerScheduler,
                       PpidChecker processChecker )
        {
            this.pingScheduler = pingScheduler;
            this.processCheckerScheduler = processCheckerScheduler;
            this.processChecker = processChecker;
        }

        void shutdown()
        {
            pingScheduler.shutdown();
            processCheckerScheduler.shutdown();
            if ( processChecker != null )
            {
                processChecker.destroyActiveCommands();
            }
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
