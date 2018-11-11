package org.apache.maven.plugins.surefire.report;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Kristian Rosenvold
 */
public class TestSuiteXmlParserTest
{
    private static final String[] linePatterns = { "at org.apache.Test.", "at org.apache.Test$" };

    private final Collection<String> loggedErrors = new ArrayList<>();

    private ConsoleLogger consoleLogger;

    @Before
    public void instantiateLogger()
    {
        consoleLogger = new ConsoleLogger()
        {
            @Override
            public boolean isDebugEnabled()
            {
                return true;
            }

            @Override
            public void debug( String message )
            {
            }

            @Override
            public boolean isInfoEnabled()
            {
                return true;
            }

            @Override
            public void info( String message )
            {
            }

            @Override
            public boolean isWarnEnabled()
            {
                return true;
            }

            @Override
            public void warning( String message )
            {
                loggedErrors.add( message );
            }

            @Override
            public boolean isErrorEnabled()
            {
                return true;
            }

            @Override
            public void error( String message )
            {
                loggedErrors.add( message );
            }

            @Override
            public void error( String message, Throwable t )
            {
                loggedErrors.add( message );
            }

            @Override
            public void error( Throwable t )
            {
                loggedErrors.add( t.getLocalizedMessage() );
            }
        };
    }

    @After
    public void verifyErrorFreeLogger()
    {
        assertThat( loggedErrors, is( empty() ) );
    }

