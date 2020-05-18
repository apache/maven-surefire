package org.apache.maven.surefire.junit4;

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

import java.util.Collections;
import java.util.Set;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

import junit.framework.TestCase;
import junit.framework.TestResult;
import org.junit.Test;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4TestCheckerTest
    extends TestCase
{
    private final JUnit4TestChecker jUnit4TestChecker = new JUnit4TestChecker( this.getClass().getClassLoader() );

    public void testValidJunit4Annotated()
        throws TestSetFailedException
    {
        assertTrue( jUnit4TestChecker.accept( JUnit4TestCheckerTest.class ) );
    }

    public void testValidJunit4itsAJunit3Test()
        throws TestSetFailedException
    {
        assertTrue( jUnit4TestChecker.accept( AlsoValid.class ) );
    }

    public void testValidJunitSubclassWithoutOwnTestmethods()
        throws TestSetFailedException
    {
        assertTrue( jUnit4TestChecker.accept( SubClassWithoutOwnTestMethods.class ) );
    }

    public void testValidSuite()
        throws TestSetFailedException
    {
        assertTrue( jUnit4TestChecker.accept( SuiteValid1.class ) );
    }

    public void testValidCustomSuite()
        throws TestSetFailedException
    {
        assertTrue( jUnit4TestChecker.accept( SuiteValid2.class ) );
    }

    public void testValidCustomRunner()
        throws TestSetFailedException
    {
        assertTrue( jUnit4TestChecker.accept( SuiteValidCustomRunner.class ) );
    }

    public void testInvalidTest()
        throws TestSetFailedException
    {
        assertFalse( jUnit4TestChecker.accept( NotValidTest.class ) );
    }

    public void testDontAcceptAbstractClasses()
    {
        assertFalse( jUnit4TestChecker.accept( BaseClassWithTest.class ) );
    }

    public void testSuiteOnlyTest()
    {
        assertTrue( jUnit4TestChecker.accept( SuiteOnlyTest.class ) );
    }

    public void testCustomSuiteOnlyTest()
    {
        assertTrue( jUnit4TestChecker.accept( CustomSuiteOnlyTest.class ) );
    }

    public void testInnerClassNotAutomaticallyTc()
    {
        assertTrue( jUnit4TestChecker.accept( NestedTC.class ) );
        assertFalse( jUnit4TestChecker.accept( NestedTC.Inner.class ) );
    }

    public void testCannotLoadRunWithAnnotation()
        throws Exception
    {
        Class testClass = SimpleJUnit4TestClass.class;
        ClassLoader testClassLoader = testClass.getClassLoader();
        // Emulate an OSGi classloader which filters on package level.
        // Use a classloader which can only load classes in package org.junit,
        // e.g. org.junit.Test, but no classes from other packages,
        // in particular org.junit.runner.RunWith can't be loaded
        Set<String> visiblePackages = Collections.singleton( "org.junit" );
        PackageFilteringClassLoader filteringTestClassloader =
            new PackageFilteringClassLoader( testClassLoader, visiblePackages );
        JUnit4TestChecker checker = new JUnit4TestChecker( filteringTestClassloader );
        assertTrue( checker.accept( testClass ) );
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
    @SuppressWarnings( { "UnusedDeclaration" } )
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
    {
        @Test
        public void weAreAlsoATest()
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

    /**
     *
     */
    @RunWith( Suite.class )
    public static class SuiteValid1
    {
        public void testSomething()
        {

        }
    }

    class CustomRunner
        extends Runner
    {
        @Override
        public Description getDescription()
        {
            return Description.createSuiteDescription( "CustomRunner" );
        }

        @Override
        public void run( RunNotifier runNotifier )
        {
        }
    }

    /**
     *
     */
    @RunWith( CustomRunner.class )
    public static class SuiteValidCustomRunner
    {
        public void testSomething()
        {

        }
    }


    /**
     *
     */
    @RunWith( MySuite.class )
    public static class SuiteValid2
    {
        public void testSomething()
        {

        }
    }

    /**
     *
     */
    public static class SimpleJUnit4TestClass
    {
        @Test
        public void testMethod()
        {
        }
    }

    class MySuite
        extends Suite
    {
        MySuite( Class<?> klass )
            throws InitializationError
        {
            super( klass );
        }
    }

    class NestedTC
        extends TestCase
    {
        public class Inner
        {

        }
    }
}
