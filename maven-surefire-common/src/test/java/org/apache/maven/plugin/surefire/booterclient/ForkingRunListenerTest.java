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

import junit.framework.TestCase;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.plugin.surefire.extensions.EventConsumerThread;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.ForkingRunListener;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoder;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.api.report.CategorizedReportEntry;
import org.apache.maven.surefire.api.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.api.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.PrintStream;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class ForkingRunListenerTest
    extends TestCase
{
    private final ByteArrayOutputStream content, anotherContent;

    private final PrintStream printStream, anotherPrintStream;

    public ForkingRunListenerTest()
    {
        content = new ByteArrayOutputStream();
        printStream = new PrintStream( content );

        anotherContent = new ByteArrayOutputStream();
        anotherPrintStream = new PrintStream( anotherContent );
    }

    private void reset()
    {
        printStream.flush();
        content.reset();
    }

    public void testSetStarting() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        TestSetReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSetStarting( expected );
        standardTestRun.assertExpected( MockReporter.SET_STARTING, expected );
    }

    public void testSetCompleted() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        TestSetReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSetCompleted( expected );
        standardTestRun.assertExpected( MockReporter.SET_COMPLETED, expected );
    }

    public void testStarting() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testStarting( expected );
        standardTestRun.assertExpected( MockReporter.TEST_STARTING, expected );
    }

    public void testSucceeded() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSucceeded( expected );
        standardTestRun.assertExpected( MockReporter.TEST_SUCCEEDED, expected );
    }

    public void testFailed() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createReportEntryWithStackTrace();
        standardTestRun.run().testFailed( expected );
        standardTestRun.assertExpected( MockReporter.TEST_FAILED, expected );
    }

    public void testFailedWithCommaInMessage() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createReportEntryWithSpecialMessage( "We, the people" );
        standardTestRun.run().testFailed( expected );
        standardTestRun.assertExpected( MockReporter.TEST_FAILED, expected );
    }

    public void testFailedWithUnicodeEscapeInMessage() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createReportEntryWithSpecialMessage( "We, \\u0177 people" );
        standardTestRun.run().testFailed( expected );
        standardTestRun.assertExpected( MockReporter.TEST_FAILED, expected );
    }

    public void testFailure() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testError( expected );
        standardTestRun.assertExpected( MockReporter.TEST_ERROR, expected );
    }

    public void testSkipped() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSkipped( expected );
        standardTestRun.assertExpected( MockReporter.TEST_SKIPPED, expected );
    }

    public void testAssumptionFailure() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testAssumptionFailure( expected );
        standardTestRun.assertExpected( MockReporter.TEST_ASSUMPTION_FAIL, expected );
    }

    public void testConsole() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ConsoleLogger directConsoleReporter = (ConsoleLogger) standardTestRun.run();
        directConsoleReporter.info( "HeyYou" );
        standardTestRun.assertExpected( MockReporter.CONSOLE_INFO, "HeyYou" );
    }

    public void testConsoleOutput() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ConsoleOutputReceiver directConsoleReporter = (ConsoleOutputReceiver) standardTestRun.run();
        directConsoleReporter.writeTestOutput( "HeyYou", false, true );
        standardTestRun.assertExpected( MockReporter.STDOUT, "HeyYou" );
    }

    public void testSystemProperties() throws Exception
    {
        StandardTestRun standardTestRun = new StandardTestRun();
        standardTestRun.run();

        reset();
        createForkingRunListener();

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        ForkClient forkStreamClient = new ForkClient( providerReporterFactory, new MockNotifiableTestStream(), 1 );

        byte[] cmd = ( ":maven-surefire-event:\u0008:sys-prop:" + (char) 10 + ":normal-run:\u0005:UTF-8:"
            + "\u0000\u0000\u0000\u0002:k1:\u0000\u0000\u0000\u0002:v1:\n" ).getBytes();
        for ( Event e : streamToEvent( cmd ) )
        {
            forkStreamClient.handleEvent( e );
        }
        cmd = ( "\n:maven-surefire-event:\u0008:sys-prop:" + (char) 10 + ":normal-run:\u0005:UTF-8:"
            + "\u0000\u0000\u0000\u0002:k2:\u0000\u0000\u0000\u0002:v2:\n" ).getBytes();
        for ( Event e : streamToEvent( cmd ) )
        {
            forkStreamClient.handleEvent( e );
        }

        assertThat( forkStreamClient.getTestVmSystemProperties() )
            .hasSize( 2 );

        assertThat( forkStreamClient.getTestVmSystemProperties() )
            .includes( entry( "k1", "v1" ) );

        assertThat( forkStreamClient.getTestVmSystemProperties() )
            .includes( entry( "k2", "v2" ) );
    }

    public void testMultipleEntries() throws Exception
    {
        StandardTestRun standardTestRun = new StandardTestRun();
        standardTestRun.run();

        reset();
        RunListener forkingReporter = createForkingRunListener();

        TestSetReportEntry reportEntry = createDefaultReportEntry();
        forkingReporter.testSetStarting( reportEntry );
        forkingReporter.testStarting( reportEntry );
        forkingReporter.testSucceeded( reportEntry );
        forkingReporter.testSetCompleted( reportEntry );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        ForkClient forkStreamClient =
                new ForkClient( providerReporterFactory, new MockNotifiableTestStream(), 1 );

        for ( Event e : streamToEvent( content.toByteArray() ) )
        {
            forkStreamClient.handleEvent( e );
        }

        final MockReporter reporter = (MockReporter) forkStreamClient.getReporter();
        final List<String> events = reporter.getEvents();
        assertEquals( MockReporter.SET_STARTING, events.get( 0 ) );
        assertEquals( MockReporter.TEST_STARTING, events.get( 1 ) );
        assertEquals( MockReporter.TEST_SUCCEEDED, events.get( 2 ) );
        assertEquals( MockReporter.SET_COMPLETED, events.get( 3 ) );
    }

    public void test2DifferentChannels()
        throws Exception
    {
        reset();
        ReportEntry expected = createDefaultReportEntry();
        SimpleReportEntry secondExpected = createAnotherDefaultReportEntry();

        new ForkingRunListener( new LegacyMasterProcessChannelEncoder( newBufferedChannel( printStream ) ), false )
                .testStarting( expected );

        new ForkingRunListener(
            new LegacyMasterProcessChannelEncoder( newBufferedChannel( anotherPrintStream ) ), false )
                .testSkipped( secondExpected );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        NotifiableTestStream notifiableTestStream = new MockNotifiableTestStream();

        ForkClient forkStreamClient = new ForkClient( providerReporterFactory, notifiableTestStream, 1 );
        for ( Event e : streamToEvent( content.toByteArray() ) )
        {
            forkStreamClient.handleEvent( e );
        }

        MockReporter reporter = (MockReporter) forkStreamClient.getReporter();
        assertEquals( MockReporter.TEST_STARTING, reporter.getFirstEvent() );
        assertEquals( expected, reporter.getFirstData() );
        assertEquals( 1, reporter.getEvents().size() );

        forkStreamClient = new ForkClient( providerReporterFactory, notifiableTestStream, 2 );
        for ( Event e : streamToEvent( anotherContent.toByteArray() ) )
        {
            forkStreamClient.handleEvent( e );
        }
        MockReporter reporter2 = (MockReporter) forkStreamClient.getReporter();
        assertEquals( MockReporter.TEST_SKIPPED, reporter2.getFirstEvent() );
        assertEquals( secondExpected, reporter2.getFirstData() );
        assertEquals( 1, reporter2.getEvents().size() );
    }

    private static List<Event> streamToEvent( byte[] stream ) throws Exception
    {
        List<Event> events = new ArrayList<>();
        EH handler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArguments arguments = mock( ForkNodeArguments.class );
        when( arguments.dumpStreamText( anyString() ) ).thenReturn( new File( "" ) );
        when( arguments.getConsoleLogger() ).thenReturn( logger );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( stream ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, handler, countdown, arguments ) )
        {
            t.start();
            countdown.awaitClosed();
            verifyZeroInteractions( logger );
            verify( arguments, never() ).dumpStreamText( anyString() );
            for ( int i = 0, size = handler.countEventsInCache(); i < size; i++ )
            {
                events.add( handler.pullEvent() );
            }
            assertEquals( 0, handler.countEventsInCache() );
            return events;
        }
    }

    private static class EH implements EventHandler<Event>
    {
        private final BlockingQueue<Event> cache = new LinkedBlockingQueue<>();

        Event pullEvent() throws InterruptedException
        {
            return cache.poll( 1, TimeUnit.MINUTES );
        }

        int countEventsInCache()
        {
            return cache.size();
        }

        @Override
        public void handleEvent( @Nonnull Event event )
        {
            cache.add( event );
        }
    }

    // Todo: Test weird characters

    private SimpleReportEntry createDefaultReportEntry( Map<String, String> sysProps )
    {
        return new SimpleReportEntry( "com.abc.TestClass", null, "testMethod", null, null, 22, sysProps );
    }

    private SimpleReportEntry createDefaultReportEntry()
    {
        return createDefaultReportEntry( Collections.<String, String>emptyMap() );
    }

    private SimpleReportEntry createAnotherDefaultReportEntry()
    {
        return new SimpleReportEntry( "com.abc.AnotherTestClass", null, "testAnotherMethod", null, 42 );
    }

    private SimpleReportEntry createReportEntryWithStackTrace()
    {
        try
        {
            throw new RuntimeException();
        }
        catch ( RuntimeException e )
        {
            StackTraceWriter stackTraceWriter =
                new LegacyPojoStackTraceWriter( "org.apache.tests.TestClass", "testMethod11", e );
            return new CategorizedReportEntry( "com.abc.TestClass", "testMethod", "aGroup", stackTraceWriter, 77 );
        }
    }

    private SimpleReportEntry createReportEntryWithSpecialMessage( String message )
    {
        try
        {
            throw new RuntimeException( message );
        }
        catch ( RuntimeException e )
        {
            StackTraceWriter stackTraceWriter =
                new LegacyPojoStackTraceWriter( "org.apache.tests.TestClass", "testMethod11", e );
            return new CategorizedReportEntry( "com.abc.TestClass", "testMethod", "aGroup", stackTraceWriter, 77 );
        }
    }

    private RunListener createForkingRunListener()
    {
        WritableBufferedByteChannel channel = (WritableBufferedByteChannel) newChannel( printStream );
        return new ForkingRunListener( new LegacyMasterProcessChannelEncoder( channel ), false );
    }

    private class StandardTestRun
    {
        private MockReporter reporter;

        public RunListener run()
            throws ReporterException
        {
            reset();
            return createForkingRunListener();
        }

        public void clientReceiveContent() throws Exception
        {
            TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
            ForkClient handler = new ForkClient( providerReporterFactory, new MockNotifiableTestStream(), 1 );
            for ( Event e : streamToEvent( content.toByteArray() ) )
            {
                handler.handleEvent( e );
            }
            reporter = (MockReporter) handler.getReporter();
        }

        public String getFirstEvent()
        {
            return reporter.getEvents().get( 0 );
        }

        public ReportEntry getFirstData()
        {
            return (ReportEntry) reporter.getData().get( 0 );
        }

        private void assertExpected( String actionCode, ReportEntry expected ) throws Exception
        {
            clientReceiveContent();
            assertEquals( actionCode, getFirstEvent() );
            final ReportEntry firstData = getFirstData();
            assertEquals( expected.getSourceName(), firstData.getSourceName() );
            assertEquals( expected.getName(), firstData.getName() );
            //noinspection deprecation
            assertEquals( expected.getElapsed(), firstData.getElapsed() );
            assertEquals( expected.getGroup(), firstData.getGroup() );
            if ( expected.getStackTraceWriter() != null )
            {
                //noinspection ThrowableResultOfMethodCallIgnored
                assertEquals( expected.getStackTraceWriter().getThrowable().getLocalizedMessage(),
                              firstData.getStackTraceWriter().getThrowable().getLocalizedMessage() );
                assertEquals( expected.getStackTraceWriter().writeTraceToString(),
                              firstData.getStackTraceWriter().writeTraceToString() );
            }
        }

        private void assertExpected( String actionCode, String expected ) throws Exception
        {
            clientReceiveContent();
            assertEquals( actionCode, getFirstEvent() );
            final String firstData = (String) reporter.getData().get( 0 );
            assertEquals( expected, firstData );
        }

    }
}
