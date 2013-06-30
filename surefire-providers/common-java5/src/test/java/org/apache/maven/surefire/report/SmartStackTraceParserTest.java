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

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

import static org.apache.maven.surefire.report.SmartStackTraceParser.findInnermostWithClass;
import static org.apache.maven.surefire.report.SmartStackTraceParser.focusInsideClass;

@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
public class SmartStackTraceParserTest
    extends TestCase
{

    public void testGetString()
        throws Exception
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.failInAssert();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.failInAssert:30 X is not Z", res );

        }

    }

    public void testNestedFailure()
        throws Exception
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.nestedFailInAssert();
        }
        catch ( AssertionError e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.nestedFailInAssert:35->failInAssert:30 X is not Z", res );
        }
    }

    public void testNestedNpe()
        throws Exception
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.nestedNpe();
        }
        catch ( NullPointerException e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.nestedNpe:45->npe:40 NullPointer It was null", res );

        }
    }

    public void testNestedNpeOutsideTest()
        throws Exception
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.nestedNpeOutsideTest();
        }
        catch ( NullPointerException e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.nestedNpeOutsideTest:55->npeOutsideTest:50 » NullPointer", res );

        }
    }

    public void testLongMessageTruncation()
        throws Exception
    {
        ATestClass aTestClass = new ATestClass();
        try
        {
            aTestClass.aLongTestErrorMessage();
        }
        catch ( RuntimeException e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "ATestClass.aLongTestErrorMessage:60 Runtime This message will be truncated, so...", res );

        }
    }

    public void testFailureInBaseClass()
        throws Exception
    {
        ASubClass aTestClass = new ASubClass();
        try
        {
            aTestClass.npe();
        }
        catch ( NullPointerException e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ASubClass.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "ASubClass>ABaseClass.npe:27 » NullPointer It was null", res );
        }
    }

    public void testClassThatWillFail()
        throws Exception
    {
        CaseThatWillFail aTestClass = new CaseThatWillFail();
        try
        {
            aTestClass.testThatWillFail();
        }
        catch ( ComparisonFailure e )
        {
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( CaseThatWillFail.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "CaseThatWillFail.testThatWillFail:29 expected:<abc> but was:<def>", res );
        }
    }

    public Throwable getAThrownException()
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
            focusInsideClass( aThrownException.getCause().getStackTrace(), TestClass1.InnerBTestClass.class.getName() );
        assertEquals( 3, innerMost.size() );
        StackTraceElement inner = innerMost.get( 0 );
        assertEquals( TestClass2.InnerCTestClass.class.getName(), inner.getClassName() );
        StackTraceElement outer = innerMost.get( 2 );
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
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( AssertionNoMessage.class, e );
            String res = smartStackTraceParser.getString();
            assertEquals( "AssertionNoMessage.testThrowSomething:29 expected:<abc> but was:<xyz>", res );
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
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( FailWithFail.class, e );
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
                focusInsideClass( t.getStackTrace(), InnerATestClass.class.getName() );
            assertNotNull( stackTraceElements );
            assertEquals( 5, stackTraceElements.size() );
            StackTraceElement innerMost = stackTraceElements.get( 0 );
            assertEquals( Assert.class.getName(), innerMost.getClassName() );
            StackTraceElement outer = stackTraceElements.get( 4 );
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
            SmartStackTraceParser smartStackTraceParser = new SmartStackTraceParser( ATestClass.class, e );
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
        String name = this.getClass().getName();
        Throwable focus = findInnermostWithClass( e, name );
        assertEquals( e, focus );
        List<StackTraceElement> stackTraceElements = focusInsideClass( focus.getStackTrace(), name );
        assertEquals( stackTraceElements.get( stackTraceElements.size() - 1 ).getClassName(), name );
    }


    public void testDoubleNestedWithThread()
    {
        ExecutionException e = getDoubleNestedException();

        String name = this.getClass().getName();
        Throwable focus = findInnermostWithClass( e, name );
        assertEquals( e, focus );
        List<StackTraceElement> stackTraceElements = focusInsideClass( focus.getStackTrace(), name );
        assertEquals( stackTraceElements.get( stackTraceElements.size() - 1 ).getClassName(), name );

        name = "org.apache.maven.surefire.report.RunnableTestClass1";
        focus = findInnermostWithClass( e, name );
        assertEquals( e.getCause(), focus );
        stackTraceElements = focusInsideClass( focus.getStackTrace(), name );
        assertEquals( stackTraceElements.get( stackTraceElements.size() - 1 ).getClassName(), name );

    }

    public ExecutionException getSingleNested()
    {
        FutureTask<Object> futureTask = new FutureTask<Object>( new RunnableTestClass2() );
        new Thread( futureTask ).start();
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
        FutureTask<Object> futureTask = new FutureTask<Object>( new RunnableTestClass1() );
        new Thread( futureTask ).start();
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
