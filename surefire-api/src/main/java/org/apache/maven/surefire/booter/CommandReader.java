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
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static java.lang.StrictMath.max;
import static org.apache.maven.surefire.booter.Command.toShutdown;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.booter.MasterProcessCommand.decode;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThread;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static org.apache.maven.surefire.util.internal.ObjectUtils.requireNonNull;

/**
 * Reader of commands coming from plugin(master) process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class CommandReader
{
    private static final String LAST_TEST_SYMBOL = "";

    private static final CommandReader READER = new CommandReader();

    private final Queue<BiProperty<MasterProcessCommand, CommandListener>> listeners
        = new ConcurrentLinkedQueue<BiProperty<MasterProcessCommand, CommandListener>>();

    private final Thread commandThread = newDaemonThread( new CommandRunnable(), "surefire-forkedjvm-command-thread" );

    private final AtomicReference<Thread.State> state = new AtomicReference<Thread.State>( NEW );

    private final CountDownLatch startMonitor = new CountDownLatch( 1 );

    private final Semaphore nextCommandNotifier = new Semaphore( 0 );

    private final CopyOnWriteArrayList<String> testClasses = new CopyOnWriteArrayList<String>();

    private volatile Shutdown shutdown;

    private int iteratedCount;

    private volatile ConsoleLogger logger = new NullConsoleLogger();

    private CommandReader()
    {
    }

    public static CommandReader getReader()
    {
        final CommandReader reader = READER;
        if ( reader.state.compareAndSet( NEW, RUNNABLE ) )
        {
            reader.commandThread.start();
        }
        return reader;
    }

    public CommandReader setShutdown( Shutdown shutdown )
    {
        this.shutdown = shutdown;
        return this;
    }

    public CommandReader setLogger( ConsoleLogger logger )
    {
        this.logger = requireNonNull( logger, "null logger" );
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
                DumpErrorSingleton.getSingleton().dumpException( e );
                throw new TestSetFailedException( e.getLocalizedMessage() );
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * @param listener listener called with <b>Any</b> {@link MasterProcessCommand command type}
     */
    public void addListener( CommandListener listener )
    {
        listeners.add( new BiProperty<MasterProcessCommand, CommandListener>( null, listener ) );
    }

    public void addTestListener( CommandListener listener )
    {
        addListener( RUN_CLASS, listener );
    }

    public void addTestsFinishedListener( CommandListener listener )
    {
        addListener( TEST_SET_FINISHED, listener );
    }

    public void addSkipNextTestsListener( CommandListener listener )
    {
        addListener( SKIP_SINCE_NEXT_TEST, listener );
    }

    public void addShutdownListener( CommandListener listener )
    {
        addListener( SHUTDOWN, listener );
    }

    public void addNoopListener( CommandListener listener )
    {
        addListener( NOOP, listener );
    }

    public void addByeAckListener( CommandListener listener )
    {
        addListener( BYE_ACK, listener );
    }

    private void addListener( MasterProcessCommand cmd, CommandListener listener )
    {
        listeners.add( new BiProperty<MasterProcessCommand, CommandListener>( cmd, listener ) );
    }

    public void removeListener( CommandListener listener )
    {
        for ( Iterator<BiProperty<MasterProcessCommand, CommandListener>> it = listeners.iterator(); it.hasNext(); )
        {
            BiProperty<MasterProcessCommand, CommandListener> listenerWrapper = it.next();
            if ( listener == listenerWrapper.getP2() )
            {
                it.remove();
            }
        }
    }

    /**
     * @return test classes which have been retrieved by {@link CommandReader#getIterableClasses(PrintStream)}.
     */
    Iterator<String> iterated()
    {
        return testClasses.subList( 0, iteratedCount ).iterator();
    }

    /**
     * The iterator can be used only in one Thread.
     * Two simultaneous instances are not allowed for sake of only one {@link #nextCommandNotifier}.
     *
     * @param originalOutStream original stream in current JVM process
     * @return Iterator with test classes lazily loaded as commands from the main process
     */
    Iterable<String> getIterableClasses( PrintStream originalOutStream )
    {
        return new ClassesIterable( originalOutStream );
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

    /**
     * @return {@code true} if {@link #LAST_TEST_SYMBOL} found at the last index in {@link #testClasses}.
     */
    private boolean isQueueFull()
    {
        // The problem with COWAL is that such collection doe not have operation getLast, however it has get(int)
        // and we need both atomic.
        //
        // Both lines can be Java Concurrent, but the last operation is atomic with optimized search.
        // Searching index of LAST_TEST_SYMBOL in the only last few (concurrently) inserted strings.
        // The insert operation is concurrent with this method.
        // Prerequisite: The strings are added but never removed and the method insertToQueue() does not
        // allow adding a string after LAST_TEST_SYMBOL.
        int searchFrom = max( 0, testClasses.size() - 1 );
        return testClasses.indexOf( LAST_TEST_SYMBOL, searchFrom ) != -1;
    }

    private void makeQueueFull()
    {
        testClasses.addIfAbsent( LAST_TEST_SYMBOL );
    }

    private boolean insertToQueue( String test )
    {
        return isNotBlank( test ) && !isQueueFull() && testClasses.add( test );
    }

    private final class ClassesIterable
        implements Iterable<String>
    {
        private final PrintStream originalOutStream;

        ClassesIterable( PrintStream originalOutStream )
        {
            this.originalOutStream = originalOutStream;
        }

        @Override
        public Iterator<String> iterator()
        {
            return new ClassesIterator( originalOutStream );
        }
    }

    private final class ClassesIterator
        implements Iterator<String>
    {
        private final PrintStream originalOutStream;

        private String clazz;

        private int nextQueueIndex;

        private ClassesIterator( PrintStream originalOutStream )
        {
            this.originalOutStream = originalOutStream;
        }

        @Override
        public boolean hasNext()
        {
            popUnread();
            return isNotBlank( clazz );
        }

        @Override
        public String next()
        {
            popUnread();
            try
            {
                if ( isBlank( clazz ) )
                {
                    throw new NoSuchElementException( CommandReader.this.isStopped() ? "stream was stopped" : "" );
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

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void popUnread()
        {
            if ( shouldFinish() )
            {
                clazz = null;
                return;
            }

            if ( isBlank( clazz ) )
            {
                requestNextTest();
                CommandReader.this.awaitNextTest();
                if ( shouldFinish() )
                {
                    clazz = null;
                    return;
                }
                clazz = CommandReader.this.testClasses.get( nextQueueIndex++ );
                CommandReader.this.iteratedCount = nextQueueIndex;
            }

            if ( CommandReader.this.isStopped() )
            {
                clazz = null;
            }
        }

        private void requestNextTest()
        {
            byte[] encoded = encodeStringForForkCommunication( ( (char) BOOTERCODE_NEXT_TEST ) + ",0,want more!\n" );
            synchronized ( originalOutStream )
            {
                originalOutStream.write( encoded, 0, encoded.length );
                originalOutStream.flush();
            }
        }

        private boolean shouldFinish()
        {
            boolean wasLastTestRead = isEndSymbolAt( nextQueueIndex );
            return CommandReader.this.isStopped() || wasLastTestRead;
        }

        private boolean isEndSymbolAt( int index )
        {
            return CommandReader.this.isQueueFull() && 1 + index == CommandReader.this.testClasses.size();
        }
    }

    private void awaitNextTest()
    {
        nextCommandNotifier.acquireUninterruptibly();
    }

    private void wakeupIterator()
    {
        nextCommandNotifier.release();
    }

    private final class CommandRunnable
        implements Runnable
    {
        @Override
        public void run()
        {
            CommandReader.this.startMonitor.countDown();
            DataInputStream stdIn = new DataInputStream( System.in );
            boolean isTestSetFinished = false;
            try
            {
                while ( CommandReader.this.state.get() == RUNNABLE )
                {
                    Command command = decode( stdIn );
                    if ( command == null )
                    {
                        String errorMessage = "[SUREFIRE] std/in stream corrupted: first sequence not recognized";
                        DumpErrorSingleton.getSingleton().dumpStreamText( errorMessage );
                        logger.error( errorMessage );
                        break;
                    }
                    else
                    {
                        switch ( command.getCommandType() )
                        {
                            case RUN_CLASS:
                                String test = command.getData();
                                boolean inserted = CommandReader.this.insertToQueue( test );
                                if ( inserted )
                                {
                                    CommandReader.this.wakeupIterator();
                                    insertToListeners( command );
                                }
                                break;
                            case TEST_SET_FINISHED:
                                CommandReader.this.makeQueueFull();
                                isTestSetFinished = true;
                                CommandReader.this.wakeupIterator();
                                insertToListeners( command );
                                break;
                            case SHUTDOWN:
                                CommandReader.this.makeQueueFull();
                                CommandReader.this.wakeupIterator();
                                insertToListeners( command );
                                break;
                            default:
                                insertToListeners( command );
                                break;
                        }
                    }
                }
            }
            catch ( EOFException e )
            {
                CommandReader.this.state.set( TERMINATED );
                if ( !isTestSetFinished )
                {
                    String msg = "TestSet has not finished before stream error has appeared >> "
                                         + "initializing exit by non-null configuration: "
                                         + CommandReader.this.shutdown;
                    DumpErrorSingleton.getSingleton().dumpStreamException( e, msg );

                    exitByConfiguration();
                    // does not go to finally for non-default config: Shutdown.EXIT or Shutdown.KILL
                }
            }
            catch ( IOException e )
            {
                CommandReader.this.state.set( TERMINATED );
                // If #stop() method is called, reader thread is interrupted and cause is InterruptedException.
                if ( !( e.getCause() instanceof InterruptedException ) )
                {
                    String msg = "[SUREFIRE] std/in stream corrupted";
                    DumpErrorSingleton.getSingleton().dumpStreamException( e, msg );
                    logger.error( msg, e );
                }
            }
            finally
            {
                // ensure fail-safe iterator as well as safe to finish in for-each loop using ClassesIterator
                if ( !isTestSetFinished )
                {
                    CommandReader.this.makeQueueFull();
                }
                CommandReader.this.wakeupIterator();
            }
        }

        private void insertToListeners( Command cmd )
        {
            MasterProcessCommand expectedCommandType = cmd.getCommandType();
            for ( BiProperty<MasterProcessCommand, CommandListener> listenerWrapper : CommandReader.this.listeners )
            {
                MasterProcessCommand commandType = listenerWrapper.getP1();
                CommandListener listener = listenerWrapper.getP2();
                if ( commandType == null || commandType == expectedCommandType )
                {
                    listener.update( cmd );
                }
            }
        }

        private void exitByConfiguration()
        {
            Shutdown shutdown = CommandReader.this.shutdown; // won't read inconsistent changes through the stack
            if ( shutdown != null )
            {
                CommandReader.this.makeQueueFull();
                CommandReader.this.wakeupIterator();
                insertToListeners( toShutdown( shutdown ) );
                if ( shutdown.isExit() )
                {
                    System.exit( 1 );
                }
                else if ( shutdown.isKill() )
                {
                    Runtime.getRuntime().halt( 1 );
                }
                // else is default: other than Shutdown.DEFAULT should not happen; otherwise you missed enum case
            }
        }
    }

}
