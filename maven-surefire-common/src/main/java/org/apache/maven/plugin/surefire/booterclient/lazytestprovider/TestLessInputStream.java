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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.maven.surefire.booter.Command.BYE_ACK;
import static org.apache.maven.surefire.booter.Command.NOOP;
import static org.apache.maven.surefire.booter.Command.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.booter.Command.toShutdown;

/**
 * Dispatches commands without tests.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class TestLessInputStream
    extends AbstractCommandStream
{
    private final Semaphore barrier = new Semaphore( 0 );

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Queue<Command> immediateCommands = new ConcurrentLinkedQueue<Command>();

    private final TestLessInputStreamBuilder builder;

    private Iterator<Command> cachableCommands;

    private TestLessInputStream( TestLessInputStreamBuilder builder )
    {
        this.builder = builder;
    }

    @Override
    public void provideNewTest()
    {
    }

    @Override
    public void skipSinceNextTest()
    {
        if ( canContinue() )
        {
            immediateCommands.add( SKIP_SINCE_NEXT_TEST );
            barrier.release();
        }
    }

    @Override
    public void shutdown( Shutdown shutdownType )
    {
        if ( canContinue() )
        {
            immediateCommands.add( toShutdown( shutdownType ) );
            barrier.release();
        }
    }

    @Override
    public void noop()
    {
        if ( canContinue() )
        {
            immediateCommands.add( NOOP );
            barrier.release();
        }
    }

    @Override
    public void acknowledgeByeEventReceived()
    {
        if ( canContinue() )
        {
            immediateCommands.add( BYE_ACK );
            barrier.release();
        }
    }

    @Override
    protected boolean isClosed()
    {
        return closed.get();
    }

    @Override
    protected Command nextCommand()
    {
        Command cmd = immediateCommands.poll();
        if ( cmd == null )
        {
            if ( cachableCommands == null )
            {
                cachableCommands = builder.getIterableCachable().iterator();
            }

            cmd = cachableCommands.next();
        }
        return cmd;
    }

    @Override
    protected void beforeNextCommand()
        throws IOException
    {
        awaitNextCommand();
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

    /**
     * For testing purposes only.
     *
     * @return permits used internally by {@link #beforeNextCommand()}
     */
    int availablePermits()
    {
        return barrier.availablePermits();
    }

    private void awaitNextCommand()
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

    /**
     * Builds {@link TestLessInputStream streams}, registers cachable commands
     * and provides accessible API to dispatch immediate commands to all atomically
     * alive streams.
     */
    public static final class TestLessInputStreamBuilder
    {
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Queue<TestLessInputStream> aliveStreams = new ConcurrentLinkedQueue<TestLessInputStream>();
        private final ImmediateCommands immediateCommands = new ImmediateCommands();
        private final CachableCommands cachableCommands = new CachableCommands();
        private final Node head = new Node( null );
        private final Iterable<Command> iterableCachable;

        public TestLessInputStreamBuilder()
        {
            iterableCachable = new Iterable<Command>()
            {
                @Override
                public Iterator<Command> iterator()
                {
                    return new CIt();
                }
            };
        }

        public TestLessInputStream build()
        {
            Lock lock = rwLock.writeLock();
            lock.lock();
            try
            {
                TestLessInputStream is = new TestLessInputStream( this );
                aliveStreams.offer( is );
                return is;
            }
            finally
            {
                lock.unlock();
            }
        }

        public void removeStream( TestLessInputStream is )
        {
            Lock lock = rwLock.writeLock();
            lock.lock();
            try
            {
                aliveStreams.remove( is );
            }
            finally
            {
                lock.unlock();
            }
        }

        public NotifiableTestStream getImmediateCommands()
        {
            return immediateCommands;
        }

        public NotifiableTestStream getCachableCommands()
        {
            return cachableCommands;
        }

        /**
         * The iterator is not thread safe.
         */
        Iterable<Command> getIterableCachable()
        {
            return iterableCachable;
        }

        @SuppressWarnings( "checkstyle:innerassignment" )
        private boolean addTailNodeIfAbsent( Command command )
        {
            Node newTail = new Node( command );
            Node currentTail = head;
            do
            {
                for ( Node successor; ( successor = currentTail.next.get() ) != null; )
                {
                    currentTail = successor;
                    if ( command.equals( currentTail.command ) )
                    {
                        return false;
                    }
                }
            } while ( !currentTail.next.compareAndSet( null, newTail ) );
            return true;
        }

        private static Node nextCachedNode( Node current )
        {
            return current.next.get();
        }

        private final class CIt
            implements Iterator<Command>
        {
            private Node node = TestLessInputStreamBuilder.this.head;

            @Override
            public boolean hasNext()
            {
                return examineNext( false ) != null;
            }

            @Override
            public Command next()
            {
                Command command = examineNext( true );
                if ( command == null )
                {
                    throw new NoSuchElementException();
                }
                return command;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }

            private Command examineNext( boolean store )
            {
                Node next = nextCachedNode( node );
                if ( store && next != null )
                {
                    node = next;
                }
                return next == null ? null : next.command;
            }
        }

        /**
         * Event is called just now for all alive streams and command is not persisted.
         */
        private final class ImmediateCommands
            implements NotifiableTestStream
        {
            @Override
            public void provideNewTest()
            {
            }

            @Override
            public void skipSinceNextTest()
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    for ( TestLessInputStream aliveStream : TestLessInputStreamBuilder.this.aliveStreams )
                    {
                        aliveStream.skipSinceNextTest();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            @Override
            public void shutdown( Shutdown shutdownType )
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    for ( TestLessInputStream aliveStream : TestLessInputStreamBuilder.this.aliveStreams )
                    {
                        aliveStream.shutdown( shutdownType );
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            @Override
            public void noop()
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    for ( TestLessInputStream aliveStream : TestLessInputStreamBuilder.this.aliveStreams )
                    {
                        aliveStream.noop();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            @Override
            public void acknowledgeByeEventReceived()
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    for ( TestLessInputStream aliveStream : TestLessInputStreamBuilder.this.aliveStreams )
                    {
                        aliveStream.acknowledgeByeEventReceived();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }
        }

        /**
         * Event is persisted.
         */
        private final class CachableCommands
            implements NotifiableTestStream
        {
            @Override
            public void provideNewTest()
            {
            }

            @Override
            public void skipSinceNextTest()
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    if ( TestLessInputStreamBuilder.this.addTailNodeIfAbsent( SKIP_SINCE_NEXT_TEST ) )
                    {
                        release();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            @Override
            public void shutdown( Shutdown shutdownType )
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    if ( TestLessInputStreamBuilder.this.addTailNodeIfAbsent( toShutdown( shutdownType ) ) )
                    {
                        release();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            @Override
            public void noop()
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    if ( TestLessInputStreamBuilder.this.addTailNodeIfAbsent( NOOP ) )
                    {
                        release();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            @Override
            public void acknowledgeByeEventReceived()
            {
                Lock lock = rwLock.readLock();
                lock.lock();
                try
                {
                    if ( TestLessInputStreamBuilder.this.addTailNodeIfAbsent( BYE_ACK ) )
                    {
                        release();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }

            private void release()
            {
                for ( TestLessInputStream aliveStream : TestLessInputStreamBuilder.this.aliveStreams )
                {
                    aliveStream.barrier.release();
                }
            }
        }

        private static class Node
        {
            private final AtomicReference<Node> next = new AtomicReference<Node>();
            private final Command command;

            Node( Command command )
            {
                this.command = command;
            }
        }
    }
}
