package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.apache.maven.surefire.util.internal.StringUtils.FORK_STREAM_CHARSET_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Testing singleton {@code MasterProcessReader} in multiple class loaders.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
@RunWith( NewClassLoaderRunner.class )
public class MasterProcessReaderTest
{
    private final BlockingQueue<Byte> blockingStream = new LinkedBlockingQueue<Byte>();
    private InputStream realInputStream;
    private MasterProcessReader reader;

    @Before
    public void init()
        throws UnsupportedEncodingException
    {
        Thread.interrupted();
        realInputStream = System.in;
        addThisTestToPipeline( getClass().getName() );
        System.setIn( new SystemInputStream() );
        reader = MasterProcessReader.getReader();
    }

    @After
    public void deinit()
    {
        reader.stop();
        System.setIn( realInputStream );
    }

    @Test
    public void readJustOneClass() throws Exception
    {
        Iterator<String> it = reader.getIterableClasses( new PrintStream( new ByteArrayOutputStream() ) )
            .iterator();
        assertTrue( it.hasNext() );
        assertThat( it.next(), is( getClass().getName() ) );
        reader.stop();
        assertFalse( it.hasNext() );
        try
        {
            it.next();
            fail();
        }
        catch ( NoSuchElementException e )
        {
            // expected
        }
    }

    @Test( expected = NoSuchElementException.class )
    public void stopBeforeReadInThread()
        throws Throwable
    {
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                Iterator<String> it = reader.getIterableClasses( new PrintStream( new ByteArrayOutputStream() ) )
                    .iterator();
                assertThat( it.next(), is( MasterProcessReaderTest.class.getName() ) );
            }
        };
        FutureTask<Object> futureTask = new FutureTask<Object>( runnable, null );
        Thread t = new Thread( futureTask );
        reader.stop();
        t.start();
        try
        {
            futureTask.get();
        }
        catch ( ExecutionException e )
        {
            throw e.getCause();
        }
    }

    @Test
    public void readTwoClassesInThread()
        throws Throwable
    {
        final CountDownLatch counter = new CountDownLatch( 1 );
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                Iterator<String> it = reader.getIterableClasses( new PrintStream( new ByteArrayOutputStream() ) )
                    .iterator();
                assertThat( it.next(), is( MasterProcessReaderTest.class.getName() ) );
                counter.countDown();
                assertThat( it.next(), is( PropertiesWrapperTest.class.getName() ) );
            }
        };
        FutureTask<Object> futureTask = new FutureTask<Object>( runnable, null );
        Thread t = new Thread( futureTask );
        t.start();
        counter.await();
        addThisTestToPipeline( PropertiesWrapperTest.class.getName() );
        try
        {
            futureTask.get();
        }
        catch ( ExecutionException e )
        {
            throw e.getCause();
        }
    }

    // @Test
    public void readTwoClassesInTwoThreads()
        throws Throwable
    {
        final Iterable<String> lazyTestSet =
            reader.getIterableClasses( new PrintStream( new ByteArrayOutputStream() ) );

        class Provider implements Runnable
        {
            public void run()
            {
                Iterator<String> it = lazyTestSet.iterator();
                assertThat( it.next(), is( MasterProcessReaderTest.class.getName() ) );
                assertThat( it.next(), is( PropertiesWrapperTest.class.getName() ) );
                assertFalse( it.hasNext() );
            }
        }

        ExecutorService es = Executors.newFixedThreadPool( 2 );
        Future<?> result1 = es.submit( new Provider() );
        Random rnd = new Random();
        TimeUnit.MILLISECONDS.sleep( rnd.nextInt( 50 ) );
        Future<?> result2 = es.submit( new Provider() );
        TimeUnit.MILLISECONDS.sleep( rnd.nextInt( 50 ) );
        addThisTestToPipeline( PropertiesWrapperTest.class.getName() );
        TimeUnit.MILLISECONDS.sleep( rnd.nextInt( 50 ) );
        addTestSetFinishedToPipeline();
        for ( Future<?> result : Arrays.asList( result1, result2 ) )
        {
            try
            {
                result.get();
            }
            catch ( ExecutionException e )
            {
                throw e.getCause();
            }
        }
    }

    @Test( timeout = 15000 )
    public void shouldAwaitReaderUp()
        throws TestSetFailedException
    {
        assertTrue( reader.awaitStarted() );
        reader.stop();
        assertFalse( reader.awaitStarted() );
    }

    private class SystemInputStream
        extends InputStream
    {
        @Override
        public int read()
            throws IOException
        {
            try
            {
                return MasterProcessReaderTest.this.blockingStream.take();
            }
            catch ( InterruptedException e )
            {
                throw new IOException( e );
            }
        }
    }

    private void addThisTestToPipeline( String cls )
        throws UnsupportedEncodingException
    {
        byte[] clazz = cls.getBytes( FORK_STREAM_CHARSET_NAME );
        ByteBuffer buffer = ByteBuffer.allocate( 8 + clazz.length )
            .putInt( MasterProcessCommand.RUN_CLASS.getId() )
            .putInt( clazz.length )
            .put( clazz );
        buffer.rewind();
        for ( ; buffer.hasRemaining(); )
        {
            blockingStream.add( buffer.get() );
        }
    }

    private void addTestSetFinishedToPipeline()
        throws UnsupportedEncodingException
    {
        for ( byte b : MasterProcessCommand.TEST_SET_FINISHED.encode() )
        {
            blockingStream.add( b );
        }
    }
}
