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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static java.lang.System.setErr;
import static java.lang.System.setOut;

/**
 * Deals with system.out/err.
 * <br>
 */
public final class ConsoleOutputCapture
{
    public static void startCapture( ConsoleOutputReceiver target )
    {
        setOut( new ForwardingPrintStream( true, target ) );
        setErr( new ForwardingPrintStream( false, target ) );
    }

    private static final class ForwardingPrintStream
        extends PrintStream
    {
        private final boolean isStdout;
        private final ConsoleOutputReceiver target;

        ForwardingPrintStream( boolean stdout, ConsoleOutputReceiver target )
        {
            super( new NullOutputStream() );
            isStdout = stdout;
            this.target = target;
        }

        @Override
        public void write( byte[] buf, int off, int len )
        {
            target.writeTestOutput( new String( buf, off, len ), false, isStdout );
        }

        @Override
        public void write( byte[] b )
            throws IOException
        {
            write( b, 0, b.length );
        }

        @Override
        public void write( int b )
        {
            try
            {
                write( new byte[] { (byte) b } );
            }
            catch ( IOException e )
            {
                setError();
            }
        }

        @Override
        public void println( boolean x )
        {
            println( x ? "true" : "false" );
        }

        @Override
        public void println( char x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( int x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( long x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( float x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( double x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( char[] x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( Object x )
        {
            println( String.valueOf( x ) );
        }

        @Override
        public void println( String s )
        {
            target.writeTestOutput( s == null ? "null" : s, true, isStdout );
        }

        @Override
        public void println()
        {
            target.writeTestOutput( "", true, isStdout );
        }

        @Override
        public void print( boolean x )
        {
            print( x ? "true" : "false" );
        }

        @Override
        public void print( char x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( int x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( long x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( float x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( double x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( char[] x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( Object x )
        {
            print( String.valueOf( x ) );
        }

        @Override
        public void print( String s )
        {
            target.writeTestOutput( s == null ? "null" : s, false, isStdout );
        }

        @Override
        public PrintStream append( CharSequence csq )
        {
            print( csq == null ? "null" : csq.toString() );
            return this;
        }

        @Override
        public PrintStream append( CharSequence csq, int start, int end )
        {
            CharSequence s = csq == null ? "null" : csq;
            print( s.subSequence( start, end ).toString() );
            return this;
        }

        @Override
        public PrintStream append( char c )
        {
            print( c );
            return this;
        }

        @Override
        public void close()
        {
        }

        @Override
        public void flush()
        {
        }
    }

    private static final class NullOutputStream
            extends OutputStream
    {
        @Override
        public void write( int b )
        {
        }
    }
}
