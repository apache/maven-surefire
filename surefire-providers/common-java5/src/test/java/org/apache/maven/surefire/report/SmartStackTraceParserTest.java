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
import junit.framework.TestCase;

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
            Assert.assertTrue( "ATestClass#failInAssert(30) X is not Z".equals( res ) );

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
            Assert.assertTrue( "ATestClass#nestedFailInAssert(35).failInAssert(30) X is not Z".equals( res ) );

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
            Assert.assertTrue( "ATestClass#nestedNpe(45).npe(40) NullPointerException It was null".equals( res ) );

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
            Assert.assertTrue(
                "ATestClass#nestedNpeOutsideTest(55).npeOutsideTest(50) >> NullPointerException".equals( res ) );

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
            Assert.assertTrue(
                "ATestClass#aLongTestErrorMessage(60) RuntimeException This message will be tru...".equals( res ) );

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

        aThrownException.printStackTrace();
        List<StackTraceElement> innerMost =
            SmartStackTraceParser.focusInsideClass( aThrownException.getCause().getStackTrace(),
                                                    TestClass1.InnerBTestClass.class.getName() );
        assertEquals( 3, innerMost.size() );
        StackTraceElement inner = innerMost.get( 0 );
        assertEquals( TestClass2.InnerCTestClass.class.getName(), inner.getClassName() );
        StackTraceElement outer = innerMost.get( 2 );
        assertEquals( TestClass1.InnerBTestClass.class.getName(), outer.getClassName() );
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
