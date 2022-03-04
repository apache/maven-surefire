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
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.booter.spi.EventChannelEncoder;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.api.report.CategorizedReportEntry;
import org.apache.maven.surefire.api.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdOut;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

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
        ConsoleLogger directConsoleReporter = standardTestRun.run();
        directConsoleReporter.info( "HeyYou" );
        standardTestRun.assertExpected( MockReporter.CONSOLE_INFO, "HeyYou" );
    }

    public void testConsoleOutput() throws Exception
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        TestOutputReceiver<TestOutputReportEntry> directConsoleReporter = standardTestRun.run();
        directConsoleReporter.writeTestOutput( new TestOutputReportEntry( stdOut( "HeyYou" ), NORMAL_RUN, 1L )  );
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

        byte[] cmd = ( ":maven-surefire-event:\u0008:sys-prop:" + (char) 10 + ":normal-run:"
            + "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
            + "\u0005:UTF-8:"
            + "\u0000\u0000\u0000\u0002:k1:\u0000\u0000\u0000\u0002:v1:" ).getBytes();
        for ( Event e : streamToEvent( cmd ) )
        {
            forkStreamClient.handleEvent( e );
        }
        cmd = ( "\n:maven-surefire-event:\u0008:sys-prop:" + (char) 10 + ":normal-run:"
            + "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
            + "\u0005:UTF-8:"
            + "\u0000\u0000\u0000\u0002:k2:\u0000\u0000\u0000\u0002:v2:" ).getBytes();
        for ( Event e : streamToEvent( cmd ) )
        {
            forkStreamClient.handleEvent( e );
        }

        assertThat( forkStreamClient.getTestVmSystemProperties() )
            .hasSize( 2 );

        assertThat( forkStreamClient.getTestVmSystemProperties() )
            .containsEntry( "k1", "v1" );

        assertThat( forkStreamClient.getTestVmSystemProperties() )
            .containsEntry( "k2", "v2" );
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

        new ForkingRunListener( new EventChannelEncoder( newBufferedChannel( printStream ) ), false )
                .testStarting( expected );

        new ForkingRunListener(
            new EventChannelEncoder( newBufferedChannel( anotherPrintStream ) ), false )
                .testSkipped( secondExpected );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        NotifiableTestStream notifiableTestStream = new MockNotifiableTestStream();

        ForkClient forkStreamClient = new ForkClient( providerReporterFactory, notifiableTestStream, 1 );
        for ( Event e : streamToEvent( content.toByteArray() ) )
        {
            forkStreamClient.handleEvent( e );
        }

        MockReporter reporter = (MockReporter) forkStreamClient.getReporter();
        assertThat( reporter.getFirstEvent() ).isEqualTo( MockReporter.TEST_STARTING );
        assertThat( reporter.getFirstData() ).isEqualTo( expected );
        assertThat( reporter.getEvents() ).hasSize( 1 );

        forkStreamClient = new ForkClient( providerReporterFactory, notifiableTestStream, 2 );
        for ( Event e : streamToEvent( anotherContent.toByteArray() ) )
        {
            forkStreamClient.handleEvent( e );
        }
        MockReporter reporter2 = (MockReporter) forkStreamClient.getReporter();
        assertThat( reporter2.getFirstEvent() ).isEqualTo( MockReporter.TEST_SKIPPED );
        assertThat( reporter2.getFirstData() ).isEqualTo( secondExpected );
        assertThat( reporter2.getEvents() ).hasSize( 1 );
    }

    private static List<Event> streamToEvent( byte[] stream ) throws Exception
    {
        List<Event> events = new ArrayList<>();
        EH handler = new EH();
        CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
        ConsoleLogger logger = mock( ConsoleLogger.class );
        ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
        ReadableByteChannel channel = newChannel( new ByteArrayInputStream( stream ) );
        try ( EventConsumerThread t = new EventConsumerThread( "t", channel, handler, countdown, arguments ) )
        {
            t.start();
            countdown.awaitClosed();
            verifyZeroInteractions( logger );
            assertThat( arguments.isCalled() )
                .isFalse();
            for ( int i = 0, size = handler.countEventsInCache(); i < size; i++ )
            {
                events.add( handler.pullEvent() );
            }
            assertEquals( 0, handler.countEventsInCache() );
            return events;
        }
    }

    /**
     * Threadsafe impl. Mockito and Powermock are not thread-safe.
     */
    private static class ForkNodeArgumentsMock implements ForkNodeArguments
    {
        private final ConcurrentLinkedQueue<String> dumpStreamText = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> logWarningAtEnd = new ConcurrentLinkedQueue<>();
        private final ConsoleLogger logger;
        private final File dumpStreamTextFile;

        ForkNodeArgumentsMock( ConsoleLogger logger, File dumpStreamTextFile )
        {
            this.logger = logger;
            this.dumpStreamTextFile = dumpStreamTextFile;
        }

        @Nonnull
        @Override
        public String getSessionId()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getForkChannelId()
        {
            return 0;
        }

        @Nonnull
        @Override
        public File dumpStreamText( @Nonnull String text )
        {
            dumpStreamText.add( text );
            return dumpStreamTextFile;
        }

        @Nonnull
        @Override
        public File dumpStreamException( @Nonnull Throwable t )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logWarningAtEnd( @Nonnull String text )
        {
            logWarningAtEnd.add( text );
        }

        @Nonnull
        @Override
        public ConsoleLogger getConsoleLogger()
        {
            return logger;
        }

        @Nonnull
        @Override
        public Object getConsoleLock()
        {
            return logger;
        }

        boolean isCalled()
        {
            return !dumpStreamText.isEmpty() || !logWarningAtEnd.isEmpty();
        }

        @Override
        public File getEventStreamBinaryFile()
        {
            return null;
        }

        @Override
        public File getCommandStreamBinaryFile()
        {
            return null;
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
        return new SimpleReportEntry( NORMAL_RUN, 1L,
            "com.abc.TestClass", null, "testMethod", null, null, 22, sysProps );
    }

    private SimpleReportEntry createDefaultReportEntry()
    {
        return createDefaultReportEntry( Collections.<String, String>emptyMap() );
    }

    private SimpleReportEntry createAnotherDefaultReportEntry()
    {
        return new SimpleReportEntry( NORMAL_RUN, 0L,
            "com.abc.AnotherTestClass", null, "testAnotherMethod", null, 42 );
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
            return new CategorizedReportEntry( NORMAL_RUN, 0L,
                "com.abc.TestClass", "testMethod", "aGroup", stackTraceWriter, 77 );
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
            return new CategorizedReportEntry( NORMAL_RUN, 1L,
                "com.abc.TestClass", "testMethod", "aGroup", stackTraceWriter, 77 );
        }
    }

    private TestReportListener<TestOutputReportEntry> createForkingRunListener()
    {
        WritableBufferedByteChannel channel = (WritableBufferedByteChannel) newChannel( printStream );
        return new ForkingRunListener( new EventChannelEncoder( channel ), false );
    }

    private class StandardTestRun
    {
        private MockReporter reporter;

        public TestReportListener<TestOutputReportEntry> run()
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
