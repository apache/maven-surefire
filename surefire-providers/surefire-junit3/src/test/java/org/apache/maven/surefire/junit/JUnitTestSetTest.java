package org.apache.maven.surefire.junit;

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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.maven.surefire.common.junit3.JUnit3Reflector;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.TestSetReportEntry;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.ArrayList;
import java.util.List;

public class JUnitTestSetTest
    extends TestCase
{

    public void testExecuteSuiteClass()
        throws TestSetFailedException
    {
        ClassLoader testClassLoader = this.getClass().getClassLoader();
        JUnit3Reflector reflector = new JUnit3Reflector( testClassLoader );
        JUnitTestSet testSet = new JUnitTestSet( Suite.class, reflector );
        SuccessListener listener = new SuccessListener();
        testSet.execute( listener, testClassLoader );
        List succeededTests = listener.getSucceededTests();
        assertEquals( 1, succeededTests.size() );
        assertEquals( "testSuccess(org.apache.maven.surefire.junit.JUnitTestSetTest$AlwaysSucceeds)",
                      ( (ReportEntry) succeededTests.get( 0 ) ).getName() );
    }

    public static final class AlwaysSucceeds
        extends TestCase
    {
        public void testSuccess()
        {
            assertTrue( true );
        }
    }

    public static class SuccessListener
        implements RunListener
    {

        private List<ReportEntry> succeededTests = new ArrayList<ReportEntry>();

        @Override
        public void testSetStarting( TestSetReportEntry report )
        {
        }

        @Override
        public void testSetCompleted( TestSetReportEntry report )
        {
        }

        @Override
        public void testStarting( ReportEntry report )
        {
        }

        @Override
        public void testSucceeded( ReportEntry report )
        {
            this.succeededTests.add( report );
        }

        @Override
        public void testAssumptionFailure( ReportEntry report )
        {
            throw new IllegalStateException();
        }

        @Override
        public void testError( ReportEntry report )
        {
            throw new IllegalStateException();
        }

        @Override
        public void testFailed( ReportEntry report )
        {
            throw new IllegalStateException();
        }

        @Override
        public void testSkipped( ReportEntry report )
        {
            throw new IllegalStateException();
        }

        @Override
        public void testExecutionSkippedByUser()
        {
        }

        public void testSkippedByUser( ReportEntry report )
        {
            testSkipped( report );
        }

        public List getSucceededTests()
        {
            return succeededTests;
        }

    }

    public static class Suite
    {

        public static Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTestSuite( AlwaysSucceeds.class );
            return suite;
        }
    }
}
