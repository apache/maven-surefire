package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.booter.Shutdown;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.surefire.booter.Command.BYE_ACK;
import static org.apache.maven.surefire.booter.Command.NOOP;
import static org.apache.maven.surefire.booter.Command.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.booter.Command.toRunClass;
import static org.apache.maven.surefire.booter.Command.toShutdown;

/**
 * An {@link java.io.InputStream} that, when read, provides test class names out of a queue.
 * <br>
 * The Stream provides only one test at a time, but only after {@link #provideNewTest()} has been invoked.
 * <br>
 * After providing each test class name, followed by a newline character, a flush is performed on the
 * {@link FlushReceiver} provided by the {@link FlushReceiverProvider} that can be set using
 * {@link #setFlushReceiverProvider(FlushReceiverProvider)}.
 * <br>
 * The instance is used only in reusable forks in {@link org.apache.maven.plugin.surefire.booterclient.ForkStarter}
 * by one Thread.
 *
 * @author Andreas Gudian
 * @author Tibor Digana (tibor17)
 */
public final class TestProvidingInputStream
    extends AbstractCommandStream
{
    private final Semaphore barrier = new Semaphore( 0 );

    private final Queue<Command> commands = new ConcurrentLinkedQueue<Command>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Queue<String> testClassNames;

    /**
     * C'tor
     *
     * @param testClassNames source of the tests to be read from this stream
     */
    public TestProvidingInputStream( Queue<String> testClassNames )
    {
        this.testClassNames = testClassNames;
    }

    /**
     * For testing purposes.
     */
    void testSetFinished()
    {
        if ( canContinue() )
        {
            commands.add( Command.TEST_SET_FINISHED );
            barrier.release();
        }
    }

    @Override
    public void skipSinceNextTest()
    {
        if ( canContinue() )
        {
            commands.add( SKIP_SINCE_NEXT_TEST );
            barrier.release();
        }
    }

    @Override
    public void shutdown( Shutdown shutdownType )
    {
        if ( canContinue() )
        {
            commands.add( toShutdown( shutdownType ) );
            barrier.release();
        }
    }

    @Override
    public void noop()
    {
        if ( canContinue() )
        {
            commands.add( NOOP );
            barrier.release();
        }
    }

    @Override
    public void acknowledgeByeEventReceived()
    {
        if ( canContinue() )
        {
            commands.add( BYE_ACK );
            barrier.release();
        }
    }

    @Override
    protected Command nextCommand()
    {
        Command cmd = commands.poll();
        if ( cmd == null )
        {
            String cmdData = testClassNames.poll();
            return cmdData == null ? Command.TEST_SET_FINISHED : toRunClass( cmdData );
        }
        else
        {
            return cmd;
        }
    }

    @Override
    protected void beforeNextCommand()
        throws IOException
    {
        awaitNextTest();
    }

    @Override
    protected boolean isClosed()
    {
        return closed.get();
    }

    /**
     * Signal that a new test is to be provided.
     */
    @Override
    public void provideNewTest()
    {
        if ( canContinue() )
        {
            barrier.release();
        }
    }

    @Override
    public void close()
    {
        if ( closed.compareAndSet( false, true ) )
        {
            invalidateInternalBuffer();
            barrier.drainPermits();
            barrier.release();
        }
    }

    private void awaitNextTest()
        throws IOException
    {
        try
        {
            barrier.acquire();
        }
        catch ( InterruptedException e )
        {
            // help GC to free this object because StreamFeeder Thread cannot read it anyway after IOE
            invalidateInternalBuffer();
            throw new IOException( e.getLocalizedMessage() );
        }
    }
}