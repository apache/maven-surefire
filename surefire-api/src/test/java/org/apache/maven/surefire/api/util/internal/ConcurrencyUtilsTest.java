package org.apache.maven.surefire.api.util.internal;

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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.surefire.api.util.internal.ConcurrencyUtils.runIfZeroCountDown;
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
    public void countDownShouldBeUnchangedAsZeroNegativeTest()
    {
        AtomicInteger atomicCounter = new AtomicInteger( 0 );
        AtomicBoolean runner = new AtomicBoolean();
        runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
        assertFalse( runner.get() );
        assertThat( atomicCounter.get(), is( 0 ) );
    }

    @Test
    public void countDownShouldBeUnchangedAsNegativeNegativeTest()
    {
        AtomicInteger atomicCounter = new AtomicInteger( -1 );
        AtomicBoolean runner = new AtomicBoolean();
        runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
        assertFalse( runner.get() );
        assertThat( atomicCounter.get(), is( -1 ) );
    }

    @Test
    public void countDownShouldBeDecreasedByOneThreadModification()
    {
        AtomicInteger atomicCounter = new AtomicInteger( 10 );
        AtomicBoolean runner = new AtomicBoolean();
        runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
        assertFalse( runner.get() );
        assertThat( atomicCounter.get(), is( 9 ) );
    }

    @Test
    public void countDownToZeroShouldBeDecreasedByOneThreadModification()
    {
        AtomicInteger atomicCounter = new AtomicInteger( 1 );
        AtomicBoolean runner = new AtomicBoolean();
        runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
        assertTrue( runner.get() );
        assertThat( atomicCounter.get(), is( 0 ) );
    }

    @Test
    public void countDownShouldBeDecreasedByTwoThreadsModification()
        throws ExecutionException, InterruptedException
    {
        AtomicInteger atomicCounter = new AtomicInteger( 3 );

        FutureTask<Boolean> task = new FutureTask<>( () ->
        {
            AtomicBoolean runner = new AtomicBoolean();
            runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
            return runner.get();
        } );
        Thread t = new Thread( task );
        t.start();

        AtomicBoolean runner = new AtomicBoolean();
        runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
        assertFalse( runner.get() );

        assertFalse( task.get() );

        assertThat( atomicCounter.get(), is( 1 ) );

        runner.set( false );
        runIfZeroCountDown( () -> runner.set( true ), atomicCounter );
        assertTrue( runner.get() );

        assertThat( atomicCounter.get(), is( 0 ) );
    }

}