    @Test
    public void testParse()
        throws Exception
    {
        TestSuiteXmlParser testSuiteXmlParser = new TestSuiteXmlParser( consoleLogger );
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<testsuite failures=\"4\" time=\"0.005\" errors=\"0\" skipped=\"0\" tests=\"4\" name=\"wellFormedXmlFailures.TestSurefire3\">\n"
            +
            "  <properties>\n" +
            "    <property name=\"java.runtime.name\" value=\"Java(TM) SE Runtime Environment\"/>\n" +
            "    <property name=\"sun.cpu.isalist\" value=\"amd64\"/>\n" +
            "  </properties>\n" +
            "  <testcase time=\"0.005\" classname=\"wellFormedXmlFailures.TestSurefire3\" name=\"testLower\">\n" +
            "    <failure message=\"&lt;\" type=\"junit.framework.AssertionFailedError\"><![CDATA[junit.framework.AssertionFailedError: <\n"
            +
            "\tat junit.framework.Assert.fail(Assert.java:47)\n" +
            "\tat wellFormedXmlFailures.TestSurefire3.testLower(TestSurefire3.java:30)\n" +
            "]]></failure>\n" +
            "  </testcase>\n" +
            "  <testcase time=\"0\" classname=\"wellFormedXmlFailures.TestSurefire3\" name=\"testU0000\">\n" +
            "    <failure message=\"&amp;0#;\" type=\"junit.framework.AssertionFailedError\">junit.framework.AssertionFailedError:  \n"
            +
            "\tat junit.framework.Assert.fail(Assert.java:47)\n" +
            "\tat wellFormedXmlFailures.TestSurefire3.testU0000(TestSurefire3.java:40)\n" +
            "</failure>\n" +
            "  </testcase>\n" +
            "  <testcase time=\"0\" classname=\"wellFormedXmlFailures.TestSurefire3\" name=\"testGreater\">\n" +
            "    <failure message=\"&gt;\" type=\"junit.framework.AssertionFailedError\">junit.framework.AssertionFailedError: >\n"
            +
            "\tat junit.framework.Assert.fail(Assert.java:47)\n" +
            "\tat wellFormedXmlFailures.TestSurefire3.testGreater(TestSurefire3.java:35)\n" +
            "</failure>\n" +
            "  </testcase>\n" +
            "  <testcase time=\"0\" classname=\"wellFormedXmlFailures.TestSurefire3\" name=\"testQuote\">\n" +
            "    <failure message=\"&quot;\" type=\"junit.framework.AssertionFailedError\">junit.framework.AssertionFailedError: \"\n"
            +
            "\tat junit.framework.Assert.fail(Assert.java:47)\n" +
            "\tat wellFormedXmlFailures.TestSurefire3.testQuote(TestSurefire3.java:25)\n" +
            "</failure>\n" +
            "  </testcase>\n" +
            "</testsuite>";
        InputStream byteArrayIs = new ByteArrayInputStream( xml.getBytes() );
        List<ReportTestSuite> parse = testSuiteXmlParser.parse( new InputStreamReader(byteArrayIs, "UTF-8") );
        assertThat( parse.size(), is( 1 ) );
        ReportTestSuite report = parse.get( 0 );
        assertThat( report.getFullClassName(), is( "wellFormedXmlFailures.TestSurefire3" ) );
        assertThat( report.getName(), is( "TestSurefire3" ) );
        assertThat( report.getPackageName(), is( "wellFormedXmlFailures" ) );
        assertThat( report.getNumberOfTests(), is( 4 ) );
        assertThat( report.getNumberOfSkipped(), is( 0 ) );
        assertThat( report.getNumberOfErrors(), is( 0 ) );
        assertThat( report.getNumberOfFailures(), is( 4 ) );
        assertThat( report.getNumberOfFlakes(), is( 0 ) );
        assertThat( report.getTimeElapsed(), is( 0.005f ) );
        assertThat( report.getTestCases().size(), is( 4 ) );

        List<ReportTestCase> tests = report.getTestCases();
        assertThat( tests.get( 0 ).getFullClassName(), is( "wellFormedXmlFailures.TestSurefire3" ) );
        assertThat( tests.get( 0 ).getName(), is( "testLower" ) );
        assertThat( tests.get( 0 ).getFailureDetail(),
                    is( "junit.framework.AssertionFailedError: <\n"
                            + "\tat junit.framework.Assert.fail(Assert.java:47)\n"
                            + "\tat wellFormedXmlFailures.TestSurefire3.testLower(TestSurefire3.java:30)\n" ) );
        assertThat( tests.get( 0 ).getClassName(), is( "TestSurefire3" ) );
        assertThat( tests.get( 0 ).getTime(), is( 0.005f ) );
        assertThat( tests.get( 0 ).getFailureErrorLine(), is( "30" ) );
        assertThat( tests.get( 0 ).getFailureMessage(), is( "<" ) );
        assertThat( tests.get( 0 ).getFullName(), is( "wellFormedXmlFailures.TestSurefire3.testLower" ) );
        assertThat( tests.get( 0 ).getFailureType(), is( "junit.framework.AssertionFailedError" ) );
        assertThat( tests.get( 0 ).hasError(), is( false ) );

        assertThat( tests.get( 1 ).getFullClassName(), is( "wellFormedXmlFailures.TestSurefire3" ) );
        assertThat( tests.get( 1 ).getName(), is( "testU0000" ) );
        assertThat( tests.get( 1 ).getFailureDetail(),
                    is( "junit.framework.AssertionFailedError:  \n"
                            + "\tat junit.framework.Assert.fail(Assert.java:47)\n"
                            + "\tat wellFormedXmlFailures.TestSurefire3.testU0000(TestSurefire3.java:40)\n" ) );
        assertThat( tests.get( 1 ).getClassName(), is( "TestSurefire3" ) );
        assertThat( tests.get( 1 ).getTime(), is( 0f ) );
        assertThat( tests.get( 1 ).getFailureErrorLine(), is( "40" ) );
        assertThat( tests.get( 1 ).getFailureMessage(), is( "&0#;" ) );
        assertThat( tests.get( 1 ).getFullName(), is( "wellFormedXmlFailures.TestSurefire3.testU0000" ) );
        assertThat( tests.get( 1 ).getFailureType(), is( "junit.framework.AssertionFailedError" ) );
        assertThat( tests.get( 1 ).hasError(), is( false ) );

        assertThat( tests.get( 2 ).getFullClassName(), is( "wellFormedXmlFailures.TestSurefire3" ) );
        assertThat( tests.get( 2 ).getName(), is( "testGreater" ) );
        assertThat( tests.get( 2 ).getFailureDetail(),
                    is( "junit.framework.AssertionFailedError: >\n"
                            + "\tat junit.framework.Assert.fail(Assert.java:47)\n"
                            + "\tat wellFormedXmlFailures.TestSurefire3.testGreater(TestSurefire3.java:35)\n" ) );
        assertThat( tests.get( 2 ).getClassName(), is( "TestSurefire3" ) );
        assertThat( tests.get( 2 ).getTime(), is( 0f ) );
        assertThat( tests.get( 2 ).getFailureErrorLine(), is( "35" ) );
        assertThat( tests.get( 2 ).getFailureMessage(), is( ">" ) );
        assertThat( tests.get( 2 ).getFullName(), is( "wellFormedXmlFailures.TestSurefire3.testGreater" ) );
        assertThat( tests.get( 2 ).getFailureType(), is( "junit.framework.AssertionFailedError" ) );
        assertThat( tests.get( 2 ).hasError(), is( false ) );

        assertThat( tests.get( 3 ).getFullClassName(), is( "wellFormedXmlFailures.TestSurefire3" ) );
        assertThat( tests.get( 3 ).getName(), is( "testQuote" ) );
        assertThat( tests.get( 3 ).getFailureDetail(),
                    is( "junit.framework.AssertionFailedError: \"\n"
                            + "\tat junit.framework.Assert.fail(Assert.java:47)\n"
                            + "\tat wellFormedXmlFailures.TestSurefire3.testQuote(TestSurefire3.java:25)\n" ) );
        assertThat( tests.get( 3 ).getClassName(), is( "TestSurefire3" ) );
        assertThat( tests.get( 3 ).getTime(), is( 0f ) );
        assertThat( tests.get( 3 ).getFailureErrorLine(), is( "25" ) );
        assertThat( tests.get( 3 ).getFailureMessage(), is( "\"" ) );
        assertThat( tests.get( 3 ).getFullName(), is( "wellFormedXmlFailures.TestSurefire3.testQuote" ) );
        assertThat( tests.get( 3 ).getFailureType(), is( "junit.framework.AssertionFailedError" ) );
        assertThat( tests.get( 3 ).hasError(), is( false ) );
    }

