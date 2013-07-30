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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.surefire.booter.ForkingRunListener;
import org.apache.maven.surefire.report.CategorizedReportEntry;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class ForkingRunListenerTest
    extends TestCase
{

    private final ByteArrayOutputStream content;

    private final PrintStream printStream;

    final Integer defaultChannel = 17;

    final Integer anotherChannel = 18;

    public ForkingRunListenerTest()
    {
        this.content = new ByteArrayOutputStream();
        printStream = new PrintStream( content );
    }

    private void reset()
    {
        printStream.flush();
        content.reset();
    }


    public void testHeaderCreation()
    {
        final byte[] header = ForkingRunListener.createHeader( (byte) 'F', 0xCAFE );
        String asString = new String( header );
        assertEquals( "F,cafe,", asString );
    }

    public void testHeaderCreationShort()
    {
        final byte[] header = ForkingRunListener.createHeader( (byte) 'F', 0xE );
        String asString = new String( header );
        assertEquals( "F,000e,", asString );
    }

    public void testSetStarting()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
        standardTestRun.run().testSetStarting( expected );
        standardTestRun.assertExpected( MockReporter.SET_STARTING, expected );
    }

    public void testSetCompleted()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ReportEntry expected = createDefaultReportEntry();
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

    public void testStringTokenizer()
    {
        String test = "5,11,com.abc.TestClass,testMethod,null,22,,,";
        StringTokenizer tok = new StringTokenizer( test, "," );
        assertEquals( "5", tok.nextToken() );
        assertEquals( "11", tok.nextToken() );
        assertEquals( "com.abc.TestClass", tok.nextToken() );
        assertEquals( "testMethod", tok.nextToken() );
        assertEquals( "null", tok.nextToken() );
        assertEquals( "22", tok.nextToken() );
        assertFalse( tok.hasMoreTokens() );
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
        standardTestRun.assertExpected( MockReporter.CONSOLE_OUTPUT, "HeyYou" );
    }

    public void testConsoleOutput()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        ConsoleOutputReceiver directConsoleReporter = (ConsoleOutputReceiver) standardTestRun.run();
        directConsoleReporter.writeTestOutput( "HeyYou".getBytes(), 0, 6, true );
        standardTestRun.assertExpected( MockReporter.STDOUT, "HeyYou" );
    }

    public void testSystemProperties()
        throws ReporterException, IOException
    {
        final StandardTestRun standardTestRun = new StandardTestRun();
        standardTestRun.run();

        reset();
        createForkingRunListener( defaultChannel );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        final Properties testVmSystemProperties = new Properties();
        ForkClient forkStreamClient = new ForkClient( providerReporterFactory, testVmSystemProperties );

        forkStreamClient.consumeMultiLineContent( content.toString( "utf-8" ) );

        assertTrue( testVmSystemProperties.size() > 1 );
    }

    public void testMultipleEntries()
        throws ReporterException, IOException
    {

        final StandardTestRun standardTestRun = new StandardTestRun();
        standardTestRun.run();

        reset();
        RunListener forkingReporter = createForkingRunListener( defaultChannel );

        ReportEntry reportEntry = createDefaultReportEntry();
        forkingReporter.testSetStarting( reportEntry );
        forkingReporter.testStarting( reportEntry );
        forkingReporter.testSucceeded( reportEntry );
        forkingReporter.testSetCompleted( reportEntry );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        ForkClient forkStreamClient = new ForkClient( providerReporterFactory, new Properties() );

        forkStreamClient.consumeMultiLineContent( content.toString( "utf-8" ) );

        final MockReporter reporter = (MockReporter) forkStreamClient.getReporter( defaultChannel );
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

        new ForkingRunListener( printStream, defaultChannel, false ).testStarting( expected );
        new ForkingRunListener( printStream, anotherChannel, false ).testSkipped( secondExpected );

        TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
        final ForkClient forkStreamClient = new ForkClient( providerReporterFactory, new Properties() );
        forkStreamClient.consumeMultiLineContent( content.toString( "utf-8" ) );

        MockReporter reporter = (MockReporter) forkStreamClient.getReporter( defaultChannel );
        Assert.assertEquals( MockReporter.TEST_STARTING, reporter.getFirstEvent() );
        Assert.assertEquals( expected, reporter.getFirstData() );
        Assert.assertEquals( 1, reporter.getEvents().size() );

        MockReporter reporter2 = (MockReporter) forkStreamClient.getReporter( anotherChannel );
        Assert.assertEquals( MockReporter.TEST_SKIPPED, reporter2.getFirstEvent() );
        Assert.assertEquals( secondExpected, reporter2.getFirstData() );
        Assert.assertEquals( 1, reporter2.getEvents().size() );
    }

    // Todo: Test weird characters

    private SimpleReportEntry createDefaultReportEntry()
    {
        return new SimpleReportEntry( "com.abc.TestClass", "testMethod", 22 );
    }

    private SimpleReportEntry createAnotherDefaultReportEntry()
    {
        return new SimpleReportEntry( "com.abc.AnotherTestClass", "testAnotherMethod", 42 );
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

    private RunListener createForkingRunListener( Integer testSetCHannel )
    {
        return new ForkingRunListener( printStream, testSetCHannel, false );
    }

    private class StandardTestRun
    {
        private MockReporter reporter;

        public RunListener run()
            throws ReporterException
        {
            reset();
            return createForkingRunListener( defaultChannel );
        }

        public void clientReceiveContent()
            throws ReporterException, IOException
        {
            TestSetMockReporterFactory providerReporterFactory = new TestSetMockReporterFactory();
            final ForkClient forkStreamClient = new ForkClient( providerReporterFactory, new Properties() );
            forkStreamClient.consumeMultiLineContent( content.toString( ) );
            reporter = (MockReporter) forkStreamClient.getReporter( defaultChannel );
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
