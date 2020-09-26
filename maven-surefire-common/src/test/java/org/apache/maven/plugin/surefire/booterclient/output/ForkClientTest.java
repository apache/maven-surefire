package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.plugin.surefire.booterclient.MockReporter;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.extensions.EventConsumerThread;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.event.ConsoleDebugEvent;
import org.apache.maven.surefire.api.event.ConsoleErrorEvent;
import org.apache.maven.surefire.api.event.ConsoleInfoEvent;
import org.apache.maven.surefire.api.event.ConsoleWarningEvent;
import org.apache.maven.surefire.api.event.ControlByeEvent;
import org.apache.maven.surefire.api.event.ControlNextTestEvent;
import org.apache.maven.surefire.api.event.ControlStopOnNextTestEvent;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.event.StandardStreamErrEvent;
import org.apache.maven.surefire.api.event.StandardStreamErrWithNewLineEvent;
import org.apache.maven.surefire.api.event.StandardStreamOutEvent;
import org.apache.maven.surefire.api.event.StandardStreamOutWithNewLineEvent;
import org.apache.maven.surefire.api.event.SystemPropertyEvent;
import org.apache.maven.surefire.api.event.TestAssumptionFailureEvent;
import org.apache.maven.surefire.api.event.TestErrorEvent;
import org.apache.maven.surefire.api.event.TestFailedEvent;
import org.apache.maven.surefire.api.event.TestSkippedEvent;
import org.apache.maven.surefire.api.event.TestStartingEvent;
import org.apache.maven.surefire.api.event.TestSucceededEvent;
import org.apache.maven.surefire.api.event.TestsetCompletedEvent;
import org.apache.maven.surefire.api.event.TestsetStartingEvent;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.Channels.newChannel;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.CONSOLE_DEBUG;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.CONSOLE_ERR;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.CONSOLE_INFO;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.CONSOLE_WARN;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.SET_COMPLETED;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.SET_STARTING;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.STDERR;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.STDOUT;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.TEST_ASSUMPTION_FAIL;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.TEST_ERROR;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.TEST_FAILED;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.TEST_SKIPPED;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.TEST_STARTING;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.TEST_SUCCEEDED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_BYE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_ERROR;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ForkClient}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class ForkClientTest
{
    private static final int ELAPSED_TIME = 102;

    @Test( expected = NullPointerException.class )
    public void shouldFailOnNPE()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        ForkClient client = new ForkClient( factory, null, 0 );
        client.handleEvent( null );
    }

    @Test
    public void shouldLogJvmMessage() throws Exception
    {
        String nativeStream = "Listening for transport dt_socket at address: bla";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        when( logger.isDebugEnabled() ).thenReturn( false );
        when( logger.isInfoEnabled() ).thenReturn( true );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .info( "Listening for transport dt_socket at address: bla" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verify( logger ).isDebugEnabled();

        verify( logger ).isInfoEnabled();

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmError1() throws Exception
    {
        String nativeStream = "\nCould not create the Java Virtual Machine\n";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .error( "Could not create the Java Virtual Machine" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmError2() throws Exception
    {
        String nativeStream = "\nError occurred during initialization of VM\n";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .error( "Error occurred during initialization of VM" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmError3() throws Exception
    {
        String nativeStream = "\nError: A fatal exception has occurred. Program will exit.\n";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .error( "Error: A fatal exception has occurred. Program will exit." );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmError4() throws Exception
    {
        String nativeStream = "\nCould not reserve enough space for object heap\n";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .error( "Could not reserve enough space for object heap" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmError5() throws Exception
    {
        String nativeStream = "\njava.lang.module.FindException: Module java.ws.rs not found, required by com.foo.api";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .error( "java.lang.module.FindException: Module java.ws.rs not found, required by com.foo.api" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmError6() throws Exception
    {
        String nativeStream = "\njava.lang.module.FindException: Module java.ws.rs not found, required by com.foo.api";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .error( "java.lang.module.FindException: Module java.ws.rs not found, required by com.foo.api" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogUnorderedErrors() throws Exception
    {
        String nativeStream = "unordered error";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        when( logger.isDebugEnabled() )
            .thenReturn( true );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            countdown.awaitClosed();

            verify( logger )
                .debug( "unordered error" );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verify( logger ).isDebugEnabled();

        ArgumentCaptor<String> dumpText = ArgumentCaptor.forClass( String.class );
        verify( arguments ).dumpStreamText( dumpText.capture() );
        String msg = "Corrupted STDOUT by directly writing to native stream in forked JVM 0. Stream 'unordered error'.";
        assertThat( dumpText.getValue() )
            .isEqualTo( msg );

        ArgumentCaptor<String> warningText = ArgumentCaptor.forClass( String.class );
        verify( arguments ).logWarningAtEnd( warningText.capture() );
        assertThat( warningText.getValue() )
            .startsWith( "Corrupted STDOUT by directly writing to native stream in forked JVM 0. "
                + "See FAQ web page and the dump file" );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogJvmMessageAndProcessEvent() throws Exception
    {
        String nativeStream = "Listening for transport dt_socket at address: bla\n:maven-surefire-event:\u0003:bye:\n";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        when( logger.isDebugEnabled() ).thenReturn( true );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            Event event = eventHandler.pullEvent();
            assertThat( event )
                .isNotNull();
            assertThat( event.isControlCategory() )
                .isTrue();
            assertThat( event.getEventType() )
                .isEqualTo( BOOTERCODE_BYE );

            verify( logger )
                .debug( "Listening for transport dt_socket at address: bla" );

            countdown.awaitClosed();
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verify( logger ).isDebugEnabled();

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldBePossibleToKill()
    {
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        ForkClient client = new ForkClient( null, notifiableTestStream, 0 );
        client.kill();

        verify( notifiableTestStream, times( 1 ) )
                .shutdown( eq( Shutdown.KILL ) );
    }

    @Test
    public void shouldAcquireNextTest()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new ControlNextTestEvent() );
        verify( notifiableTestStream, times( 1 ) )
                .provideNewTest();
        verifyNoMoreInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldNotifyWithBye()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new ControlByeEvent() );
        client.kill();

        verify( notifiableTestStream, times( 1 ) )
                .acknowledgeByeEventReceived();
        verify( notifiableTestStream, never() )
                .shutdown( any( Shutdown.class ) );
        verifyNoMoreInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
        assertThat( client.isSaidGoodBye() )
                .isTrue();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldStopOnNextTest()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        final boolean[] verified = {false};
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 )
        {
            @Override
            protected void stopOnNextTest()
            {
                super.stopOnNextTest();
                verified[0] = true;
            }
        };
        client.handleEvent( new ControlStopOnNextTestEvent() );
        verifyZeroInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
        assertThat( verified[0] )
                .isTrue();
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldReceiveStdOut()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new StandardStreamOutEvent( NORMAL_RUN, "msg" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDOUT );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldReceiveStdOutNewLine()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new StandardStreamOutWithNewLineEvent( NORMAL_RUN, "msg" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDOUT );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg\n" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldReceiveStdErr()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new StandardStreamErrEvent( NORMAL_RUN, "msg" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDERR );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldReceiveStdErrNewLine()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new StandardStreamErrWithNewLineEvent( NORMAL_RUN, "msg" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( STDERR );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "msg\n" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldLogConsoleError()
    {
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        StackTraceWriter stackTrace =
            new DeserializedStacktraceWriter( "Listening for transport dt_socket at address: 5005", null, null );
        Event event = new ConsoleErrorEvent( stackTrace );
        client.handleEvent( event );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verify( factory, times( 1 ) )
            .getReportsDirectory();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .isNotEmpty();
        assertThat( receiver.getEvents() )
                .contains( CONSOLE_ERR );
        assertThat( receiver.getData() )
                .isNotEmpty();
        assertThat( receiver.getData() )
                .contains( "Listening for transport dt_socket at address: 5005" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldLogConsoleErrorWithStackTrace() throws Exception
    {
        String nativeStream = ":maven-surefire-event:\u0011:console-error-log:\u0005:UTF-8"
            + ":\u0000\u0000\u0000\u0032:Listening for transport dt_socket at address: 5005"
            + ":\u0000\u0000\u0000\u0002:s1:\u0000\u0000\u0000\u0002:s2:";
        EH eventHandler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( nativeStream.getBytes() ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
        {
            t.start();

            Event event = eventHandler.pullEvent();
            assertThat( event.isConsoleErrorCategory() )
                .isTrue();
            assertThat( event.isConsoleCategory() )
                .isTrue();
            assertThat( event.getEventType() )
                .isEqualTo( BOOTERCODE_CONSOLE_ERROR );

            ConsoleErrorEvent consoleEvent = (ConsoleErrorEvent) event;
            assertThat( consoleEvent.getStackTraceWriter() )
                .isNotNull();
            assertThat( consoleEvent.getStackTraceWriter().getThrowable().getMessage() )
                .isEqualTo( "Listening for transport dt_socket at address: 5005" );
            assertThat( consoleEvent.getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( "s1" );
            assertThat( consoleEvent.getStackTraceWriter().writeTraceToString() )
                .isEqualTo( "s2" );

            countdown.awaitClosed();

            verifyZeroInteractions( logger );
        }

        assertThat( eventHandler.sizeOfEventCache() )
            .isEqualTo( 0 );

        verifyNoMoreInteractions( logger );
    }

    @Test
    public void shouldLogConsoleWarning()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new ConsoleWarningEvent( "s1" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( CONSOLE_WARN );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "s1" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldLogConsoleDebug()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new ConsoleDebugEvent( "s1" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( CONSOLE_DEBUG );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "s1" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldLogConsoleInfo()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new ConsoleInfoEvent( "s1" ) );
        verifyZeroInteractions( notifiableTestStream );
        verify( factory, times( 1 ) )
                .createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 )
                .contains( CONSOLE_INFO );
        assertThat( receiver.getData() )
                .hasSize( 1 )
                .contains( "s1" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldSendSystemProperty()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new SystemPropertyEvent( NORMAL_RUN, "k1", "v1" ) );
        verifyZeroInteractions( notifiableTestStream );
        verifyZeroInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .isEmpty();
        assertThat( receiver.getData() )
                .isEmpty();
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .hasSize( 1 );
        assertThat( client.getTestVmSystemProperties() )
                .includes( entry( "k1", "v1" ) );
    }

    @Test
    public void shouldSendTestsetStartingKilled()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        TestSetReportEntry reportEntry = mock( TestSetReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new TestsetStartingEvent( NORMAL_RUN, reportEntry ) );

        client.tryToTimeout( System.currentTimeMillis() + 1000L, 1 );

        verify( notifiableTestStream )
                .shutdown( Shutdown.KILL );
        verifyNoMoreInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 );
        assertThat( receiver.getEvents() )
                .contains( SET_STARTING );
        assertThat( receiver.getData() )
                .hasSize( 1 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( exceptionMessage );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isTrue();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
    }

    @Test
    public void shouldSendTestsetStarting()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        TestSetReportEntry reportEntry = mock( TestSetReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameText() ).thenReturn( "dn2" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getSourceText() ).thenReturn( "dn1" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new TestsetStartingEvent( NORMAL_RUN, reportEntry ) );
        client.tryToTimeout( System.currentTimeMillis(), 1 );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 );
        assertThat( receiver.getEvents() )
                .contains( SET_STARTING );
        assertThat( receiver.getData() )
                .hasSize( 1 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isEqualTo( "dn1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isEqualTo( "dn2" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestsetCompleted()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        TestSetReportEntry reportEntry = mock( TestSetReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new TestsetCompletedEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 );
        assertThat( receiver.getEvents() )
                .contains( SET_COMPLETED );
        assertThat( receiver.getData() )
                .hasSize( 1 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( "MyTest:86 >> Error" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestStarting()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        client.handleEvent( new TestStartingEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.hasTestsInProgress() )
                .isTrue();
        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 1 );
        assertThat( receiver.getEvents() )
                .contains( TEST_STARTING );
        assertThat( receiver.getData() )
                .hasSize( 1 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestSucceeded()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream,  0 );
        SimpleReportEntry testStarted = new SimpleReportEntry( reportEntry.getSourceName(), null, null, null );
        client.handleEvent( new TestStartingEvent( NORMAL_RUN, testStarted ) );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.handleEvent( new TestSucceededEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 2 );
        assertThat( receiver.getEvents() )
                .contains( TEST_STARTING, TEST_SUCCEEDED );
        assertThat( receiver.getData() )
                .hasSize( 2 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestFailed()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        SimpleReportEntry testClass = new SimpleReportEntry( reportEntry.getSourceName(), null, null, null );
        client.handleEvent( new TestStartingEvent( NORMAL_RUN, testClass ) );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.handleEvent( new TestFailedEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 2 );
        assertThat( receiver.getEvents() )
                .contains( TEST_STARTING, TEST_FAILED );
        assertThat( receiver.getData() )
                .hasSize( 2 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( "MyTest:86 >> Error" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestSkipped()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        SimpleReportEntry testStarted = new SimpleReportEntry( reportEntry.getSourceName(), null, null, null );
        client.handleEvent( new TestStartingEvent( NORMAL_RUN, testStarted ) );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.handleEvent( new TestSkippedEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 2 );
        assertThat( receiver.getEvents() )
                .contains( TEST_STARTING, TEST_SKIPPED );
        assertThat( receiver.getData() )
                .hasSize( 2 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestError()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getSourceText() ).thenReturn( "display name" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        SimpleReportEntry testStarted =
            new SimpleReportEntry( reportEntry.getSourceName(), reportEntry.getSourceText(), null, null );
        client.handleEvent( new TestStartingEvent( NORMAL_RUN, testStarted ) );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.handleEvent( new TestErrorEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 2 );
        assertThat( receiver.getEvents() )
                .contains( TEST_STARTING, TEST_ERROR );
        assertThat( receiver.getData() )
                .hasSize( 2 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isEqualTo( "display name" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceText() )
                .isEqualTo( "display name" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    @Test
    public void shouldSendTestAssumptionFailure()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        MockReporter receiver = new MockReporter();
        when( factory.createReporter() )
                .thenReturn( receiver );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameText() ).thenReturn( "display name" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ForkClient client = new ForkClient( factory, notifiableTestStream, 0 );
        SimpleReportEntry testStarted = new SimpleReportEntry( reportEntry.getSourceName(), null, null, null );
        client.handleEvent( new TestStartingEvent( NORMAL_RUN, testStarted ) );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.handleEvent( new TestAssumptionFailureEvent( NORMAL_RUN, reportEntry ) );

        verifyZeroInteractions( notifiableTestStream );
        verify( factory ).createReporter();
        verifyNoMoreInteractions( factory );
        assertThat( client.getReporter() )
                .isNotNull();
        assertThat( receiver.getEvents() )
                .hasSize( 2 );
        assertThat( receiver.getEvents() )
                .contains( TEST_STARTING, TEST_ASSUMPTION_FAIL );
        assertThat( receiver.getData() )
                .hasSize( 2 );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getName() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getNameText() )
                .isEqualTo( "display name" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getElapsed() )
                .isEqualTo( ELAPSED_TIME );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getMessage() )
                .isEqualTo( "some test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getGroup() )
                .isEqualTo( "this group" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter() )
                .isNotNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) )
                .getStackTraceWriter().getThrowable().getLocalizedMessage() )
                .isEqualTo( "msg" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().smartTrimmedStackTrace() )
                .isEqualTo( smartStackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTraceToString() )
                .isEqualTo( stackTrace );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( trimmedStackTrace );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isFalse();
        assertThat( client.getErrorInFork() )
                .isNull();
        assertThat( client.hadTimeout() )
                .isFalse();
        assertThat( client.hasTestsInProgress() )
                .isFalse();
        assertThat( client.testsInProgress() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getTestVmSystemProperties() )
                .isEmpty();
        assertThat( client.getDefaultReporterFactory() )
                .isSameAs( factory );
    }

    private static class EH implements EventHandler<Event>
    {
        private final BlockingQueue<Event> cache = new LinkedBlockingQueue<>( 1 );

        Event pullEvent() throws InterruptedException
        {
            return cache.poll( 1, TimeUnit.MINUTES );
        }

        int sizeOfEventCache()
        {
            return cache.size();
        }

        @Override
        public void handleEvent( @Nonnull Event event )
        {
            cache.add( event );
        }
    }
}
