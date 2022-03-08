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
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.common.junit3.JUnit3Reflector;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_17;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_RECENT;

/**
 *
 */
public class JUnitTestSetTest
    extends TestCase
{

    public void testExecuteSuiteClass()
        throws Exception
    {
        ClassLoader testClassLoader = this.getClass().getClassLoader();
        JUnit3Reflector reflector = new JUnit3Reflector( testClassLoader );
        SuccessListener listener = new SuccessListener();
        JUnit3Reporter reporter = new JUnit3Reporter( listener );
        JUnitTestSetExecutor testSet = new JUnitTestSetExecutor( reflector, reporter );
        testSet.execute( Suite.class, testClassLoader );
        List<ReportEntry> succeededTests = listener.getSucceededTests();
        assertEquals( 1, succeededTests.size() );
        assertEquals( "org.apache.maven.surefire.junit.JUnitTestSetTest$AlwaysSucceeds",
                succeededTests.get( 0 ).getSourceName() );
        assertEquals( "testSuccess",
                      succeededTests.get( 0 ).getName() );
    }

    public void testSystemManager()
    {
        boolean isDeprecated = !JAVA_RECENT.atMost( JAVA_17 );
        Object originalSm = null;
        try
        {
            if ( !isDeprecated )
            {
                originalSm = System.getSecurityManager();
            }

            JUnit3Provider.setSystemManager( "java.lang.SecurityManager" );

            if ( isDeprecated )
            {
                fail();
            }

            Object sm = System.getSecurityManager();
            assertNotNull( sm );
            assertEquals( "java.lang.SecurityManager", sm.getClass().getName() );
            assertNotSame( originalSm, sm );
        }
        catch ( TestSetFailedException e )
        {
            if ( !isDeprecated )
            {
                fail();
            }
        }
        finally
        {
            if ( !isDeprecated )
            {
                try
                {
                    SecurityManager sm = (SecurityManager) originalSm;
                    AccessController.doPrivileged( (PrivilegedAction<Object>) () ->
                    {
                        System.setSecurityManager( sm );
                        return null;
                    } );
                }
                catch ( AccessControlException e )
                {
                    // ignore
                }
            }
        }
    }

    /**
     *
     */
    public static final class AlwaysSucceeds
        extends TestCase
    {
        public void testSuccess()
        {
            assertTrue( true );
        }
    }

    /**
     *
     */
    public static class SuccessListener
        implements RunListener, TestReportListener<TestOutputReportEntry>
    {

        private List<ReportEntry> succeededTests = new ArrayList<>();

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
            succeededTests.add( report );
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

        List<ReportEntry> getSucceededTests()
        {
            return succeededTests;
        }

        @Override
        public void writeTestOutput( TestOutputReportEntry reportEntry )
        {

        }

        @Override
        public boolean isDebugEnabled()
        {
            return false;
        }

        @Override
        public void debug( String message )
        {

        }

        @Override
        public boolean isInfoEnabled()
        {
            return false;
        }

        @Override
        public void info( String message )
        {

        }

        @Override
        public boolean isWarnEnabled()
        {
            return false;
        }

        @Override
        public void warning( String message )
        {

        }

        @Override
        public boolean isErrorEnabled()
        {
            return false;
        }

        @Override
        public void error( String message )
        {

        }

        @Override
        public void error( String message, Throwable t )
        {

        }

        @Override
        public void error( Throwable t )
        {

        }
    }

    /**
     *
     */
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
