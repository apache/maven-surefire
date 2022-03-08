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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.booter.spi.CommandChannelDecoder;
import org.apache.maven.surefire.booter.spi.EventChannelEncoder;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing singleton {@code MasterProcessReader} in multiple class loaders.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
@RunWith( NewClassLoaderRunner.class )
@SuppressWarnings( "checkstyle:magicnumber" )
public class CommandReaderTest
{
    private static final long DELAY = 200L;
    private static final long TEST_TIMEOUT = 15_000L;

    private final BlockingQueue<Byte> blockingStream = new LinkedBlockingQueue<>();
    private CommandReader reader;

    static class A
    {
    }

    static class B
    {
    }

    static class C
    {
    }

    static class D
    {
    }

    @Before
    public void init()
    {
        //noinspection ResultOfMethodCallIgnored
        Thread.interrupted();
        InputStream realInputStream = new SystemInputStream();
        addTestToPipeline( getClass().getName() );
        ConsoleLogger logger = new NullConsoleLogger();
        ForkNodeArguments args = new ForkedNodeArg( 1, false );
        MasterProcessChannelDecoder decoder =
            new CommandChannelDecoder( newChannel( realInputStream ), args );
        reader = new CommandReader( decoder, Shutdown.DEFAULT, logger );
    }

    @After
    public void deinit()
    {
        reader.stop();
    }

    @Test
    public void readJustOneClass()
    {
        Iterator<String> it = reader.getIterableClasses( new EventChannelEncoder( nul() ) ).iterator();
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
        Iterator<String> it1 = reader.getIterableClasses( new EventChannelEncoder( nul() ) ).iterator();
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
        Iterator<String> it1 = reader.getIterableClasses( new EventChannelEncoder( nul() ) ).iterator();

        assertThat( it1.next(), is( getClass().getName() ) );
        addTestToPipeline( A.class.getName() );
        assertThat( it1.next(), is( A.class.getName() ) );
        addTestToPipeline( B.class.getName() );

        TimeUnit.MILLISECONDS.sleep( DELAY ); // give the test chance to fail

        Iterator<String> it2 = reader.iterated();

        assertThat( it1.next(), is( B.class.getName() ) );
        addTestToPipeline( C.class.getName() );

        assertThat( it2.hasNext(), is( true ) );
        assertThat( it2.next(), is( getClass().getName() ) );
        assertThat( it2.hasNext(), is( true ) );
        assertThat( it2.next(), is( A.class.getName() ) );
        assertThat( it2.hasNext() ).isFalse();

        assertThat( it1.next(), is( C.class.getName() ) );
        addEndOfPipeline();
        assertThat( it1.hasNext() ).isFalse();
    }

    @Test( expected = NoSuchElementException.class )
    public void stopBeforeReadInThread() throws Throwable
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                Iterator<String> it = reader.getIterableClasses( new EventChannelEncoder( nul() ) ).iterator();
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
    public void readTwoClassesInThread() throws Throwable
    {
        final CountDownLatch counter = new CountDownLatch( 1 );
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                Iterator<String> it =
                    reader.getIterableClasses( new EventChannelEncoder( nul() ) ).iterator();
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

    @Test( timeout = TEST_TIMEOUT )
    public void shouldAwaitReaderUp() throws TestSetFailedException
    {
        assertTrue( reader.awaitStarted() );
        reader.stop();
        assertFalse( reader.awaitStarted() );
    }

    private class SystemInputStream extends InputStream
    {
        @Override
        public int read() throws IOException
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
        int clsLength = cls.length();
        String cmd = new StringBuilder( 512 )
            .append( ":maven-surefire-command:" )
            .append( (char) 13 )
            .append( ":run-testclass:" )
            .append( (char) 5 )
            .append( ":UTF-8:" )
            .append( (char) ( clsLength >> 24 ) )
            .append( (char) ( ( clsLength >> 16 ) & 0xff ) )
            .append( (char) ( ( clsLength >> 8 ) & 0xff ) )
            .append( (char) ( clsLength & 0xff ) )
            .append( ":" )
            .append( cls )
            .append( ":" )
            .toString();

        for ( byte cmdByte : cmd.getBytes( US_ASCII ) )
        {
            blockingStream.add( cmdByte );
        }
    }

    private void addEndOfPipeline()
    {
        for ( byte cmdByte : ( ":maven-surefire-command:" + (char) 16 + ":testset-finished:" ).getBytes( US_ASCII ) )
        {
            blockingStream.add( cmdByte );
        }
    }

    private static WritableBufferedByteChannel nul()
    {
        return newBufferedChannel( new PrintStream( new ByteArrayOutputStream() ) );
    }
}
