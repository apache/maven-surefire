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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

/*
 * @author Kristian Rosenvold
 */

public class DemultiplexingRunListenerTest
{
    @Test
    public void testTestStarted()
        throws Exception
    {
        RunListener real = mock( RunListener.class );
        DemultiplexingRunListener listener = new DemultiplexingRunListener( real );

        Description testRunDescription = Description.createSuiteDescription( DemultiplexingRunListenerTest.class );
        Description description1 =
            Description.createTestDescription( DemultiplexingRunListenerTest.class, "testStub1" );
        Description description2 = Description.createTestDescription( Dummy.class, "testStub2" );
        testRunDescription.addChild( description1 );
        testRunDescription.addChild( description2 );

        listener.testRunStarted( testRunDescription );
        listener.testStarted( description1 );
        listener.testStarted( description2 );
        listener.testFinished( description1 );
        listener.testFinished( description2 );
        Result temp = new Result();
        listener.testRunFinished( temp );

        verify( real ).testRunStarted( any( Description.class ) );
        verify( real ).testStarted( description1 );
        verify( real ).testFinished( description1 );
        verify( real ).testStarted( description2 );
        verify( real ).testFinished( description2 );
        verify( real ).testRunFinished( any( Result.class ) );
    }

    @Test
    public void testJunitResultCountingReferenceValue()
        throws Exception
    {
        Result result = new Result();
        runACoupleOfClasses( result );

        assertEquals( 5, result.getRunCount() );
        assertEquals( 1, result.getIgnoreCount() );
        assertEquals( 1, result.getFailureCount() );
    }

    @Test
    public void testJunitResultCountingDemultiplexed()
        throws Exception
    {
        Result result = new Result();
        runACoupleOfClasses( result );

        assertEquals( 5, result.getRunCount() );
        assertEquals( 1, result.getIgnoreCount() );
        assertEquals( 1, result.getFailureCount() );
    }

    private void runACoupleOfClasses( Result result )
    {
        DemultiplexingRunListener demultiplexingRunListener = new DemultiplexingRunListener( result.createListener() );

        JUnitCore jUnitCore = new JUnitCore();

        jUnitCore.addListener( demultiplexingRunListener );
        Computer computer = new Computer();

        jUnitCore.run( computer, new Class[]{ Dummy.class, Dummy2.class } );
    }

    public static class Dummy
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

    public static class Dummy2
    {

        @Test
        public void testNotMuchA()
        {

        }

        @Test
        public void testStub1A()
        {
            Assert.fail( "We will fail" );
        }

        @Test
        public void testStub2A()
        {
        }
    }

}
