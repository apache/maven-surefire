package org.apache.maven.surefire.util.internal;

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

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.surefire.util.internal.ConcurrencyUtils.countDownToZero;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Concurrency utilities.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class ConcurrencyUtilsTest
{

    @Test
    public void countDownShouldBeUnchangedAsZero$NegativeTest()
    {
        AtomicInteger atomicCounter = new AtomicInteger( 0 );
        assertFalse( countDownToZero( atomicCounter ) );
        assertThat( atomicCounter.get(), is( 0 ) );
    }

    @Test
    public void countDownShouldBeUnchangedAsNegative$NegativeTest()
    {
        AtomicInteger atomicCounter = new AtomicInteger( -1 );
        assertFalse( countDownToZero( atomicCounter ) );
        assertThat( atomicCounter.get(), is( -1 ) );
    }

    @Test
    public void countDownShouldBeDecreasedByOneThreadModification()
    {
        AtomicInteger atomicCounter = new AtomicInteger( 10 );
        assertFalse( countDownToZero( atomicCounter ) );
        assertThat( atomicCounter.get(), is( 9 ) );
    }

    @Test
    public void countDownToZeroShouldBeDecreasedByOneThreadModification()
    {
        AtomicInteger atomicCounter = new AtomicInteger( 1 );
        assertTrue( countDownToZero( atomicCounter ) );
        assertThat( atomicCounter.get(), is( 0 ) );
    }

    @Test
    public void countDownShouldBeDecreasedByTwoThreadsModification()
        throws ExecutionException, InterruptedException
    {
        final AtomicInteger atomicCounter = new AtomicInteger( 3 );

        FutureTask<Boolean> task = new FutureTask<Boolean>( new Callable<Boolean>()
        {
            public Boolean call()
                throws Exception
            {
                return countDownToZero( atomicCounter );
            }
        } );
        Thread t = new Thread( task );
        t.start();

        assertFalse( countDownToZero( atomicCounter ) );

        assertFalse( task.get() );

        assertThat( atomicCounter.get(), is( 1 ) );

        assertTrue( countDownToZero( atomicCounter ) );

        assertThat( atomicCounter.get(), is( 0 ) );
    }

}
