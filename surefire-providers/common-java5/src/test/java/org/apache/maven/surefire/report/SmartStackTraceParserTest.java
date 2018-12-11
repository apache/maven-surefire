package org.apache.maven.surefire.report;

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

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;
import org.apache.maven.surefire.util.internal.DaemonThreadFactory;

import static org.apache.maven.surefire.report.SmartStackTraceParser.*;

@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
public class SmartStackTraceParserTest
    extends TestCase
{
    public void testGetString()
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.failInAssert();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.failInAssert:30 X is not Z", res );
        }
    }

    public void testGetStringFromNested()
    {
        OutermostClass aTestClass = new OutermostClass();
        try
        {
            aTestClass.junit();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.failInAssert:30 X is not Z", res );
        }
    }

    public void testGetStringWithMethod()
    {
        OutermostClass aTestClass = new OutermostClass();
        try
        {
            aTestClass.junit();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( InnerATestClass.class.getName(), e, "myMethod" );
            String res = smartStackTraceParser.getString();
            assertEquals( "InnerATestClass.myMethod X is not Z", res );
        }
    }

    public void testNestedFailure()
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.nestedFailInAssert();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.nestedFailInAssert:35->failInAssert:30 X is not Z", res );
        }
    }

    public void testNestedNpe()
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.nestedNpe();
        }
        catch ( NullPointerException e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.nestedNpe:45->npe:40 NullPointer It was null", res );
        }
    }

    public void testNestedNpeOutsideTest()
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.nestedNpeOutsideTest();
        }
        catch ( NullPointerException e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.nestedNpeOutsideTest:55->npeOutsideTest:50 » NullPointer", res );
        }
    }

    public void testLongMessageTruncation()
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.aLongTestErrorMessage();
        }
        catch ( RuntimeException e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.aLongTestErrorMessage:60 Runtime This message will be truncated, so...",
                    res );
        }
    }

    public void testFailureInBaseClass()
    {
        ASubClass aTestClass = new ASubClass();
        try
        {
            aTestClass.npe();
        }
        catch ( NullPointerException e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ASubClass.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "ASubClass>ABaseClass.npe:27 » NullPointer It was null", res );
        }
    }

    public void testClassThatWillFail()
    {
        CaseThatWillFail aTestClass = new CaseThatWillFail();
        try
        {
            aTestClass.testThatWillFail();
        }
        catch ( ComparisonFailure e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( CaseThatWillFail.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "CaseThatWillFail.testThatWillFail:29 expected:<[abc]> but was:<[def]>", res );
        }
    }

    private static Throwable getAThrownException()
    {
        try
        {
            TestClass1.InnerBTestClass.throwSomething();
        }
        catch ( Throwable t )
        {
            return t;
        }
        return null;
    }

    public void testCollections()
    {
        Throwable aThrownException = getAThrownException();
        List<StackTraceElement> innerMost =
            focusInsideClass( aThrownException.getCause().getStackTrace(),
                              new ClassNameStackTraceFilter( TestClass1.InnerBTestClass.class.getName() ) );
        assertEquals( 2, innerMost.size() );
        StackTraceElement inner = innerMost.get( 0 );
        assertEquals( TestClass1.InnerBTestClass.class.getName(), inner.getClassName() );
        StackTraceElement outer = innerMost.get( 1 );
        assertEquals( TestClass1.InnerBTestClass.class.getName(), outer.getClassName() );
    }

    public void testAssertionWithNoMessage()
    {
        try
        {
            new AssertionNoMessage().testThrowSomething();
        }
        catch ( ComparisonFailure e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( AssertionNoMessage.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "AssertionNoMessage.testThrowSomething:29 expected:<[abc]> but was:<[xyz]>", res );
        }
    }

    public void testFailWithFail()
    {
        try
        {
            new FailWithFail().testThatWillFail();
        }
        catch ( AssertionFailedError e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( FailWithFail.class.getName(), e, null );
            String res = smartStackTraceParser.getString();
            assertEquals( "FailWithFail.testThatWillFail:29 abc", res );
        }
    }

    public void testCollectorWithNested()
    {
        try
        {
            InnerATestClass.testFake();
        }
        catch ( Throwable t )
        {
            List<StackTraceElement> stackTraceElements =
                focusInsideClass( t.getStackTrace(),
                                  new ClassNameStackTraceFilter( InnerATestClass.class.getName() ) );
            assertNotNull( stackTraceElements );
            assertEquals( 2, stackTraceElements.size() );
            StackTraceElement innerMost = stackTraceElements.get( 0 );
            assertEquals( InnerATestClass.class.getName(), innerMost.getClassName() );
            StackTraceElement outer = stackTraceElements.get( 1 );
            assertEquals( InnerATestClass.class.getName(), outer.getClassName() );
        }
    }

    public void testNonClassNameStacktrace()
    {
        SmartStackTraceParser smartStackTraceParser =
            new SmartStackTraceParser( "Not a class name", new Throwable( "my message" ), null );
        assertEquals( "my message", smartStackTraceParser.getString() );
    }

    public void testNullElementInStackTrace()
        throws Exception
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.failInAssert();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser =
                    new SmartStackTraceParser( ATestClass.class.getName(), e, null );
            Field stackTrace = SmartStackTraceParser.class.getDeclaredField( "stackTrace" );
            stackTrace.setAccessible( true );
            stackTrace.set( smartStackTraceParser, new StackTraceElement[0] );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass X is not Z", res );
        }
    }

    public void testSingleNestedWithThread()
    {
        ExecutionException e = getSingleNested();
        String name = getClass().getName();
        Throwable focus = findTopmostWithClass( e, new ClassNameStackTraceFilter( name ) );
        assertSame( e, focus );
        List<StackTraceElement> stackTraceElements =
            focusInsideClass( focus.getStackTrace(), new ClassNameStackTraceFilter( name ) );
        assertEquals( stackTraceElements.get( stackTraceElements.size() - 1 ).getClassName(), name );
    }

    public void testDoubleNestedWithThread()
    {
        ExecutionException e = getDoubleNestedException();

        String name = getClass().getName();
        Throwable focus = findTopmostWithClass( e, new ClassNameStackTraceFilter( name ) );
        assertSame( e, focus );
        List<StackTraceElement> stackTraceElements =
            focusInsideClass( focus.getStackTrace(), new ClassNameStackTraceFilter( name ) );
        assertEquals( stackTraceElements.get( stackTraceElements.size() - 1 ).getClassName(), name );

        name = RunnableTestClass1.class.getName();
        focus = findTopmostWithClass( e, new ClassNameStackTraceFilter( name ) );
        assertSame( e.getCause(), focus );
        stackTraceElements = focusInsideClass( focus.getStackTrace(), new ClassNameStackTraceFilter( name ) );
        assertEquals( stackTraceElements.get( stackTraceElements.size() - 1 ).getClassName(), name );
    }

    public void testStackTraceWithFocusOnClassAsString()
    {
        try
        {
            new StackTraceFocusedOnClass.C().c();
            fail();
        }
        catch ( Exception e )
        {
            String trace = stackTraceWithFocusOnClassAsString( e, StackTraceFocusedOnClass.B.class.getName() );

            assertEquals( "java.lang.RuntimeException: java.lang.IllegalStateException: java.io.IOException: I/O error\n"
            + "\tat org.apache.maven.surefire.report.StackTraceFocusedOnClass$B.b(StackTraceFocusedOnClass.java:65)\n"
            + "Caused by: java.lang.IllegalStateException: java.io.IOException: I/O error\n"
            + "\tat org.apache.maven.surefire.report.StackTraceFocusedOnClass$B.b(StackTraceFocusedOnClass.java:61)\n"
            + "Caused by: java.io.IOException: I/O error\n"
            + "\tat org.apache.maven.surefire.report.StackTraceFocusedOnClass$B.abs(StackTraceFocusedOnClass.java:73)\n"
            + "\tat org.apache.maven.surefire.report.StackTraceFocusedOnClass$B.b(StackTraceFocusedOnClass.java:61)\n",
            trace );
        }
    }

    private ExecutionException getSingleNested()
    {
        FutureTask<Object> futureTask = new FutureTask<>( new RunnableTestClass2() );
        DaemonThreadFactory.newDaemonThread( futureTask ).start();
        try
        {
            futureTask.get();
        }
        catch ( InterruptedException e )
        {
            fail();
        }
        catch ( ExecutionException e )
        {
            return e;
        }
        fail();
        return null;
    }

    private ExecutionException getDoubleNestedException()
    {
        FutureTask<Object> futureTask = new FutureTask<>( new RunnableTestClass1() );
        DaemonThreadFactory.newDaemonThread( futureTask ).start();
        try
        {
            futureTask.get();
        }
        catch ( InterruptedException e )
        {
            fail();
        }
        catch ( ExecutionException e )
        {
            return e;
        }
        return null;
    }
}
