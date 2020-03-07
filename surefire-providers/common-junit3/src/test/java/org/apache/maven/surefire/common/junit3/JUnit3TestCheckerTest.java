package org.apache.maven.surefire.common.junit3;

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
import junit.framework.TestResult;

/**
 * @author Kristian Rosenvold
 */
public class JUnit3TestCheckerTest
    extends TestCase
{
    private final JUnit3TestChecker jUnit3TestChecker = new JUnit3TestChecker( this.getClass().getClassLoader() );

    public void testValidJunit4Annotated()
    {
        assertTrue( jUnit3TestChecker.accept( JUnit3TestCheckerTest.class ) );
    }

    public void testValidJunit4itsAJunit3Test()
    {
        assertTrue( jUnit3TestChecker.accept( AlsoValid.class ) );
    }

    public void testValidJunitSubclassWithoutOwnTestmethods()
    {
        assertTrue( jUnit3TestChecker.accept( SubClassWithoutOwnTestMethods.class ) );
    }

    public void testInvalidTest()
    {
        assertFalse( jUnit3TestChecker.accept( NotValidTest.class ) );
    }

    public void testDontAcceptAbstractClasses()
    {
        assertFalse( jUnit3TestChecker.accept( BaseClassWithTest.class ) );
    }

    public void testSuiteOnlyTest()
    {
        assertTrue( jUnit3TestChecker.accept( SuiteOnlyTest.class ) );
    }

    public void testCustomSuiteOnlyTest()
    {
        assertTrue( jUnit3TestChecker.accept( CustomSuiteOnlyTest.class ) );
    }

    public void testIinnerClassNotAutomaticallyTc()
    {
        assertTrue( jUnit3TestChecker.accept( NestedTC.class ) );
        assertFalse( jUnit3TestChecker.accept( NestedTC.Inner.class ) );
    }

    /**
     *
     */
    public static class AlsoValid
        extends TestCase
    {
        public void testSomething()
        {

        }
    }

    /**
     *
     */
    public static class SuiteOnlyTest
    {
        public static junit.framework.Test suite()
        {
            return null;
        }
    }

    /**
     *
     */
    public static class CustomSuiteOnlyTest
    {
        public static MySuite2 suite()
        {
            return null;
        }
    }

    /**
     *
     */
    public static class MySuite2
        implements junit.framework.Test
    {
        @Override
        public int countTestCases()
        {
            return 0;
        }

        @Override
        public void run( TestResult testResult )
        {
        }
    }


    /**
     *
     */
    public static class NotValidTest
    {
        public void testSomething()
        {
        }
    }

    /**
     *
     */
    public abstract static class BaseClassWithTest
        extends TestCase
    {
        public void testWeAreAlsoATest()
        {
        }
    }

    /**
     *
     */
    public static class SubClassWithoutOwnTestMethods
        extends BaseClassWithTest
    {
    }

    class NestedTC
        extends TestCase
    {
        public class Inner
        {

        }
    }

}
