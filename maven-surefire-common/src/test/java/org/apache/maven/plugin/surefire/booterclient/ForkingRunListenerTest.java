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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.booter.ForkedChannelEncoder;
import org.apache.maven.surefire.booter.ForkingRunListener;
import org.apache.maven.surefire.report.CategorizedReportEntry;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.report.TestSetReportEntry;
import org.hamcrest.MatcherAssert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.is;

/**
 * @author Kristian Rosenvold
 */
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

    public void testSetStarting()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        TestSetReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSetStarting( expected );
        standardTestRun.assertExpected( MockReporter.SET_STARTING, expected );
    }

    public void testSetCompleted()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        TestSetReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSetCompleted( expected );
        standardTestRun.assertExpected( MockReporter.SET_COMPLETED, expected );
    }

    public void testStarting()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testStarting( expected );
        standardTestRun.assertExpected( MockReporter.TEST_STARTING, expected );
    }

    public void testSucceded()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSucceeded( expected );
        standardTestRun.assertExpected( MockReporter.TEST_SUCCEEDED, expected );
    }

    public void testFailed()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createReportEntryWithStackTrace();
        standardTestRun.run().testFailed( expected );
        standardTestRun.assertExpected( MockReporter.TEST_FAILED, expected );
    }

    public void testFailedWithCommaInMessage()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createReportEntryWithSpecialMessage( "We, the people" );
        standardTestRun.run().testFailed( expected );
        standardTestRun.assertExpected( MockReporter.TEST_FAILED, expected );
    }

    public void testFailedWithUnicodeEscapeInMessage()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createReportEntryWithSpecialMessage( "We, \\u0177 people" );
        standardTestRun.run().testFailed( expected );
        standardTestRun.assertExpected( MockReporter.TEST_FAILED, expected );
    }

    public void testFailure()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testError( expected );
        standardTestRun.assertExpected( MockReporter.TEST_ERROR, expected );
    }

    public void testSkipped()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSkipped( expected );
        standardTestRun.assertExpected( MockReporter.TEST_SKIPPED, expected );
    }

    public void testAssumptionFailure()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testAssumptionFailure( expected );
        standardTestRun.assertExpected( MockReporter.TEST_ASSUMPTION_FAIL, expected );
    }

    public void testConsole()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ConsoleLogger directConsoleReporter = (ConsoleLogger) standardTestRun.run();
        directConsoleReporter.info( "HeyYou" );
        standardTestRun.assertExpected( MockReporter.CONSOLE_INFO, "HeyYou" );
    }

    public void testConsoleOutput()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ConsoleOutputReceiver directConsoleReporter = (ConsoleOutputReceiver) standardTestRun.run();
        directConsoleReporter.writeTestOutput( "HeyYou", false, true );
        standardTestRun.assertExpected( MockReporter.STDOUT, "HeyYou" );
    }

    public void testSystemProperties()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        standardTestRun.run();

        reset();
        createForkingRunListener();

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        NullConsoleLogger log = new NullConsoleLogger();
        ForkClient forkStreamClient =
                new ForkClient( providerReporterFactory, new MockNotifiableTestStream(), log, new AtomicBoolean(), 1 );

        forkStreamClient.consumeMultiLineContent( ":maven:surefire:std:out:sys-prop:normal-run:UTF-8:azE=:djE="
                + "\n:maven:surefire:std:out:sys-prop:normal-run:UTF-8:azI=:djI=" );

        MatcherAssert.assertThat( forkStreamClient.getTestVmSystemProperties().size(), is( 2 ) );
    }

    public void testMultipleEntries()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        standardTestRun.run();

        reset();
        RunListener forkingReporter = createForkingRunListener();

        TestSetReportEntry reportEntry = createDefaultReportEntry();
        forkingReporter.testSetStarting( reportEntry );
        forkingReporter.testStarting( reportEntry );
        forkingReporter.testSucceeded( reportEntry );
        forkingReporter.testSetCompleted( reportEntry );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        NullConsoleLogger log = new NullConsoleLogger();
        ForkClient forkStreamClient =
                new ForkClient( providerReporterFactory, new MockNotifiableTestStream(), log, null, 1 );

        forkStreamClient.consumeMultiLineContent( content.toString( "UTF-8" ) );

        final MockReporter reporter = (MockReporter) forkStreamClient.getReporter();
        final List<String> events = reporter.getEvents();
        assertEquals( MockReporter.SET_STARTING, events.get( 0 ) );
        assertEquals( MockReporter.TEST_STARTING, events.get( 1 ) );
        assertEquals( MockReporter.TEST_SUCCEEDED, events.get( 2 ) );
        assertEquals( MockReporter.SET_COMPLETED, events.get( 3 ) );
    }

    public void test2DifferentChannels()
        throws ReporterException, IOException
    {
        reset();
        ReportEntry expected = createDefaultReportEntry();
        final SimpleReportEntry secondExpected = createAnotherDefaultReportEntry();

        new ForkingRunListener( new ForkedChannelEncoder( printStream ), false )
                .testStarting( expected );

        new ForkingRunListener( new ForkedChannelEncoder( anotherPrintStream ), false )
                .testSkipped( secondExpected );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        NotifiableTestStream notifiableTestStream = new MockNotifiableTestStream();
        NullConsoleLogger log = new NullConsoleLogger();

        ForkClient forkStreamClient = new ForkClient( providerReporterFactory, notifiableTestStream, log, null, 1 );
        forkStreamClient.consumeMultiLineContent( content.toString( "UTF-8" ) );

        MockReporter reporter = (MockReporter) forkStreamClient.getReporter();
        Assert.assertEquals( MockReporter.TEST_STARTING, reporter.getFirstEvent() );
        Assert.assertEquals( expected, reporter.getFirstData() );
        Assert.assertEquals( 1, reporter.getEvents().size() );

        forkStreamClient = new ForkClient( providerReporterFactory, notifiableTestStream, log, null, 2 );
        forkStreamClient.consumeMultiLineContent( anotherContent.toString( "UTF-8" ) );
        MockReporter reporter2 = (MockReporter) forkStreamClient.getReporter();
        Assert.assertEquals( MockReporter.TEST_SKIPPED, reporter2.getFirstEvent() );
        Assert.assertEquals( secondExpected, reporter2.getFirstData() );
        Assert.assertEquals( 1, reporter2.getEvents().size() );
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
        return new ForkingRunListener( new ForkedChannelEncoder( printStream ), false );
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

        public void clientReceiveContent()
            throws ReporterException, IOException
        {
            TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
            NullConsoleLogger log = new NullConsoleLogger();
            final ForkClient forkStreamClient =
                    new ForkClient( providerReporterFactory, new MockNotifiableTestStream(), log, null, 1 );
            forkStreamClient.consumeMultiLineContent( content.toString( ) );
            reporter = (MockReporter) forkStreamClient.getReporter();
        }

        public String getFirstEvent()
        {
            return reporter.getEvents().get( 0 );
        }

        public ReportEntry getFirstData()
        {
            return (ReportEntry) reporter.getData().get( 0 );
        }

        private void assertExpected( String actionCode, ReportEntry expected )
            throws IOException, ReporterException
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

        private void assertExpected( String actionCode, String expected )
            throws IOException, ReporterException
        {
            clientReceiveContent();
            assertEquals( actionCode, getFirstEvent() );
            final String firstData = (String) reporter.getData().get( 0 );
            assertEquals( expected, firstData );
        }

    }
}
