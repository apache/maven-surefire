package org.apache.maven.surefire.common.junit4;

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

import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import junit.framework.Assert;
import org.apache.maven.surefire.api.util.internal.DaemonThreadFactory;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4RunListenerTest
    extends TestCase
{
    public void testTestStarted()
    {
        RunListener jUnit4TestSetReporter = new JUnit4RunListener( new MockReporter() );
        Runner junitTestRunner = Request.classes( "abc", STest1.class, STest2.class ).getRunner();
        RunNotifier runNotifier = new RunNotifier();
        runNotifier.addListener( jUnit4TestSetReporter );
        junitTestRunner.run( runNotifier );
    }

    public void testParallelInvocations()
        throws Exception
    {
        final MockReporter reporter = new MockReporter();
        final RunListener jUnit4TestSetReporter = new JUnit4RunListener( reporter );
        final CountDownLatch countDownLatch = new CountDownLatch( 1 );
        final Description testSomething = Description.createTestDescription( STest1.class, "testSomething" );
        final Description testSomething2 = Description.createTestDescription( STest2.class, "testSomething2" );

        jUnit4TestSetReporter.testStarted( testSomething );

        DaemonThreadFactory.newDaemonThread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    jUnit4TestSetReporter.testStarted( testSomething2 );
                    jUnit4TestSetReporter.testFailure( new Failure( testSomething2, new AssertionError( "Fud" ) ) );
                    jUnit4TestSetReporter.testFinished( testSomething2 );
                    countDownLatch.countDown();
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        } ).start();

        countDownLatch.await();
        jUnit4TestSetReporter.testFinished( testSomething );

        Assert.assertEquals( "Failing tests", 1, reporter.getTestFailed() );
        Assert.assertEquals( "Succeeded tests", 1, reporter.getTestSucceeded() );
    }

    /**
     *
     */
    public static class STest1
    {
        @Test
        public void testSomething()
        {

        }
    }

    /**
     *
     */
    public static class STest2
    {
        @Test
        public void testSomething2()
        {

        }
    }
}
