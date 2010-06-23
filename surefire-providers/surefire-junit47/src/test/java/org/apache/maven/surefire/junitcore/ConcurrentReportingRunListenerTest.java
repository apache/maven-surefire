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


import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 * @author Kristian Rosenvold
 */

public class ConcurrentReportingRunListenerTest
{
    // Tests are in order of increasing complexity
    @Test
    public void testNoErrorsCounting() throws Exception
    {
        runClasses(  3, 0 ,0, DummyAllOk.class );
    }

    @Test
    public void testNoErrorsCounting2() throws Exception
    {
        runClasses( 2, 0 ,0 , Dummy3.class );
    }

    @Test
    public void testOneIgnoreCounting() throws Exception
    {
        runClasses( 3, 1, 0, DummyWithOneIgnore.class  );
    }

    @Test
    public void testOneFailureCounting() throws Exception
    {
        runClasses( 3, 0 ,1,  DummyWithFailure.class  );
    }

    @Test
    public void testWithErrorsCountingDemultiplexed()
        throws Exception
    {
        runClasses( 6, 1, 1 , DummyWithOneIgnore.class, DummyWithFailure.class);
    }


    @Test
    public void testJunitResultCountingDemultiplexed()
        throws Exception
    {
        runClasses( 8, 1, 1, DummyWithOneIgnore.class, DummyWithFailure.class, Dummy3.class   );
    }

    @Test
    public void testJunitResultCountingJUnit3Demultiplexed()
        throws Exception
    {
        runClasses( 3, 0 ,0, Junit3Tc1.class, Junit3Tc2.class  );
    }

    @Test
    public void testJunitResultCountingJUnit3OddTest()
        throws Exception
    {
        runClasses( 2, 0 ,0, Junit3OddTest1.class );
    }

    @Test
    public void testJunit3WithNestedSuite()
        throws TestSetFailedException
    {
        runClasses(  4, 0 ,0, Junit3WithNestedSuite.class );
    }

    @Test
    public void testJunit3NestedSuite()
        throws Exception
    {
        runClasses( 2, 0 ,0, Junit3OddTest1.class );
    }


    @Test
    public void testSimpleOutput()
        throws Exception
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream collector = new PrintStream( byteArrayOutputStream );
        PrintStream orgOur = System.out;
        System.setOut( collector );

        RunStatistics result = runClasses(Dummy3.class);
        assertReporter( result,  2, 0 ,0, "msgs" );


        String foo = new String( byteArrayOutputStream.toByteArray() );
        assertNotNull( foo );

        System.setOut( orgOur );
    }

    @Test
    public void testOutputOrdering()
        throws Exception
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream collector = new PrintStream( byteArrayOutputStream );
        PrintStream orgOur = System.out;
        System.setOut( collector );

        RunStatistics result = runClasses(DummyWithOneIgnore.class, DummyWithFailure.class, Dummy3.class);
        assertReporter( result,  8, 1 ,1, "msgs" );


        String foo = new String( byteArrayOutputStream.toByteArray() );
        assertNotNull( foo );

        System.setOut( orgOur );

