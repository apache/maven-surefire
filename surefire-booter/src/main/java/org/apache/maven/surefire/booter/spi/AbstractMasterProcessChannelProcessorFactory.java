package org.apache.maven.surefire.booter.spi;

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

import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.apache.maven.surefire.spi.MasterProcessChannelProcessorFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.security.PrivilegedAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static java.security.AccessController.doPrivileged;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.maven.surefire.api.util.internal.DaemonThreadFactory.newDaemonThreadFactory;

/**
 * Default implementation of {@link MasterProcessChannelProcessorFactory}.
 */
public abstract class AbstractMasterProcessChannelProcessorFactory
    implements MasterProcessChannelProcessorFactory
{
    private static final String STREAM_FLUSHER = "surefire-forkedjvm-stream-flusher";
    private final ScheduledExecutorService flusher;

    public AbstractMasterProcessChannelProcessorFactory()
    {
        flusher = newScheduledThreadPool( 1, newDaemonThreadFactory( STREAM_FLUSHER ) );
    }

    protected void schedulePeriodicFlusher( int delayInMillis, final WritableBufferedByteChannel channel  )
    {
        final AtomicLong bufferOverflows = new AtomicLong();
        flusher.scheduleWithFixedDelay( new Runnable()
        {
            @Override
            public void run()
            {
                long currentBufferOverflows = channel.countBufferOverflows();
                // optimization: flush the Channel only if the buffer has not overflew after last period of time
                if ( bufferOverflows.get() == currentBufferOverflows )
                {
                    try
                    {
                        channel.write( ByteBuffer.allocate( 0 ) );
                    }
                    catch ( Exception e )
                    {
                        // cannot do anything about this I/O issue
                    }
                }
                else
                {
                    bufferOverflows.set( currentBufferOverflows );
                }
            }
        }, 0L, delayInMillis, MILLISECONDS );
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            doPrivileged( new PrivilegedAction<Object>()
                          {
                              @Override
                              public Object run()
                              {
                                  flusher.shutdown();
                                  // Do NOT call awaitTermination() due to this would unnecessarily prolong teardown
                                  // time of the JVM. It is not a critical situation when the JXM exits this daemon
                                  // thread because the interrupted flusher does not break any business function.
                                  // All business data is already safely flushed by test events, then by sending BYE
                                  // event at the exit time and finally by flushEventChannelOnExit() in ForkedBooter.
                                  return null;
                              }
                          }
            );
        }
        catch ( AccessControlException e )
        {
            // ignore
        }
    }
}
