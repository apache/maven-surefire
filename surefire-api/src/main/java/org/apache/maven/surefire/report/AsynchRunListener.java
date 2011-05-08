package org.apache.maven.surefire.report;

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

import java.util.concurrent.LinkedBlockingQueue;
import org.apache.maven.surefire.util.internal.ByteBuffer;

/**
 * Transfers further processing of the request to a different thread,
 * returning immediately to calling code.
 * Deals with system.out/err from single-threaded processes.
 * <p/>
 */
public class AsynchRunListener
    implements RunListener, ConsoleOutputReceiver, ConsoleLogger
{
    private final LinkedBlockingQueue blockingQueue = new LinkedBlockingQueue();

    private final Processor processor;

    private final RunListener target;

    private final ConsoleOutputReceiver consoleOutputReceiver;

    private final Thread asynchRunListener;

    static class Processor
        implements Runnable
    {
        private final LinkedBlockingQueue blockingQueue;

        private volatile InterruptedException exception;

        Processor( LinkedBlockingQueue blockingQueue )
        {
            this.blockingQueue = blockingQueue;
        }

        public void run()
        {
            try
            {
                Runnable take;
                take = (Runnable) blockingQueue.take();
                while ( take != poison )
                {
                    take.run();
                    take = (Runnable) blockingQueue.take();
                }
            }
            catch ( InterruptedException e )
            {
                this.exception = e;
            }
        }

        public InterruptedException getException()
        {
            return exception;
        }
    }

    public AsynchRunListener( RunListener target, String role )
    {
        this.processor = new Processor( blockingQueue );
        this.target = target;
        consoleOutputReceiver = (ConsoleOutputReceiver) target;
        asynchRunListener = new Thread( processor, "AsynchRunListener" + role );
        asynchRunListener.start();
    }

    public void testSetStarting( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testSetStarting( report );
            }
        } );
    }

    public void testSetCompleted( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testSetCompleted( report );
            }
        } );
    }

    public void testStarting( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testStarting( report );
            }
        } );
    }

    public void testSucceeded( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testSucceeded( report );
            }
        } );
    }

    public void testAssumptionFailure( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testAssumptionFailure( report );
            }
        } );

    }

    public void testError( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testError( report );
            }
        } );
    }

    public void testFailed( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testFailed( report );
            }
        } );
    }

    public void testSkipped( final ReportEntry report )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                target.testSkipped( report );
            }
        } );
    }

    static class JoinableTestOutput
        implements Runnable
    {
        final byte[] buf;

        final int off;

        final int len;

        final boolean stdout;

        private final ConsoleOutputReceiver consoleOutputReceiver;

        JoinableTestOutput( final byte[] buf, final int off, final int len, final boolean stdout,
                            ConsoleOutputReceiver consoleOutputReceiver )
        {
            this.buf = ByteBuffer.copy( buf, off, len );
            this.off = 0;
            this.len = len;
            this.stdout = stdout;
            this.consoleOutputReceiver = consoleOutputReceiver;
        }

        public void run()
        {
            consoleOutputReceiver.writeTestOutput( buf, off, len, stdout );
        }

        public JoinableTestOutput append( JoinableTestOutput other )
        {
            byte[] combined = ByteBuffer.join( buf, this.off, this.len, other.buf, other.off, other.len );
            return new JoinableTestOutput( combined, 0, combined.length, stdout, consoleOutputReceiver );
        }

    }

    public void writeTestOutput( final byte[] buf, final int off, final int len, final boolean stdout )
    {
        blockingQueue.add( new JoinableTestOutput( buf, off, len, stdout, consoleOutputReceiver ) );
    }

    public void info( final String message )
    {
        blockingQueue.add( new Runnable()
        {
            public void run()
            {
                ( (ConsoleLogger) consoleOutputReceiver ).info( message );
            }
        } );
    }

    private static final Runnable poison = new Runnable()
    {
        public void run()
        {
        }
    };

    public void close()
        throws ReporterException
    {
        try
        {
            blockingQueue.add( poison );
            asynchRunListener.join();
            final InterruptedException exception = processor.getException();
            if ( exception != null )
            {
                throw exception;
            }
        }
        catch ( InterruptedException e )
        {
            throw new ReporterException( "When waiting", e );
        }

    }
}
