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

import org.apache.maven.surefire.util.internal.DaemonThreadFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.State.NEW;
import static java.lang.Thread.State.RUNNABLE;
import static java.lang.Thread.State.TERMINATED;
import static org.apache.maven.surefire.booter.MasterProcessCommand.TEST_SET_FINISHED;
import static org.apache.maven.surefire.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.booter.MasterProcessCommand.decode;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_NEXT_TEST;

/**
 * Testing singleton {@code MasterProcessReader}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public final class MasterProcessReader
{
    private static final MasterProcessReader READER = new MasterProcessReader();

    private final BlockingQueue<Command> classes = new LinkedBlockingQueue<Command>();

    private final Queue<MasterProcessListener> listeners = new ConcurrentLinkedQueue<MasterProcessListener>();

    private final Thread commandThread =
        DaemonThreadFactory.newDaemonThread( new CommandRunnable(), "surefire-forkedjvm-command-thread" );

    private final AtomicReference<Thread.State> state = new AtomicReference<Thread.State>( NEW );

    public static MasterProcessReader getReader()
    {
        if ( READER.state.compareAndSet( NEW, RUNNABLE ) )
        {
            READER.commandThread.start();
        }
        return READER;
    }

    private MasterProcessReader()
    {
        commandThread.setDaemon( true );
    }

    Iterable<String> getIterableClasses( PrintStream originalOutStream )
    {
        return new ClassesIterable( originalOutStream );
    }

    void stop( boolean interruptCurrentThread )
    {
        if ( state.compareAndSet( NEW, TERMINATED ) || state.compareAndSet( RUNNABLE, TERMINATED ) )
        {
            classes.drainTo( new ArrayList<Command>() );
            listeners.clear();
            commandThread.interrupt();
            if ( interruptCurrentThread )
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    private final class ClassesIterable
        implements Iterable<String>
    {
        private final ClassesIterator it;

        public ClassesIterable( PrintStream originalOutStream )
        {
            it = new ClassesIterator( originalOutStream );
        }

        public Iterator<String> iterator()
        {
            return it;
        }
    }

    private final class ClassesIterator
        implements Iterator<String>
    {
        private final PrintStream originalOutStream;

        private String clazz;

        private ClassesIterator( PrintStream originalOutStream )
        {
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
                requestNextTest();
                try
                {
                    do
                    {
                        Command command = MasterProcessReader.this.classes.take();
                        clazz = command.getCommandType() == TEST_SET_FINISHED ? null : command.getData();
                    }
                    while ( tryNullWhiteClass() );
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    clazz = null;
                }
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

    private final class CommandRunnable
        implements Runnable
    {
        public void run()
        {
            DataInputStream stdIn = new DataInputStream( System.in );
            try
            {
                while ( MasterProcessReader.this.state.get() == RUNNABLE )
                {
                    Command command = decode( stdIn );
                    if ( command != null )
                    {
                        // command is null if stream is corrupted, i.e. the first sequence could not be recognized
                        insert( command );
                    }
                }
            }
            catch ( IOException e )
            {
                // ensure fail-safe iterator as well as safe to finish in for-each loop using ClassesIterator
                insert( new Command( TEST_SET_FINISHED ) );
                // and let us know what has happened with the stream
                throw new IllegalStateException( e.getLocalizedMessage(), e );
            }
        }

        @SuppressWarnings( "unchecked" )
        private void insert( Command cmd )
        {
            MasterProcessCommand commandType = cmd.getCommandType();
            if ( commandType == RUN_CLASS || commandType == TEST_SET_FINISHED )
            {
                MasterProcessReader.this.classes.add( cmd );
            }
            else
            {
                for ( MasterProcessListener listener : MasterProcessReader.this.listeners )
                {
                    listener.update( cmd );
                }
            }
        }
    }
}