    @Test
    public void testParser()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );

        Collection<ReportTestSuite> oldResult = parser.parse(
            "src/test/resources/fixture/testsuitexmlparser/TEST-org.apache.maven.surefire.test.FailingTest.xml" );

        assertNotNull( oldResult );

        assertEquals( 1, oldResult.size() );
        ReportTestSuite next = oldResult.iterator().next();
        assertEquals( 2, next.getNumberOfTests() );
    }

    @Test
    public void successfulSurefireTestReport()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );
        File surefireReport = new File( "src/test/resources/junit-pathWithÜmlaut/TEST-umlautTest.BasicTest.xml" );
        assumeTrue( surefireReport.isFile() );
        Collection<ReportTestSuite> suites = parser.parse( surefireReport.getCanonicalPath() );
        assertNotNull( suites );
        assertEquals( 1, suites.size() );
        ReportTestSuite suite = suites.iterator().next();
        assertThat( suite.getNumberOfTests(), is( 1 ) );
        assertEquals( 1, suite.getNumberOfTests() );
        assertEquals( 0, suite.getNumberOfFlakes() );
        assertEquals( 0, suite.getNumberOfFailures() );
        assertEquals( 0, suite.getNumberOfErrors() );
        assertEquals( 0, suite.getNumberOfSkipped() );
        assertThat( suite.getTimeElapsed(), is( 0.002f ) );
        assertThat( suite.getFullClassName(), is( "umlautTest.BasicTest" ) );
        assertThat( suite.getPackageName(), is( "umlautTest" ) );
        assertThat( suite.getName(), is( "BasicTest" ) );
        ReportTestCase test = suite.getTestCases().iterator().next();
        assertTrue( test.isSuccessful() );
        assertNull( test.getFailureDetail() );
        assertNull( test.getFailureErrorLine() );
        assertNull( test.getFailureType() );
        assertThat( test.getTime(), is( 0.002f ) );
        assertThat( test.getFullClassName(), is( "umlautTest.BasicTest" ) );
        assertThat( test.getClassName(), is( "BasicTest" ) );
        assertThat( test.getName(), is( "testSetUp" ) );
        assertThat( test.getFullName(), is( "umlautTest.BasicTest.testSetUp" ) );
    }

    @Test
    public void testParserHitsFailsafeSummary()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );

        parser.parse( "src/test/resources/fixture/testsuitexmlparser/failsafe-summary.xml" );

        assertFalse( parser.isValid() );

        parser.parse(
            "src/test/resources/fixture/testsuitexmlparser/TEST-org.apache.maven.surefire.test.FailingTest.xml" );

        assertTrue( parser.isValid() );
    }

    @Test
    public void lastIndexOfPatternOfOrdinalTest()
    {
        final StringBuilder stackTrace = new StringBuilder(
            "\tat org.apache.Test.util(Test.java:60)\n"
                + "\tat org.apache.Test.test(Test.java:30)\n"
                + "\tat com.sun.Impl.xyz(Impl.java:258)\n" );

        int[] result = TestSuiteXmlParser.lastIndexOf( stackTrace, linePatterns );
        assertThat( result[0], is( 40 ) );
        assertThat( result[1], is( 0 ) );
        String errorLine = TestSuiteXmlParser.parseErrorLine( stackTrace, "org.apache.Test" );
        assertThat( errorLine, is( "30" ) );
    }

    @Test
    public void lastIndexOfPatternOfOrdinalTestWithCause()
    {
        final StringBuilder stackTrace = new StringBuilder(
            "\tat org.apache.Test.util(Test.java:60)\n"
                + "\tat org.apache.Test.test(Test.java:30)\n"
                + "\tat com.sun.Impl.xyz(Impl.java:258)\n"
                + "\tat Caused by: java.lang.IndexOutOfBoundsException\n"
                + "\tat org.apache.Test.util(Test.java:70)\n" );

        int[] result = TestSuiteXmlParser.lastIndexOf( stackTrace, linePatterns );
        assertThat( result[0], is( 40 ) );
        assertThat( result[1], is( 0 ) );
        String errorLine = TestSuiteXmlParser.parseErrorLine( stackTrace, "org.apache.Test" );
        assertThat( errorLine, is( "30" ) );
    }

    @Test
    public void lastIndexOfPatternOfEnclosedTest()
    {
        final StringBuilder source = new StringBuilder(
            "\tat org.apache.Test.util(Test.java:60)\n"
                + "\tat org.apache.Test$Nested.test(Test.java:30)\n"
                + "\tat com.sun.Impl.xyz(Impl.java:258)\n" );

        int[] result = TestSuiteXmlParser.lastIndexOf( source, linePatterns );
        assertThat( result[0], is( 40 ) );
        assertThat( result[1], is( 1 ) );
        String errorLine = TestSuiteXmlParser.parseErrorLine( source, "org.apache.Test$Nested" );
        assertThat( errorLine, is( "30" ) );
    }

    @Test
    public void lastIndexOfPatternOfEnclosedTestWithCause()
    {
        final StringBuilder source = new StringBuilder(
            "\tat org.apache.Test.util(Test.java:60)\n"
                + "\tat org.apache.Test$Nested.test(Test.java:30)\n"
                + "\tat com.sun.Impl.xyz(Impl.java:258)\n"
                + "\tat Caused by: java.lang.IndexOutOfBoundsException\n"
                + "\tat org.apache.Test$Nested.util(Test.java:70)\n" );

        int[] result = TestSuiteXmlParser.lastIndexOf( source, linePatterns );
        assertThat( result[0], is( 40 ) );
        assertThat( result[1], is( 1 ) );
        String errorLine = TestSuiteXmlParser.parseErrorLine( source, "org.apache.Test$Nested" );
        assertThat( errorLine, is( "30" ) );
    }

    @Test
    public void shouldParserEverythingInOrdinalTest()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );
        List<ReportTestSuite> tests =
            parser.parse( "src/test/resources/fixture/testsuitexmlparser/TEST-surefire.MyTest.xml" );
        assertTrue( parser.isValid() );
        assertThat( tests.size(), is( 1 ) );
        assertThat( tests.get( 0 ).getFullClassName(), is( "surefire.MyTest" ) );
        assertThat( tests.get( 0 ).getNumberOfErrors(), is( 1 ) );
        assertThat( tests.get( 0 ).getNumberOfFlakes(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfSkipped(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfFailures(), is( 0 ) );
        assertThat( tests.get( 0 ).getPackageName(), is( "surefire" ) );
        assertThat( tests.get( 0 ).getNumberOfTests(), is( 1 ) );
        assertThat( tests.get( 0 ).getTestCases().size(), is( 1 ) );
        assertFalse( tests.get( 0 ).getTestCases().get( 0 ).isSuccessful() );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureErrorLine(), is( "13" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureType(), is( "java.lang.RuntimeException" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullClassName(), is( "surefire.MyTest" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getClassName(), is( "MyTest" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getName(), is( "test" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullName(), is( "surefire.MyTest.test" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getTime(), is( 0.1f ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureMessage(), is( "this is different message" ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureDetail(),
                    is( "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
        + "\tat surefire.MyTest.test(MyTest.java:13)\n"
        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n"
        + "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
        + "\tat java.lang.reflect.Method.invoke(Method.java:606)\n"
        + "\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n"
        + "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n"
        + "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n"
        + "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n"
        + "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n"
        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n"
        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n"
        + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
        + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
        + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
        + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
        + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
        + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:272)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:167)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:147)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:130)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:211)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:163)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:105)\n"
        + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).hasError(), is( true ) );
    }

    @Test
    public void shouldParserEverythingInEnclosedTest()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );
        List<ReportTestSuite> tests =
            parser.parse( "src/test/resources/fixture/testsuitexmlparser/TEST-surefire.MyTest-enclosed.xml" );
        assertTrue( parser.isValid() );
        assertThat( tests.size(), is( 1 ) );
        assertThat( tests.get( 0 ).getFullClassName(), is( "surefire.MyTest$A" ) );
        assertThat( tests.get( 0 ).getNumberOfErrors(), is( 1 ) );
        assertThat( tests.get( 0 ).getNumberOfFlakes(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfSkipped(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfFailures(), is( 0 ) );
        assertThat( tests.get( 0 ).getPackageName(), is( "surefire" ) );
        assertThat( tests.get( 0 ).getNumberOfTests(), is( 1 ) );
        assertThat( tests.get( 0 ).getTestCases().size(), is( 1 ) );
        assertFalse( tests.get( 0 ).getTestCases().get( 0 ).isSuccessful() );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureErrorLine(), is( "45" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureType(),
                    is( "java.lang.RuntimeException" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullClassName(), is( "surefire.MyTest$A" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getClassName(), is( "MyTest$A" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getName(), is( "t" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullName(), is( "surefire.MyTest$A.t" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getTime(), is( 0f ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureMessage(),
                    is( "java.lang.IndexOutOfBoundsException" ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureDetail(),
                    is( "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
        + "\tat surefire.MyTest.access$200(MyTest.java:9)\n"
        + "\tat surefire.MyTest$A.t(MyTest.java:45)\n"
        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n"
        + "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
        + "\tat java.lang.reflect.Method.invoke(Method.java:606)\n"
        + "\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n"
        + "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n"
        + "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n"
        + "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n"
        + "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n"
        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n"
        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n"
        + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
        + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
        + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
        + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
        + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
        + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
        + "\tat org.junit.runners.Suite.runChild(Suite.java:128)\n"
        + "\tat org.junit.runners.Suite.runChild(Suite.java:27)\n"
        + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
        + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
        + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
        + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
        + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
        + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:272)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:167)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:147)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:130)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:211)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:163)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:105)\n"
        + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)\n" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).hasError(), is( true ) );
    }

    @Test
    public void shouldParserEverythingInEnclosedTrimStackTraceTest()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );
        List<ReportTestSuite> tests = parser.parse( "src/test/resources/fixture/testsuitexmlparser/"
                                                        + "TEST-surefire.MyTest-enclosed-trimStackTrace.xml" );
        assertTrue( parser.isValid() );
        assertThat( tests.size(), is( 1 ) );
        assertThat( tests.get( 0 ).getFullClassName(), is( "surefire.MyTest$A" ) );
        assertThat( tests.get( 0 ).getNumberOfErrors(), is( 1 ) );
        assertThat( tests.get( 0 ).getNumberOfFlakes(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfSkipped(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfFailures(), is( 0 ) );
        assertThat( tests.get( 0 ).getPackageName(), is( "surefire" ) );
        assertThat( tests.get( 0 ).getNumberOfTests(), is( 1 ) );
        assertThat( tests.get( 0 ).getTestCases().size(), is( 1 ) );
        assertFalse( tests.get( 0 ).getTestCases().get( 0 ).isSuccessful() );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureErrorLine(), is( "45" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureType(),
                    is( "java.lang.RuntimeException" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullClassName(), is( "surefire.MyTest$A" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getClassName(), is( "MyTest$A" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getName(), is( "t" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullName(), is( "surefire.MyTest$A.t" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getTime(), is( 0f ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureMessage(),
                    is( "java.lang.IndexOutOfBoundsException" ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureDetail(),
                    is( "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                            + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                            + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                            + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                            + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                            + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)\n"
                            + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                            + "\tat surefire.MyTest.access$200(MyTest.java:9)\n"
                            + "\tat surefire.MyTest$A.t(MyTest.java:45)\n" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).hasError(), is( true ) );
    }

    @Test
    public void shouldParserEverythingInNestedClassTest()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );
        List<ReportTestSuite> tests = parser.parse( "src/test/resources/fixture/testsuitexmlparser/"
                                                        + "TEST-surefire.MyTest-nestedClass.xml" );
        assertTrue( parser.isValid() );
        assertThat( tests.size(), is( 1 ) );
        assertThat( tests.get( 0 ).getFullClassName(), is( "surefire.MyTest" ) );
        assertThat( tests.get( 0 ).getNumberOfErrors(), is( 1 ) );
        assertThat( tests.get( 0 ).getNumberOfFlakes(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfSkipped(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfFailures(), is( 0 ) );
        assertThat( tests.get( 0 ).getPackageName(), is( "surefire" ) );
        assertThat( tests.get( 0 ).getNumberOfTests(), is( 1 ) );
        assertThat( tests.get( 0 ).getTestCases().size(), is( 1 ) );
        assertFalse( tests.get( 0 ).getTestCases().get( 0 ).isSuccessful() );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureErrorLine(), is( "13" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureType(),
                    is( "java.lang.RuntimeException" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullClassName(), is( "surefire.MyTest" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getClassName(), is( "MyTest" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getName(), is( "test" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullName(), is( "surefire.MyTest.test" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getTime(), is( 0f ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureMessage(),
                    is( "java.lang.IndexOutOfBoundsException" ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureDetail(),
                    is( "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
        + "\tat surefire.MyTest.test(MyTest.java:13)\n"
        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n"
        + "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
        + "\tat java.lang.reflect.Method.invoke(Method.java:606)\n"
        + "\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n"
        + "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n"
        + "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n"
        + "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n"
        + "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n"
        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n"
        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n"
        + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
        + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
        + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
        + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
        + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
        + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:272)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:167)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:147)\n"
        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:130)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:211)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:163)\n"
        + "\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:105)\n"
        + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).hasError(), is( true ) );
    }

    @Test
    public void shouldParserEverythingInNestedClassTrimStackTraceTest()
        throws Exception
    {
        TestSuiteXmlParser parser = new TestSuiteXmlParser( consoleLogger );
        List<ReportTestSuite> tests = parser.parse( "src/test/resources/fixture/testsuitexmlparser/"
                                                        + "TEST-surefire.MyTest-nestedClass-trimStackTrace.xml" );
        assertTrue( parser.isValid() );
        assertThat( tests.size(), is( 1 ) );
        assertThat( tests.get( 0 ).getFullClassName(), is( "surefire.MyTest" ) );
        assertThat( tests.get( 0 ).getNumberOfErrors(), is( 1 ) );
        assertThat( tests.get( 0 ).getNumberOfFlakes(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfSkipped(), is( 0 ) );
        assertThat( tests.get( 0 ).getNumberOfFailures(), is( 0 ) );
        assertThat( tests.get( 0 ).getPackageName(), is( "surefire" ) );
        assertThat( tests.get( 0 ).getNumberOfTests(), is( 1 ) );
        assertThat( tests.get( 0 ).getTestCases().size(), is( 1 ) );
        assertFalse( tests.get( 0 ).getTestCases().get( 0 ).isSuccessful() );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureErrorLine(), is( "13" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureType(),
                    is( "java.lang.RuntimeException" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullClassName(), is( "surefire.MyTest" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getClassName(), is( "MyTest" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getName(), is( "test" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFullName(), is( "surefire.MyTest.test" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getTime(), is( 0f ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureMessage(),
                    is( "java.lang.IndexOutOfBoundsException" ) );

        assertThat( tests.get( 0 ).getTestCases().get( 0 ).getFailureDetail(),
                    is( "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                            + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
                            + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                            + "\tat surefire.MyTest.test(MyTest.java:13)\n"
                            + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
                            + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                            + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                            + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                            + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                            + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)" ) );
        assertThat( tests.get( 0 ).getTestCases().get( 0 ).hasError(), is( true ) );
    }

    @Test
    public void shouldTestNotBlank()
    {
        assertFalse( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', ' ', ' ', '\n' ) );
        assertFalse( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', '\t', ' ', '\n' ) );
        assertFalse( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', ' ', '\r', '\n' ) );
        assertFalse( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', ' ', '\f', '\n' ) );
        assertTrue( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', 'a', ' ', '\n' ) );
        assertTrue( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', ' ', 'a', '\n' ) );
        assertTrue( TestSuiteXmlParser.isNotBlank( 1, 2, ' ', 'a', 'b', '\n' ) );
    }

    @Test
    public void shouldTestIsNumeric()
    {
        assertFalse( TestSuiteXmlParser.isNumeric( new StringBuilder( "0?5142" ), 1, 3 ) );
        assertTrue( TestSuiteXmlParser.isNumeric( new StringBuilder( "0?51M2" ), 2, 4 ) );
        assertFalse( TestSuiteXmlParser.isNumeric( new StringBuilder( "0?51M2" ), 2, 5 ) );
    }
}
