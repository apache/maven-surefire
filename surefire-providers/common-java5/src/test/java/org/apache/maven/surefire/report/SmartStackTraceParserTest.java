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

import java.util.List;

import junit.framework.Assert;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

@SuppressWarnings( "ThrowableResultOfMethodCallIgnored" )
public class SmartStackTraceParserTest
    extends TestCase
{

    static class AssertionNoMessage
        extends TestCase
    {
        public void testThrowSomething()
        {
            assertEquals( "abc", "xyz" );
        }
    }

    static class ADifferen0tTestClass
    {
        static class InnerATestClass
        {
            public static void testFake()
            {
                innerMethod();
            }

            private static void innerMethod()
            {
                Assert.assertTrue( false );
            }
        }
    }

    static class CaseThatWillFail
        extends TestCase
    {
        public void testThatWillFail()
        {
            assertEquals( "abc", "def" );
        }
    }

    static class TestClass2
    {
        static class InnerCTestClass
        {
            public static void cThrows()
                throws Exception
            {
                throw new Exception( "Hey ho, hey ho, a throwable we throw!" );
            }
        }
    }

    static class TestClass1
    {
        static class InnerBTestClass
        {
            public static void throwSomething()
            {
                innerThrowSomething();
            }

            public static void innerThrowSomething()
            {
                try
                {
                    TestClass2.InnerCTestClass.cThrows();
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        }
    }

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
            assertEquals( "SmartStackTraceParserTest$CaseThatWillFail.testThatWillFail:62 expected:<abc> but was:<def>",
                          res );
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
            SmartStackTraceParser.focusInsideClass( aThrownException.getCause().getStackTrace(),
                                                    TestClass1.InnerBTestClass.class.getName() );
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
            assertEquals(
                "SmartStackTraceParserTest$AssertionNoMessage.testThrowSomething:37 expected:<abc> but was:<xyz>",
                res );
        }
    }

    public void testCollectorWithNested()
    {
        try
        {
            ADifferen0tTestClass.InnerATestClass.testFake();
        }
        catch ( Throwable t )
        {
            List<StackTraceElement> stackTraceElements = SmartStackTraceParser.focusInsideClass( t.getStackTrace(),
                                                                                                 ADifferen0tTestClass.InnerATestClass.class.getName() );
            assertNotNull( stackTraceElements );
            assertEquals( 5, stackTraceElements.size() );
            StackTraceElement innerMost = stackTraceElements.get( 0 );
            assertEquals( Assert.class.getName(), innerMost.getClassName() );
            StackTraceElement outer = stackTraceElements.get( 4 );
            assertEquals( ADifferen0tTestClass.InnerATestClass.class.getName(), outer.getClassName() );
        }
    }

}
