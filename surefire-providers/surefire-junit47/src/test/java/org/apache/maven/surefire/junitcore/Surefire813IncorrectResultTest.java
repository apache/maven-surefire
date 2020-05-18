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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.Result;

import static junit.framework.Assert.assertEquals;

/**
 * @author Kristian Rosenvold
 * @author nkeyval
 */
public class Surefire813IncorrectResultTest
{

    @Test
    public void dcount()
        throws TestSetFailedException, ExecutionException
    {
        JUnitCoreTester jUnitCoreTester = new JUnitCoreTester();
        final Result run = jUnitCoreTester.run( true, Test6.class );
        assertEquals( 0, run.getFailureCount() );
    }

    /**
     *
     */
    public static class Test6
    {
        private final CountDownLatch latch = new CountDownLatch( 1 );

        @Test
        public void test61()
            throws Exception
        {
            System.out.println( "Hey" );
        }

        @After
        public void tearDown()
            throws Exception
        {
            new MyThread().start();
            latch.await();
        }

        private static final String STR = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        public void synchPrint()
        {
            for ( int i = 0; i < 1000; ++i ) // Increase this number if it does no fail
            {
                System.out.println( i + ":" + STR );
            }
        }

        private class MyThread
            extends Thread
        {
            @Override
            public void run()
            {
                System.out.println( STR );
                latch.countDown();
                synchPrint();
            }
        }
    }

}
