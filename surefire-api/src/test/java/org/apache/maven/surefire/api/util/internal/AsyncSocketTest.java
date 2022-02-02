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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.net.StandardSocketOptions.SO_KEEPALIVE;
import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.TCP_NODELAY;
import static org.apache.maven.surefire.api.util.internal.Channels.newInputStream;
import static org.apache.maven.surefire.api.util.internal.Channels.newOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Low level Java benchmark test.
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class AsyncSocketTest
{
    private static final String LONG_STRING =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    private final CountDownLatch barrier = new CountDownLatch( 1 );
    private final AtomicLong writeTime = new AtomicLong();
    private final AtomicLong readTime = new AtomicLong();

    private volatile InetSocketAddress address;

    @Test( timeout = 10_000L )
    public void test() throws Exception
    {
        int forks = 2;
        ThreadFactory factory = DaemonThreadFactory.newDaemonThreadFactory();
        ExecutorService executorService = Executors.newCachedThreadPool( factory );
        if ( executorService instanceof ThreadPoolExecutor )
        {
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
            threadPoolExecutor.setCorePoolSize( Math.min( forks, Runtime.getRuntime().availableProcessors() ) );
            threadPoolExecutor.prestartCoreThread();
        }
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool( executorService );
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open( group );
        setTrueOptions( server, SO_REUSEADDR, TCP_NODELAY, SO_KEEPALIVE );
        InetAddress ip = InetAddress.getLoopbackAddress();
        server.bind( new InetSocketAddress( ip, 0 ), 1 );
        address = (InetSocketAddress) server.getLocalAddress();

        System.gc();
        TimeUnit.SECONDS.sleep( 3L );

        Thread tc = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    client();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };
        tc.setDaemon( true );
        tc.start();

        Future<AsynchronousSocketChannel> acceptFuture = server.accept();
        AsynchronousSocketChannel worker = acceptFuture.get();
        if ( !worker.isOpen() )
        {
            throw new IOException( "client socket closed" );
        }
        final InputStream is = newInputStream( worker );
        final OutputStream os = new BufferedOutputStream( newOutputStream( worker ), 64 * 1024 );

        Thread tt = new Thread()
        {
            public void run()
            {
                try
                {
                    byte[] b = new byte[1024];
                    is.read( b );
                }
                catch ( Exception e )
                {
                    //e.printStackTrace();
                }

            }
        };
        tt.setName( "fork-1-event-thread-" );
        tt.setDaemon( true );
        tt.start();

        Thread t = new Thread()
        {
            @SuppressWarnings( "checkstyle:magicnumber" )
            public void run()
            {
                try
                {
                    byte[] data = LONG_STRING.getBytes( StandardCharsets.US_ASCII );
                    long t1 = System.currentTimeMillis();
                    int i = 0;
                    for ( ; i < 320_000; i++ )
                    {
                        os.write( data );
                        long t2 = System.currentTimeMillis();
                        long spent = t2 - t1;

                        if ( i % 100_000 == 0 )
                        {
                            System.out.println( spent + "ms: " + i );
                        }
                    }
                    os.flush();
                    long spent = System.currentTimeMillis() - t1;
                    writeTime.set( spent );
                    System.out.println( spent + "ms: " + i );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }
        };
        t.setName( "commands-fork-1" );
        t.setDaemon( true );
        t.start();

        barrier.await();
        tt.join();
        t.join();
        tc.join();
        worker.close();
        server.close();

        // 160 millis on write using the asynchronous sockets
        // 320 millis on NIO blocking sockets
        assertThat( writeTime.get() )
            .isLessThan( 1000L );

        // 160 millis on read using the asynchronous sockets
        // 320 millis on NIO blocking sockets
        assertThat( readTime.get() )
            .isLessThan( 1000L );
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private void client() throws Exception
    {
        InetSocketAddress hostAddress = new InetSocketAddress( InetAddress.getLoopbackAddress(), address.getPort() );
        AsynchronousSocketChannel clientSocketChannel = AsynchronousSocketChannel.open();
        clientSocketChannel.connect( hostAddress ).get(); // Wait until connection is done.
        InputStream is = new BufferedInputStream( newInputStream( clientSocketChannel ), 64 * 1024 );
        List<byte[]> bytes = new ArrayList<>();
        long t1 = System.currentTimeMillis();
        for ( int i = 0; i < 320_000; i++ )
        {
            byte[] b = new byte[100];
            is.read( b );
            bytes.add( b );
        }
        long t2 = System.currentTimeMillis();
        long spent = t2 - t1;
        readTime.set( spent );
        System.out.println( new String( bytes.get( bytes.size() - 1 ) ) );
        System.out.println( "received within " + spent + "ms" );
        clientSocketChannel.close();
        barrier.countDown();
    }

    @SafeVarargs
    private static void setTrueOptions( AsynchronousServerSocketChannel server, SocketOption<Boolean>... options )
        throws IOException
    {
        for ( SocketOption<Boolean> option : options )
        {
            if ( server.supportedOptions().contains( option ) )
            {
                server.setOption( option, true );
            }
        }
    }
}
