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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static org.apache.maven.surefire.booter.Command.toShutdown;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.booter.MasterProcessCommand.decode;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThread;

/**
 * Reader of commands coming from plugin(master) process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class MasterProcessReader
{
    private static final MasterProcessReader READER = new MasterProcessReader();

    private final Queue<TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener>> listeners
        = new ConcurrentLinkedQueue<TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener>>();

    private final Thread commandThread = newDaemonThread( new CommandRunnable(), "surefire-forkedjvm-command-thread" );

    private final AtomicReference<Thread.State> state = new AtomicReference<Thread.State>( NEW );

    private final CountDownLatch startMonitor = new CountDownLatch( 1 );

    private final Node headTestClassQueue = new Node();

    private final Semaphore newCommandNotifier = new Semaphore( 0 );

    private volatile Node tailTestClassQueue = headTestClassQueue;

    private volatile Shutdown shutdown;

    private static class Node
    {
        final AtomicReference<Node> successor = new AtomicReference<Node>();
        volatile String item;
    }

    public static MasterProcessReader getReader()
    {
        final MasterProcessReader reader = READER;
        if ( reader.state.compareAndSet( NEW, RUNNABLE ) )
        {
            reader.commandThread.start();
        }
        return reader;
    }

    public MasterProcessReader setShutdown( Shutdown shutdown )
    {
        this.shutdown = shutdown;
        return this;
    }

    public boolean awaitStarted()
        throws TestSetFailedException
    {
        if ( state.get() == RUNNABLE )
        {
            try
            {
                startMonitor.await();
                return true;
            }
            catch ( InterruptedException e )
            {
                throw new TestSetFailedException( e.getLocalizedMessage() );
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * @param listener listener called with <em>Any</em> {@link MasterProcessCommand command type}
     */
    public void addListener( MasterProcessListener listener )
    {
        listeners.add( new TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener>( null, listener ) );
    }

    public void addTestListener( MasterProcessListener listener )
    {
        addListener( RUN_CLASS, listener );
    }

    public void addTestsFinishedListener( MasterProcessListener listener )
    {
        addListener( TEST_SET_FINISHED, listener );
    }

    public void addSkipNextListener( MasterProcessListener listener )
    {
        addListener( SKIP_SINCE_NEXT_TEST, listener );
    }

    public void addShutdownListener( MasterProcessListener listener )
    {
        addListener( SHUTDOWN, listener );
    }

    public void addNoopListener( MasterProcessListener listener )
    {
        addListener( NOOP, listener );
    }

    private void addListener( MasterProcessCommand cmd, MasterProcessListener listener )
    {
        listeners.add( new TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener>( cmd, listener ) );
    }

    public void removeListener( MasterProcessListener listener )
    {
        for ( Iterator<TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener>> it = listeners.iterator();
            it.hasNext(); )
        {
            TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener> listenerWrapper = it.next();
            if ( listener == listenerWrapper.getP2() )
            {
                it.remove();
            }
        }
    }

    Iterable<String> getIterableClasses( PrintStream originalOutStream )
    {
        return new ClassesIterable( headTestClassQueue, originalOutStream );
    }

    public void stop()
    {
        if ( state.compareAndSet( NEW, TERMINATED ) || state.compareAndSet( RUNNABLE, TERMINATED ) )
        {
            makeQueueFull();
            listeners.clear();
            commandThread.interrupt();
        }
    }

    private boolean isStopped()
    {
        return state.get() == TERMINATED;
    }

    private static boolean isLastNode( Node current )
    {
        return current.successor.get() == current;
    }

    private boolean isQueueFull()
    {
        return isLastNode( tailTestClassQueue );
    }

    /**
     * thread-safety: Must be called from single thread like here the reader thread only.
     */
    private boolean addTestClassToQueue( String item )
    {
        if ( tailTestClassQueue.item == null )
        {
            tailTestClassQueue.item = item;
            Node newNode = new Node();
            tailTestClassQueue.successor.set( newNode );
            tailTestClassQueue = newNode;
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * After this method returns the queue is closed, new item cannot be added and method
     * {@link #isQueueFull()} returns true.
     */
    @SuppressWarnings( { "all", "checkstyle:needbraces", "checkstyle:emptystatement" } )
    public void makeQueueFull()
    {
        // order between (#compareAndSet, and #get) matters in multithreading
        for ( Node tail = this.tailTestClassQueue;
              !tail.successor.compareAndSet( null, tail ) && tail.successor.get() != tail;
              tail = tail.successor.get() );
    }

    /**
     * thread-safety: Must be called from single thread like here the reader thread only.
     */
    private void insertToQueue( Command cmd )
    {
        MasterProcessCommand expectedCommandType = cmd.getCommandType();
        switch ( expectedCommandType )
        {
            case RUN_CLASS:
                addTestClassToQueue( cmd.getData() );
                break;
            case TEST_SET_FINISHED:
                makeQueueFull();
                break;
            default:
                // checkstyle noop
                break;
        }
    }

    private void insertToListeners( Command cmd )
    {
        MasterProcessCommand expectedCommandType = cmd.getCommandType();
        for ( TwoPropertiesWrapper<MasterProcessCommand, MasterProcessListener> listenerWrapper
            : MasterProcessReader.this.listeners )
        {
            MasterProcessCommand commandType = listenerWrapper.getP1();
            MasterProcessListener listener = listenerWrapper.getP2();
            if ( commandType == null || commandType == expectedCommandType )
            {
                listener.update( cmd );
            }
        }
    }

    /**
     * thread-safety: Must be called from single thread like here the reader thread only.
     */
    private void insert( Command cmd )
    {
        insertToQueue( cmd );
        insertToListeners( cmd );
    }

    private final class ClassesIterable
        implements Iterable<String>
    {
        private final Node head;
        private final PrintStream originalOutStream;

        ClassesIterable( Node head, PrintStream originalOutStream )
        {
            this.head = head;
            this.originalOutStream = originalOutStream;
        }

        public Iterator<String> iterator()
        {
            return new ClassesIterator( head, originalOutStream );
        }
    }

    private final class ClassesIterator
        implements Iterator<String>
    {
        private final PrintStream originalOutStream;

        private Node current;

        private String clazz;

        private ClassesIterator( Node current, PrintStream originalOutStream )
        {
            this.current = current;
            this.originalOutStream = originalOutStream;
        }

        public boolean hasNext()
        {
            popUnread();
            return isNotBlank( clazz );
        }

        public String next()
        {
            popUnread();
            try
            {
                if ( isBlank( clazz ) )
                {
                    throw new NoSuchElementException();
                }
                else
                {
                    return clazz;
                }
            }
            finally
            {
                clazz = null;
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void popUnread()
        {
            if ( isStopped() )
            {
                clazz = null;
                return;
            }

            if ( isBlank( clazz ) )
            {
                do
                {
                    requestNextTest();
                    if ( isLastNode( current ) )
                    {
                        clazz = null;
                    }
                    else if ( current.item == null )
                    {
                        do
                        {
                            awaitNextTest();
                            if ( isStopped() )
                            {
                                clazz = null;
                                return;
                            }
                        } while ( current.item == null && !isLastNode( current ) );
                        clazz = current.item;
                        current = current.successor.get();
                    }
                    else
                    {
                        clazz = current.item;
                        current = current.successor.get();
                    }
                }
                while ( tryNullWhiteClass() );
            }

            if ( isStopped() )
            {
                clazz = null;
            }
        }

        private boolean tryNullWhiteClass()
        {
            if ( clazz != null && isBlank( clazz ) )
            {
                clazz = null;
                return true;
            }
            else
            {
                return false;
            }
        }

        private void requestNextTest()
        {
            byte[] encoded = encodeStringForForkCommunication( ( (char) BOOTERCODE_NEXT_TEST ) + ",0,want more!\n" );
            originalOutStream.write( encoded, 0, encoded.length );
        }
    }

    /**
     * thread-safety: Must be called from single thread like here the reader thread only.
     */
    private Command read( DataInputStream stdIn )
        throws IOException
    {
        Command command = decode( stdIn );
        if ( command != null )
        {
            insertToQueue( command );
        }
        return command;
    }

    private void awaitNextTest()
    {
        newCommandNotifier.acquireUninterruptibly();
    }

    private void wakeupIterator()
    {
        newCommandNotifier.release();
    }

    private final class CommandRunnable
        implements Runnable
    {
        public void run()
        {
            MasterProcessReader.this.startMonitor.countDown();
            DataInputStream stdIn = new DataInputStream( System.in );
            boolean isTestSetFinished = false;
            try
            {
                while ( MasterProcessReader.this.state.get() == RUNNABLE )
                {
                    Command command = read( stdIn );
                    if ( command == null )
                    {
                        System.err.println( "[SUREFIRE] std/in stream corrupted: first sequence not recognized" );
                        break;
                    }
                    else
                    {
                        switch ( command.getCommandType() )
                        {
                            case TEST_SET_FINISHED:
                                isTestSetFinished = true;
                                wakeupIterator();
                                break;
                            case RUN_CLASS:
                                wakeupIterator();
                                break;
                            case SHUTDOWN:
                                insertToQueue( Command.TEST_SET_FINISHED );
                                wakeupIterator();
                                break;
                            default:
                                // checkstyle do nothing
                                break;
                        }

                        insertToListeners( command );
                    }
                }
            }
            catch ( EOFException e )
            {
                MasterProcessReader.this.state.set( TERMINATED );
                if ( !isTestSetFinished )
                {
                    exitByConfiguration();
                    // does not go to finally
                }
            }
            catch ( IOException e )
            {
                MasterProcessReader.this.state.set( TERMINATED );
                // If #stop() method is called, reader thread is interrupted and cause is InterruptedException.
                if ( !( e.getCause() instanceof InterruptedException ) )
                {
                    System.err.println( "[SUREFIRE] std/in stream corrupted" );
                    e.printStackTrace();
                }
            }
            finally
            {
                // ensure fail-safe iterator as well as safe to finish in for-each loop using ClassesIterator
                if ( !isTestSetFinished )
                {
                    insert( Command.TEST_SET_FINISHED );
                }
                wakeupIterator();
            }
        }

        /**
         * thread-safety: Must be called from single thread like here the reader thread only.
         */
        private void exitByConfiguration()
        {
            Shutdown shutdown = MasterProcessReader.this.shutdown; // won't read inconsistent changes through the stack
            if ( shutdown != null )
            {
                insert( Command.TEST_SET_FINISHED ); // lazily
                wakeupIterator();
                insertToListeners( toShutdown( shutdown ) );
                switch ( shutdown )
                {
                    case EXIT:
                        System.exit( 1 );
                    case KILL:
                        Runtime.getRuntime().halt( 1 );
                    case DEFAULT:
                    default:
                        // should not happen; otherwise you missed enum case
                        break;
                }
            }
        }
    }
}
