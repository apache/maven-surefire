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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static java.lang.System.setErr;
import static java.lang.System.setOut;
import static org.apache.maven.surefire.util.internal.StringUtils.NL;

/**
 * Deals with system.out/err.
 * <br>
 */
public class ConsoleOutputCapture
{
    public static void startCapture( ConsoleOutputReceiver target )
    {
        setOut( new ForwardingPrintStream( true, target ) );
        setErr( new ForwardingPrintStream( false, target ) );
    }

    private static class ForwardingPrintStream
        extends PrintStream
    {
        private final boolean isStdout;
        private final ConsoleOutputReceiver target;

        ForwardingPrintStream( boolean stdout, ConsoleOutputReceiver target )
        {
            super( new ByteArrayOutputStream() );
            isStdout = stdout;
            this.target = target;
        }

        @Override
        public void write( byte[] buf, int off, int len )
        {
            // Note: At this point the supplied "buf" instance is reused, which means
            // data must be copied out of the buffer
            target.writeTestOutput( buf, off, len, isStdout );
        }

        @Override
        public void write( byte[] b )
            throws IOException
        {
            target.writeTestOutput( b, 0, b.length, isStdout );
        }

        @Override
        public void write( int b )
        {
            byte[] buf = new byte[1];
            buf[0] = (byte) b;
            try
            {
                write( buf );
            }
            catch ( IOException e )
            {
                setError();
            }
        }

        @Override
        public void println( String s )
        {
            if ( s == null )
            {
                s = "null"; // Shamelessly taken from super.print
            }
            final byte[] bytes = ( s + NL ).getBytes();
            target.writeTestOutput( bytes, 0, bytes.length, isStdout );
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

}
