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
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.booter.Shutdown;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.maven.plugin.surefire.booterclient.MockReporter.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ForkClient}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class ForkClientTest
{
    @Test
    public void shouldNotFailOnEmptyInput1()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeLine( null );
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
    public void shouldNotFailOnEmptyInput2()
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeLine( "   " );
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
    public void shouldNotFailOnEmptyInput3()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( null );
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
    public void shouldNotFailOnEmptyInput4()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        when( logger.isDebugEnabled() )
                .thenReturn( true );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( "   " );
        verify( logger )
                .isDebugEnabled();
        verify( logger )
                .warning( startsWith( "Corrupted STDOUT by directly writing to native stream in forked JVM 0. "
                        + "See FAQ web page and the dump file " ) );
        verify( logger )
                .debug( "   " );
        verifyNoMoreInteractions( logger );
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
    public void shouldNotFailOnEmptyInput5()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        when( logger.isDebugEnabled() )
                .thenReturn( true );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( "Listening for transport dt_socket at address: bla" );
        verify( logger )
                .isDebugEnabled();
        verify( logger )
                .debug( "Listening for transport dt_socket at address: bla" );
        verifyNoMoreInteractions( logger );
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
    public void shouldNotFailOnEmptyInput6()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        when( logger.isDebugEnabled() )
                .thenReturn( false );
        when( logger.isInfoEnabled() )
                .thenReturn( true );
        ForkClient client = new ForkClient( factory, null, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( "Listening for transport dt_socket at address: bla" );
        verify( logger )
                .isDebugEnabled();
        verify( logger )
                .isInfoEnabled();
        verify( logger )
                .info( "Listening for transport dt_socket at address: bla" );
        verifyNoMoreInteractions( logger );
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
    public void shouldBePossibleToKill()
    {
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );

        ForkClient client = new ForkClient( null, notifiableTestStream, null, null, 0 );
        client.kill();

        verify( notifiableTestStream, times( 1 ) )
                .shutdown( eq( Shutdown.KILL ) );
    }

    @Test
    public void shouldAcquireNextTest()
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:next-test\n" );
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
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:bye\n" );
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
            throws IOException
    {
        String cwd = System.getProperty( "user.dir" );
        File target = new File( cwd, "target" );
        DefaultReporterFactory factory = mock( DefaultReporterFactory.class );
        when( factory.getReportsDirectory() )
                .thenReturn( new File( target, "surefire-reports" ) );
        NotifiableTestStream notifiableTestStream = mock( NotifiableTestStream.class );
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        final boolean[] verified = {false};
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 )
        {
            @Override
            protected void stopOnNextTest()
            {
                super.stopOnNextTest();
                verified[0] = true;
            }
        };
        client.consumeMultiLineContent( ":maven:surefire:std:out:stop-on-next-test\n" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-out-stream:normal-run:UTF-8:bXNn\n" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-out-stream-new-line:normal-run:UTF-8:bXNn\n" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-err-stream:normal-run:UTF-8:bXNn\n" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:std-err-stream-new-line:normal-run:UTF-8:bXNn\n" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:console-error-log:UTF-8:"
                + encodeBase64String( "Listening for transport dt_socket at address:".getBytes( UTF_8 ) )
                + ":-:-:-" );
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
                .contains( "Listening for transport dt_socket at address:" );
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
    public void shouldLogConsoleErrorWithStackTrace()
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:console-error-log:UTF-8"
                + ":" + encodeBase64String( "Listening for transport dt_socket at address:".getBytes( UTF_8 ) )
                + ":" + encodeBase64String( "s1".getBytes( UTF_8 ) )
                + ":" + encodeBase64String( "s2".getBytes( UTF_8 ) ) );
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
                .contains( "Listening for transport dt_socket at address:" );
        assertThat( client.isSaidGoodBye() )
                .isFalse();
        assertThat( client.isErrorInFork() )
                .isTrue();
        assertThat( client.getErrorInFork() )
                .isNotNull();
        assertThat( client.getErrorInFork().getThrowable().getLocalizedMessage() )
                .isEqualTo( "Listening for transport dt_socket at address:" );
        assertThat( client.getErrorInFork().smartTrimmedStackTrace() )
                .isEqualTo( "s1" );
        assertThat( client.getErrorInFork().writeTrimmedTraceToString() )
                .isEqualTo( "s2" );
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
    public void shouldLogConsoleWarning()
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        when( logger.isWarnEnabled() )
                .thenReturn( true );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:console-warning-log:UTF-8:"
                + encodeBase64String( "s1".getBytes( UTF_8 ) ) );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        when( logger.isDebugEnabled() )
                .thenReturn( true );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:console-debug-log:UTF-8:"
                + encodeBase64String( "s1".getBytes( UTF_8 ) ) );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:console-info-log:UTF-8:"
                + encodeBase64String( "s1".getBytes( UTF_8 ) ) );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:sys-prop:normal-run:UTF-8:azE=:djE="
                + encodeBase64String( "s1".getBytes( UTF_8 ) ) );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:testset-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedTrimmedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameText() ).thenReturn( "dn2" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getSourceText() ).thenReturn( "dn1" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedSourceText = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceText() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedNameText = encodeBase64String( toArray( UTF_8.encode( reportEntry.getNameText() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:testset-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + encodedSourceText
                + ":"
                + encodedName
                + ":"
                + encodedNameText
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedTrimmedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:testset-completed:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedTrimmedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );
        client.consumeMultiLineContent( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedTrimmedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 0 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":-:-:-:-:-:-:-:-:-" );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-succeeded:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1\ntrace line 2" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1\ntrace line 2" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":-:-:-:-:-:-:-:-:-" );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-failed:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedTrimmedStackTrace );

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
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceName() )
                .isEqualTo( "pkg.MyTest" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getSourceText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getName() )
                .isEqualTo( "my test" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getNameText() )
                .isNull();
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getElapsed() )
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":-:-:-:-:-:-:-:-:-" );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-skipped:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1\ntrace line 2" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1\ntrace line 2" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getSourceText() ).thenReturn( "display name" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedSourceText = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceText() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + encodedSourceText
                + ":-:':-:-:-:-:-:-" );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-error:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + encodedSourceText
                + ":"
                + encodedName
                + ":"
                + "-"
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedTrimmedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1" );
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
            throws IOException
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
        AtomicBoolean printedErrorStream = new AtomicBoolean();
        ConsoleLogger logger = mock( ConsoleLogger.class );


        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "some test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameText() ).thenReturn("display name");
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedText = encodeBase64String( toArray( UTF_8.encode( reportEntry.getNameText() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        ForkClient client = new ForkClient( factory, notifiableTestStream, logger, printedErrorStream, 0 );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                + encodedSourceName
                + ":-:-:-:-:-:-:-:-:-" );

        assertThat( client.testsInProgress() )
                .hasSize( 1 )
                .contains( "pkg.MyTest" );

        client.consumeMultiLineContent( ":maven:surefire:std:out:test-assumption-failure:normal-run:UTF-8:"
                + encodedSourceName
                + ":"
                + "-"
                + ":"
                + encodedName
                + ":"
                + encodedText
                + ":"
                + encodedGroup
                + ":"
                + encodedMessage
                + ":"
                + 102
                + ":"

                + encodedExceptionMsg
                + ":"
                + encodedSmartStackTrace
                + ":"
                + encodedStackTrace );

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
                .isEqualTo( 102 );
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
                .isEqualTo( "trace line 1\ntrace line 2" );
        assertThat( ( (ReportEntry) receiver.getData().get( 1 ) ).getStackTraceWriter().writeTrimmedTraceToString() )
                .isEqualTo( "trace line 1\ntrace line 2" );
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

    private static byte[] toArray( ByteBuffer buffer )
    {
        return copyOfRange( buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.remaining() );
    }

}
