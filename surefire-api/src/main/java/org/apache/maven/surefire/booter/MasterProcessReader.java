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
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;
import static org.apache.maven.surefire.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.booter.MasterProcessCommand.decode;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThread;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_NEXT_TEST;

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

    private final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

    private final Node head = new Node();

    private final CountDownLatch startMonitor = new CountDownLatch( 1 );

    private volatile Node tail = head;

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

    public void addListener( MasterProcessCommand cmd, MasterProcessListener listener )
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
        return new ClassesIterable( head, originalOutStream );
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

    private static boolean isLastNode( Node current )
    {
        return current.successor.get() == current;
    }

    private boolean isQueueFull()
    {
        return isLastNode( tail );
    }

    private boolean addToQueue( String item )
    {
        if ( tail.item == null )
        {
            tail.item = item;
            Node newNode = new Node();
            tail.successor.set( newNode );
            tail = newNode;
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
        for ( Node tail = this.tail;
              !tail.successor.compareAndSet( null, tail ) && tail.successor.get() != tail;
              tail = tail.successor.get() );
    }

    private void insertForQueue( Command cmd )
    {
        MasterProcessCommand expectedCommandType = cmd.getCommandType();
        switch ( expectedCommandType )
        {
            case RUN_CLASS:
                addToQueue( cmd.getData() );
                break;
            case TEST_SET_FINISHED:
                makeQueueFull();
                break;
            default:
                // checkstyle noop
                break;
        }
    }

    private void insertForListeners( Command cmd )
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

    private void insert( Command cmd )
    {
        insertForQueue( cmd );
        insertForListeners( cmd );
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
            if ( state.get() == TERMINATED )
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
                            await();
                            /**
                             * {@link java.util.concurrent.locks.LockSupport#park()}
                             * may spuriously (that is, for no reason) return, therefore the loop here.
                             */
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

            if ( state.get() == TERMINATED )
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

    private Command read( DataInputStream stdIn )
        throws IOException
    {
        Command command = decode( stdIn );
        if ( command != null )
        {
            insertForQueue( command );
        }
        return command;
    }

    private void await()
    {
        final Thread currentThread = Thread.currentThread();
        try
        {
            waiters.add( currentThread );
            park();
        }
        finally
        {
            waiters.remove( currentThread );
        }
    }

    private void wakeupWaiters()
    {
        for ( Thread waiter : waiters )
        {
            unpark( waiter );
        }
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
                        if ( command.getCommandType() == TEST_SET_FINISHED )
                        {
                            isTestSetFinished = true;
                            wakeupWaiters();
                        }
                        else if ( command.getCommandType() == RUN_CLASS )
                        {
                            wakeupWaiters();
                        }
                        insertForListeners( command );
                    }
                }
            }
            catch ( EOFException e )
            {
                MasterProcessReader.this.state.set( TERMINATED );
            }
            catch ( IOException e )
            {
                MasterProcessReader.this.state.set( TERMINATED );
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
                    insert( new Command( TEST_SET_FINISHED ) );
                }
                wakeupWaiters();
            }
        }
    }
}
