package org.apache.maven.surefire.junitcore;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.maven.surefire.junit4.MockReporter;

import junit.framework.TestCase;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author Kristian Rosenvold
 */
public class JUnitCoreRunListenerTest
    extends TestCase
{
    public void testTestRunStarted()
        throws Exception
    {
        RunListener jUnit4TestSetReporter =
            new JUnitCoreRunListener( new MockReporter(), new HashMap<String, TestSet>() );
        JUnitCore core = new JUnitCore();
        core.addListener( jUnit4TestSetReporter );
        Result result = core.run( new Computer(), STest1.class, STest2.class );
        core.removeListener( jUnit4TestSetReporter );
        assertEquals( 2, result.getRunCount() );
    }

    public void testFailedAssumption()
        throws Exception
    {
        RunListener jUnit4TestSetReporter =
            new JUnitCoreRunListener( new MockReporter(), new HashMap<String, TestSet>() );
        JUnitCore core = new JUnitCore();
        core.addListener( jUnit4TestSetReporter );
        Result result = core.run( new Computer(), TestWithAssumptionFailure.class );
        core.removeListener( jUnit4TestSetReporter );
        assertEquals( 1, result.getRunCount() );
    }

    public void testStateForClassesWithNoChildren()
        throws Exception
    {
        Description testDescription =
            Description.createSuiteDescription( "testMethod(cannot.be.loaded.by.junit.Test)" );
        Description st1 = Description.createSuiteDescription( STest1.class);
//        st1.addChild( Description.createSuiteDescription( STest1.class ) );
        testDescription.addChild( st1 );
        Description st2 = Description.createSuiteDescription( STest2.class);
  //      st2.addChild( Description.createSuiteDescription( STest2.class ) );
        testDescription.addChild( st2 );

        Map<String, TestSet> classMethodCounts = new HashMap<>();
        JUnitCoreRunListener listener = new JUnitCoreRunListener( new MockReporter(), classMethodCounts );
        listener.testRunStarted( testDescription );
        assertEquals( 2, classMethodCounts.size() );
        Iterator<TestSet> iterator = classMethodCounts.values().iterator();
        assertFalse(iterator.next().equals( iterator.next() ));
    }

    public void testTestClassNotLoadableFromJUnitClassLoader()
        throws Exception
    {
        // can't use Description.createTestDescription() methods as these require a loaded Class
        Description testDescription =
            Description.createSuiteDescription( "testMethod(cannot.be.loaded.by.junit.Test)" );
        assertEquals( "testMethod", testDescription.getMethodName() );
        assertEquals( "cannot.be.loaded.by.junit.Test", testDescription.getClassName() );
        // assert that the test class is not visible by the JUnit classloader
        assertNull( testDescription.getTestClass() );
        Description suiteDescription = Description.createSuiteDescription( "testSuite" );
        suiteDescription.addChild( testDescription );
        Map<String, TestSet> classMethodCounts = new HashMap<>();
        JUnitCoreRunListener listener = new JUnitCoreRunListener( new MockReporter(), classMethodCounts );
        listener.testRunStarted( suiteDescription );
        assertEquals( 1, classMethodCounts.size() );
        TestSet testSet = classMethodCounts.get( "cannot.be.loaded.by.junit.Test" );
        assertNotNull( testSet );
    }

    public void testNonEmptyTestRunStarted() throws Exception
    {
        Description aggregator = Description.createSuiteDescription( "null" );
        Description suite = Description.createSuiteDescription( "some.junit.Test" );
        suite.addChild( Description.createSuiteDescription( "testMethodA(some.junit.Test)" ) );
        suite.addChild( Description.createSuiteDescription( "testMethodB(some.junit.Test)" ) );
        suite.addChild( Description.createSuiteDescription( "testMethod(another.junit.Test)" ) );
        aggregator.addChild( suite );
        Map<String, TestSet> classMethodCounts = new HashMap<>();
        JUnitCoreRunListener listener = new JUnitCoreRunListener( new MockReporter(), classMethodCounts );
        listener.testRunStarted( aggregator );
        assertThat( classMethodCounts.keySet(), hasSize( 2 ) );
        assertThat( classMethodCounts.keySet(), containsInAnyOrder( "some.junit.Test", "another.junit.Test" ) );
        TestSet testSet = classMethodCounts.get( "some.junit.Test" );
        MockReporter reporter = new MockReporter();
        testSet.replay( reporter );
        assertTrue( reporter.containsNotification( MockReporter.SET_STARTED ) );
        assertTrue( reporter.containsNotification( MockReporter.SET_COMPLETED ) );
        listener.testRunFinished( null );
        assertThat( classMethodCounts.keySet(), empty() );
    }

    public void testEmptySuiteTestRunStarted() throws Exception
    {
        Description aggregator = Description.createSuiteDescription( "null" );
        Description suite = Description.createSuiteDescription( "some.junit.TestSuite" );
        aggregator.addChild( suite );
        Map<String, TestSet> classMethodCounts = new HashMap<>();
        JUnitCoreRunListener listener = new JUnitCoreRunListener( new MockReporter(), classMethodCounts );
        listener.testRunStarted( aggregator );
        assertThat( classMethodCounts.keySet(), hasSize( 1 ) );
        assertThat( classMethodCounts.keySet(), contains( "some.junit.TestSuite" ) );
        listener.testRunFinished( null );
        assertThat( classMethodCounts.keySet(), empty() );
    }

    public static class STest1
    {
        @Test
        public void testSomething()
        {
        }
    }

    public static class STest2
    {
        @Test
        public void testSomething2()
        {
        }
    }

    public static class TestWithAssumptionFailure
    {
        @Test
        public void testSomething2()
        {
            Assume.assumeTrue( false );
        }
    }

}
