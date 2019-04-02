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
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Testing singleton {@code MasterProcessReader} in multiple class loaders.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
@RunWith( NewClassLoaderRunner.class )
public class CommandReaderTest
{
    private final BlockingQueue<Byte> blockingStream = new LinkedBlockingQueue<>();
    private InputStream realInputStream;
    private CommandReader reader;

    static class A {
    }

    static class B {
    }

    static class C {
    }

    static class D {
    }

    @Before
    public void init()
    {
        Thread.interrupted();
        realInputStream = System.in;
        addTestToPipeline( getClass().getName() );
        System.setIn( new SystemInputStream() );
        reader = CommandReader.getReader();
    }

    @After
    public void deinit()
    {
        reader.stop();
        System.setIn( realInputStream );
    }

    @Test
    public void readJustOneClass()
    {
        Iterator<String> it = reader.getIterableClasses( new ForkedChannelEncoder( nul() ) ).iterator();
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

    @Test
    public void manyClasses()
    {
        Iterator<String> it1 = reader.getIterableClasses( new ForkedChannelEncoder( nul() ) ).iterator();
        assertThat( it1.next(), is( getClass().getName() ) );
        addTestToPipeline( A.class.getName() );
        assertThat( it1.next(), is( A.class.getName() ) );
        addTestToPipeline( B.class.getName() );
        assertThat( it1.next(), is( B.class.getName() ) );
        addTestToPipeline( C.class.getName() );
        assertThat( it1.next(), is( C.class.getName() ) );
        addEndOfPipeline();
        addTestToPipeline( D.class.getName() );
        assertFalse( it1.hasNext() );
    }

    @Test
    public void twoIterators() throws Exception
    {
        Iterator<String> it1 = reader.getIterableClasses( new ForkedChannelEncoder( nul() ) ).iterator();

        assertThat( it1.next(), is( getClass().getName() ) );
        addTestToPipeline( A.class.getName() );
        assertThat( it1.next(), is( A.class.getName() ) );
        addTestToPipeline( B.class.getName() );

        TimeUnit.MILLISECONDS.sleep( 200 ); // give the test chance to fail

        Iterator<String> it2 = reader.iterated();

        assertThat( it1.next(), is( B.class.getName() ) );
        addTestToPipeline( C.class.getName() );

        assertThat( it2.hasNext(), is( true ) );
        assertThat( it2.next(), is( getClass().getName() ) );
        assertThat( it2.hasNext(), is( true ) );
        assertThat( it2.next(), is( A.class.getName() ) );
        assertThat( it2 ).isEmpty();

        assertThat( it1.next(), is( C.class.getName() ) );
        addEndOfPipeline();
        assertThat( it1 ).isEmpty();
    }

    @Test( expected = NoSuchElementException.class )
    public void stopBeforeReadInThread()
        throws Throwable
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                Iterator<String> it = reader.getIterableClasses( new ForkedChannelEncoder( nul() ) ).iterator();
                assertThat( it.next(), is( CommandReaderTest.class.getName() ) );
            }
        };
        FutureTask<Object> futureTask = new FutureTask<>( runnable, null );
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
            @Override
            public void run()
            {
                Iterator<String> it = reader.getIterableClasses( new ForkedChannelEncoder( nul() ) ).iterator();
                assertThat( it.next(), is( CommandReaderTest.class.getName() ) );
                counter.countDown();
                assertThat( it.next(), is( Foo.class.getName() ) );
            }
        };
        FutureTask<Object> futureTask = new FutureTask<>( runnable, null );
        Thread t = new Thread( futureTask );
        t.start();
        counter.await();
        addTestToPipeline( Foo.class.getName() );
        try
        {
            futureTask.get();
        }
        catch ( ExecutionException e )
        {
            throw e.getCause();
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
                return CommandReaderTest.this.blockingStream.take();
            }
            catch ( InterruptedException e )
            {
                throw new IOException( e );
            }
        }
    }

    private void addTestToPipeline( String cls )
    {
        byte[] clazz = cls.getBytes( ISO_8859_1 );
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

    private void addEndOfPipeline()
    {
        ByteBuffer buffer = ByteBuffer.allocate( 8 )
                .putInt( MasterProcessCommand.TEST_SET_FINISHED.getId() )
                .putInt( 0 );
        buffer.rewind();
        for ( ; buffer.hasRemaining(); )
        {
            blockingStream.add( buffer.get() );
        }
    }

    private static PrintStream nul()
    {
        return new PrintStream( new ByteArrayOutputStream() );
    }
}
