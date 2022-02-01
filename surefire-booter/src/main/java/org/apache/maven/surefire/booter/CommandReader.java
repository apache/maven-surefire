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
import org.apache.maven.surefire.api.booter.BiProperty;
import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.booter.MasterProcessCommand;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.provider.CommandChainReader;
import org.apache.maven.surefire.api.provider.CommandListener;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.StrictMath.max;
import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.booter.Command.toShutdown;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.apache.maven.surefire.shared.utils.StringUtils.isNotBlank;
import static org.apache.maven.surefire.api.util.internal.DaemonThreadFactory.newDaemonThread;

/**
 * Reader of commands coming from plugin(master) process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class CommandReader implements CommandChainReader
{
    private static final String LAST_TEST_SYMBOL = "";

    private final Queue<BiProperty<MasterProcessCommand, CommandListener>> listeners = new ConcurrentLinkedQueue<>();

    private final Thread commandThread = newDaemonThread( new CommandRunnable(), "surefire-forkedjvm-command-thread" );

    private final AtomicReference<Thread.State> state = new AtomicReference<>( NEW );

    private final CountDownLatch startMonitor = new CountDownLatch( 1 );

    private final Semaphore nextCommandNotifier = new Semaphore( 0 );

    private final CopyOnWriteArrayList<String> testClasses = new CopyOnWriteArrayList<>();

    private final MasterProcessChannelDecoder decoder;

    private final Shutdown shutdown;

    private final ConsoleLogger logger;

    private int iteratedCount;

    public CommandReader( MasterProcessChannelDecoder decoder, Shutdown shutdown, ConsoleLogger logger )
    {
        this.decoder = requireNonNull( decoder, "null decoder" );
        this.shutdown = requireNonNull( shutdown, "null Shutdown config" );
        this.logger = requireNonNull( logger, "null logger" );
        state.set( RUNNABLE );
        commandThread.start();
    }

    @Override
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

    @Override
    public void addSkipNextTestsListener( CommandListener listener )
    {
        addListener( SKIP_SINCE_NEXT_TEST, listener );
    }

    @Override
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
        listeners.add( new BiProperty<>( cmd, listener ) );
    }

    /**
     * @return test classes which have been retrieved by
     * {@link CommandReader#getIterableClasses(MasterProcessChannelEncoder)}.
     */
    Iterator<String> iterated()
    {
        return testClasses.subList( 0, iteratedCount ).iterator();
    }

    /**
     * The iterator can be used only in one Thread.
     * Two simultaneous instances are not allowed for sake of only one {@link #nextCommandNotifier}.
     *
     * @param eventChannel original stream in current JVM process
     * @return Iterator with test classes lazily loaded as commands from the main process
     */
    Iterable<String> getIterableClasses( MasterProcessChannelEncoder eventChannel )
    {
        return new ClassesIterable( eventChannel );
    }

    public void stop()
    {
        if ( !isStopped() )
        {
            state.set( TERMINATED );
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
        private final MasterProcessChannelEncoder eventChannel;

        ClassesIterable( MasterProcessChannelEncoder eventChannel )
        {
            this.eventChannel = eventChannel;
        }

        @Override
        public Iterator<String> iterator()
        {
            return new ClassesIterator( eventChannel );
        }
    }

    private final class ClassesIterator
        implements Iterator<String>
    {
        private final MasterProcessChannelEncoder eventChannel;

        private String clazz;

        private int nextQueueIndex;

        private ClassesIterator( MasterProcessChannelEncoder eventChannel )
        {
            this.eventChannel = eventChannel;
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
            eventChannel.acquireNextTest();
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
            boolean isTestSetFinished = false;
            try ( MasterProcessChannelDecoder commandReader = CommandReader.this.decoder )
            {
                while ( CommandReader.this.state.get() == RUNNABLE )
                {
                    Command command = commandReader.decode();
                    switch ( command.getCommandType() )
                    {
                        case RUN_CLASS:
                            String test = command.getData();
                            boolean inserted = CommandReader.this.insertToQueue( test );
                            if ( inserted )
                            {
                                CommandReader.this.wakeupIterator();
                                callListeners( command );
                            }
                            break;
                        case TEST_SET_FINISHED:
                            CommandReader.this.makeQueueFull();
                            isTestSetFinished = true;
                            CommandReader.this.wakeupIterator();
                            callListeners( command );
                            break;
                        case SHUTDOWN:
                            CommandReader.this.makeQueueFull();
                            CommandReader.this.wakeupIterator();
                            callListeners( command );
                                break;
                        case BYE_ACK:
                            callListeners( command );
                            // After SHUTDOWN no more commands can come.
                            // Hence, do NOT go back to blocking in I/O.
                            CommandReader.this.state.set( TERMINATED );
                            break;
                        default:
                            callListeners( command );
                            break;
                    }
                }
            }
            catch ( EOFException | ClosedChannelException e )
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
                // If #stop() method is called, reader thread is interrupted
                // and exception is InterruptedIOException or its cause is InterruptedException.
                if ( !( e instanceof InterruptedIOException || e.getCause() instanceof InterruptedException ) )
                {
                    String msg = "[SUREFIRE] std/in stream corrupted";
                    DumpErrorSingleton.getSingleton().dumpStreamException( e, msg );
                    CommandReader.this.logger.error( msg, e );
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

        private void callListeners( Command cmd )
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
                callListeners( toShutdown( shutdown ) );
            }
        }
    }
}
