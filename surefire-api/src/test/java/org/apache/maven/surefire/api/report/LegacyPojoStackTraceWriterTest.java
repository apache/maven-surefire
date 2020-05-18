package org.apache.maven.surefire.api.report;

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

import java.io.PrintWriter;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class LegacyPojoStackTraceWriterTest extends TestCase
{

    @SuppressWarnings( "checkstyle:linelength" )
    public void testWriteTrimmedTraceToString()
    {
        String stackTrace = "junit.framework.AssertionFailedError: blah\n"
                + "    at junit.framework.Assert.fail(Assert.java:47)\n"
                + "    at TestSurefire3.testQuote(TestSurefire3.java:23)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
                + "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
                + "    at java.lang.reflect.Method.invoke(Method.java:585)\n"
                + "    at junit.framework.TestCase.runTest(TestCase.java:154)\n"
                + "    at junit.framework.TestCase.runBare(TestCase.java:127)\n"
                + "    at junit.framework.TestResult$1.protect(TestResult.java:106)\n"
                + "    at junit.framework.TestResult.runProtected(TestResult.java:124)\n"
                + "    at junit.framework.TestResult.run(TestResult.java:109)\n"
                + "    at junit.framework.TestCase.run(TestCase.java:118)\n"
                + "    at junit.framework.TestSuite.runTest(TestSuite.java:208)\n"
                + "    at junit.framework.TestSuite.run(TestSuite.java:203)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
                + "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
                + "    at java.lang.reflect.Method.invoke(Method.java:585)\n"
                + "    at org.apache.maven.surefire.junit.JUnitTestSet.execute(JUnitTestSet.java:213)\n"
                + "    at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.executeTestSet(AbstractDirectoryTestSuite.java:140)\n"
                + "    at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.execute(AbstractDirectoryTestSuite.java:127)\n"
                + "    at org.apache.maven.surefire.Surefire.run(Surefire.java:132)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
                + "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
                + "    at java.lang.reflect.Method.invoke(Method.java:585)\n"
                + "    at org.apache.maven.surefire.booter.SurefireBooter.runSuitesInProcess(SurefireBooter.java:318)\n"
                + "    at org.apache.maven.surefire.booter.SurefireBooter.main(SurefireBooter.java:956)\n";
        MockThrowable t = new MockThrowable( stackTrace );
        LegacyPojoStackTraceWriter w = new LegacyPojoStackTraceWriter( "TestSurefire3", "testQuote", t );
        String out = w.writeTrimmedTraceToString();
        String expected = "junit.framework.AssertionFailedError: blah\n" + "    at junit.framework.Assert.fail(Assert.java:47)\n" + "    at TestSurefire3.testQuote(TestSurefire3.java:23)\n";
        assertEquals( expected, out );
    }

    @SuppressWarnings( "checkstyle:linelength" )
    public void testCausedBy()
    {
        String stackTrace = "java.lang.RuntimeException: blah\n"
                + "    at TestSurefire3.testBlah(TestSurefire3.java:45)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
                + "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
                + "    at java.lang.reflect.Method.invoke(Method.java:585)\n"
                + "    at junit.framework.TestCase.runTest(TestCase.java:154)\n"
                + "    at junit.framework.TestCase.runBare(TestCase.java:127)\n"
                + "    at junit.framework.TestResult$1.protect(TestResult.java:106)\n"
                + "    at junit.framework.TestResult.runProtected(TestResult.java:124)\n"
                + "    at junit.framework.TestResult.run(TestResult.java:109)\n"
                + "    at junit.framework.TestCase.run(TestCase.java:118)\n"
                + "    at junit.framework.TestSuite.runTest(TestSuite.java:208)\n"
                + "    at junit.framework.TestSuite.run(TestSuite.java:203)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
                + "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
                + "    at java.lang.reflect.Method.invoke(Method.java:585)\n"
                + "    at org.apache.maven.surefire.junit.JUnitTestSet.execute(JUnitTestSet.java:213)\n"
                + "    at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.executeTestSet(AbstractDirectoryTestSuite.java:140)\n"
                + "    at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.execute(AbstractDirectoryTestSuite.java:127)\n"
                + "    at org.apache.maven.surefire.Surefire.run(Surefire.java:132)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                + "    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)\n"
                + "    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)\n"
                + "    at java.lang.reflect.Method.invoke(Method.java:585)\n"
                + "    at org.apache.maven.surefire.booter.SurefireBooter.runSuitesInProcess(SurefireBooter.java:318)\n"
                + "    at org.apache.maven.surefire.booter.SurefireBooter.main(SurefireBooter.java:956)\n"
                + "Caused by: junit.framework.AssertionFailedError: \"\n"
                + "    at junit.framework.Assert.fail(Assert.java:47)\n"
                + "    at TestSurefire3.testQuote(TestSurefire3.java:23)\n"
                + "    at TestSurefire3.testBlah(TestSurefire3.java:43)\n"
                + "    at TestSurefire3.testBlah(TestSurefire3.java:43)\n"
                + "    ... 26 more\n";
        MockThrowable t = new MockThrowable( stackTrace );
        LegacyPojoStackTraceWriter w = new LegacyPojoStackTraceWriter( "TestSurefire3", "testBlah", t );
        String out = w.writeTrimmedTraceToString();
        String expected = "java.lang.RuntimeException: blah\n"
                + "    at TestSurefire3.testBlah(TestSurefire3.java:45)\n"
                + "Caused by: junit.framework.AssertionFailedError: \"\n"
                + "    at junit.framework.Assert.fail(Assert.java:47)\n"
                + "    at TestSurefire3.testQuote(TestSurefire3.java:23)\n"
                + "    at TestSurefire3.testBlah(TestSurefire3.java:43)\n"
                + "    at TestSurefire3.testBlah(TestSurefire3.java:43)\n"
                + "    ... 26 more\n";
        assertEquals( expected, out );
    }

    static class MockThrowable extends Throwable
    {
        private static final long serialVersionUID = 1L;

        private final String stackTrace;

        MockThrowable( String stackTrace )
        {
            this.stackTrace = stackTrace;
        }

        @Override
        public void printStackTrace( PrintWriter s )
        {
            s.write( stackTrace );
        }
    }

    public void testMultiLineMessage()
    {
        String msg = "assert \"foo\" == \"bar\"\n"
                + "             |\n"
                + "             false";
        try
        {
            throw new RuntimeException( msg );
        }
        catch ( Throwable t )
        {
            LegacyPojoStackTraceWriter writer = new LegacyPojoStackTraceWriter( null, null, t );
            String stackTrace = writer.writeTraceToString();
            assertTrue( stackTrace.startsWith( "java.lang.RuntimeException: \n" + msg ) );
        }
    }
}