//        final List<String> stringList = result.getEvents();
//        assertEquals( 23, stringList.size() );
    }

    private void runClasses( int success, int ignored, int failure, Class... classes )
        throws TestSetFailedException
    {
        ReporterManagerFactory reporterManagerFactory = createReporterFactory();
        RunStatistics result = runClasses(reporterManagerFactory, new ConcurrentReportingRunListener.ClassesParallelRunListener( reporterManagerFactory ),  classes);
        assertReporter(  result, success, ignored ,failure, "classes" );

        reporterManagerFactory = createReporterFactory();
        result = runClasses(reporterManagerFactory, new ConcurrentReportingRunListener.MethodsParallelRunListener(reporterManagerFactory, true) , classes);
        assertReporter(  result, success, ignored ,failure, "methods" );

        reporterManagerFactory = createReporterFactory();
        result = runClasses(reporterManagerFactory, new ConcurrentReportingRunListener.MethodsParallelRunListener(reporterManagerFactory, false) , classes);
        assertReporter(  result, success, ignored ,failure, "methods" );

    }

    private RunStatistics runClasses( Class... classes )
        throws TestSetFailedException
    {
        final ReporterManagerFactory reporterManagerFactory = createReporterFactory();
        ConcurrentReportingRunListener demultiplexingRunListener = createRunListener( reporterManagerFactory );

        JUnitCore jUnitCore = new JUnitCore();

        jUnitCore.addListener( demultiplexingRunListener );
        Computer computer = new Computer();

        jUnitCore.run( computer, classes );
        return reporterManagerFactory.getGlobalRunStatistics();
    }

    private RunStatistics runClasses( ReporterManagerFactory reporterManagerFactory, ConcurrentReportingRunListener demultiplexingRunListener, Class... classes )
        throws TestSetFailedException
    {

        JUnitCore jUnitCore = new JUnitCore();

        jUnitCore.addListener( demultiplexingRunListener );
        Computer computer = new Computer();

        jUnitCore.run( computer, classes );
        return reporterManagerFactory.getGlobalRunStatistics();
    }

    private ConcurrentReportingRunListener createRunListener( ReporterManagerFactory reporterFactory )
        throws TestSetFailedException
    {
        return new ConcurrentReportingRunListener.ClassesParallelRunListener( reporterFactory );
    }


    public static class DummyWithOneIgnore
    {
        @Test
        public void testNotMuch()
        {

        }

        @Ignore
        @Test
        public void testStub1()
        {
        }

        @Test
        public void testStub2()
        {
        }
    }

    public static class DummyWithFailure
    {

        @Test
        public void testBeforeFail()
        {

        }

        @Test
        public void testWillFail()
        {
            Assert.fail( "We will fail" );
        }

        @Test
        public void testAfterFail()
        {
        }
    }

    public static class DummyAllOk
    {

        @Test
        public void testNotMuchA()
        {

        }

        @Test
        public void testStub1A()
        {
        }

        @Test
        public void testStub2A()
        {
        }
    }

    public static class Dummy3
    {

        @Test
        public void testNotMuchA()
        {
            System.out.println( "tNMA1" );
            System.err.println( "tNMA1err" );
        }

        @Test
        public void testStub2A()
        {
            System.out.println( "tS2A" );
            System.err.println( "tS2AErr" );
        }
    }

    public static class Junit3Tc1
        extends TestCase
    {

        public Junit3Tc1()
        {
            super( "testNotMuchJunit3TC1" );
        }

        public void testNotMuchJunit3TC1()
        {
            System.out.println( "Junit3TC1" );
        }


        public static junit.framework.Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest( new Junit3Tc1() );
            return suite;
        }
    }

    public static class Junit3Tc2
        extends TestCase
    {
        public Junit3Tc2( String testMethod )
        {
            super( testMethod );
        }

        public void testNotMuchJunit3TC2()
        {
            System.out.println( "Junit3TC2" );
        }

        public void testStubJ3TC2A()
        {
            System.out.println( "testStubJ3TC2A" );
        }


        public static junit.framework.Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest( new Junit3Tc2( "testNotMuchJunit3TC2" ) );
            suite.addTest( new Junit3Tc2( "testStubJ3TC2A" ) );
            return suite;
        }
    }

    public static class Junit3OddTest1
        extends TestCase
    {


        public static junit.framework.Test suite()
        {
            TestSuite suite = new TestSuite();

            suite.addTest( new Junit3OddTest1( "testMe" ) );
            suite.addTest( new Junit3OddTest1( "testMe" ) );



            return suite;
        }


        public Junit3OddTest1( String name )
        {
            super( name );
        }

        public void testMe()
        {
            assertTrue( true );
        }
    }

    public static class Junit3WithNestedSuite
        extends TestCase
    {


        public static junit.framework.Test suite()
        {
            TestSuite suite = new TestSuite();

            suite.addTest( new Junit3WithNestedSuite( "testMe2" ) );
            suite.addTest( new Junit3WithNestedSuite( "testMe2" ) );
            suite.addTestSuite(   Junit3Tc2.class);
            return suite;
        }


        public Junit3WithNestedSuite( String name )
        {
            super( name );
        }

        public void testMe2()
        {
            assertTrue( true );
        }
    }


    private ReporterManagerFactory createReporterFactory()
    {
        Object[] reporter = new Object[]{MockReporter.class.getCanonicalName(), new Object[] {} };
        final List<Object> objects = new ArrayList();
        objects.add( reporter );
        return new ReporterManagerFactory(objects, this.getClass().getClassLoader());
    }

    private void assertReporter( RunStatistics result, int success, int ignored, int failure, String message )
    {
        assertEquals( message,  success, result.getCompletedCount() );
        assertEquals( message,  failure, result.getFailureSources().size() );
        assertEquals( message,  ignored, result.getSkipped() );
    }

}
