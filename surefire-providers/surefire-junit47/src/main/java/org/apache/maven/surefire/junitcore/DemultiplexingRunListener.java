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

package org.apache.maven.surefire.junitcore;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demultiplexes threaded running of tests into something that does not look threaded.
 * Essentially makes a threaded junit core RunListener behave like something like a
 * junit4 reporter can handle.
 * <p/>
 * This version is non-ketchup mode, outputting test results as the individual suites complete.
 *
 * @author Kristian Rosenvold
 */
public class DemultiplexingRunListener
    extends RunListener
{
    private final RunListener realtarget;

    private volatile Map<String, TestMethod> testMethods;

    public DemultiplexingRunListener( RunListener realtarget )
    {
        this.realtarget = realtarget;
    }

    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
        testMethods = createTestMethodMap( description );
    }

    @Override
    public void testFinished( Description description )
        throws Exception
    {
        final TestMethod testDescription = getTestDescription( description );
        testDescription.testFinished( description );
        testDescription.getParent().setDone( realtarget );
    }

    @Override
    public void testIgnored( Description description )
        throws Exception
    {
        final TestMethod testDescription = getTestDescription( description );
        testDescription.testIgnored( description );
        testDescription.getParent().setDone( realtarget );
    }

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        getTestDescription( failure.getDescription() ).testFailure( failure );
    }

    @Override
    public void testAssumptionFailure( Failure failure )
    {
        final TestMethod testDescription = getTestDescription( failure.getDescription() );
        testDescription.testAssumptionFailure( failure );
    }


    private TestMethod getTestDescription( Description description )
    {
        final TestMethod result = testMethods.get( description.getDisplayName() );
        if ( result == null )
        {
            throw new IllegalStateException( "No TestDescription found for " + description +
                ", inconsistent junit behaviour. Unknown junit version?" );
        }
        return result;
    }


    static Map<String, TestMethod> createTestMethodMap( Description description )
    {
        Map<String, TestMethod> result = new HashMap<String, TestMethod>();
        createTestDescription( description, result );
        return result;
    }

    private static void createTestDescription( Description description, Map<String, TestMethod> result )
    {
        final ArrayList<Description> children = description.getChildren();

        TestDescription current = new TestDescription( description );

        for ( Description item : children )
        {
            if ( item.isTest() )
            {
                TestMethod testMethod = new TestMethod( item, current );
                if ( item.getDisplayName() != null )
                {
                    result.put( item.getDisplayName(), testMethod );
                }
                current.addTestMethod( testMethod );
            }
            else
            {
                createTestDescription( item, result );
            }
        }
    }

    public static class TestDescription
    {
        private final Result resultForThisClass = new Result();

        private final Description testRunStarted;


        private AtomicInteger numberOfCompletedChildren = new AtomicInteger( 0 );

        private final List<TestMethod> testMethods = Collections.synchronizedList( new ArrayList<TestMethod>() );

        public TestDescription( Description description )
        {
            testRunStarted = description;
        }

        private void addTestMethod( TestMethod testMethod )
        {
            testMethods.add( testMethod );
        }

        private boolean incrementCompletedChildrenCount()
        {
            return testMethods.size() == numberOfCompletedChildren.incrementAndGet();
        }

        boolean setDone( RunListener target )
        {
            final boolean result = incrementCompletedChildrenCount();
            if ( result )
            {
                notifyListener( target );
                notifyListener( resultForThisClass.createListener() );
            }
            return result;
        }

        @SuppressWarnings( { "SynchronizationOnLocalVariableOrMethodParameter" } )
        private void notifyListener( final RunListener target )
        {
            try
            {
                synchronized ( target )
                {
                    target.testRunStarted( testRunStarted );
                    for ( TestMethod testMethod : testMethods )
                    {
                        testMethod.replay( target );
                    }
                    target.testRunFinished( resultForThisClass );
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    static class TestMethod
    {
        private final Description description;

        private final TestDescription parent;

        private volatile Failure testFailure;

        private volatile Failure testAssumptionFailure;

        private volatile Description finished;

        private volatile Description ignored;


        public TestMethod( Description description, TestDescription current )
        {
            this.description = description;
            this.parent = current;
        }


        public void testFinished( Description description )
            throws Exception
        {
            this.finished = description;
        }


        public void testIgnored( Description description )
            throws Exception
        {
            ignored = description;
        }

        public void testFailure( Failure failure )
            throws Exception
        {
            this.testFailure = failure;
        }

        public void testAssumptionFailure( Failure failure )
        {
            this.testAssumptionFailure = failure;
        }

        public void replay( RunListener runListener )
            throws Exception
        {
            if ( ignored != null )
            {
                runListener.testIgnored( ignored );
            }
            else
            {
                runListener.testStarted( description );
                if ( testFailure != null )
                {
                    runListener.testFailure( testFailure );
                }
                if ( testAssumptionFailure != null )
                {
                    runListener.testAssumptionFailure( testAssumptionFailure );
                }
                runListener.testFinished( finished );
            }
        }

        public TestDescription getParent()
        {
            return parent;
        }
    }

}
