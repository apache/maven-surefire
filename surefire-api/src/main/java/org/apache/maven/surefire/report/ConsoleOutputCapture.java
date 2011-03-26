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

/**
 * Deals with system.out/err.
 * <p/>
 */
public class ConsoleOutputCapture
{

    private static final PrintStream oldOut = System.out;

    private static final PrintStream oldErr = System.err;

    public ConsoleOutputCapture( ConsoleOutputReceiver target )
    {
        System.setOut( new ForwardingPrintStream( true, target ) );

        System.setErr( new ForwardingPrintStream( false, target ) );
    }

    public void restoreStreams()
    {
        System.setOut( oldOut );
        System.setErr( oldErr );
    }

    static class ForwardingPrintStream
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

        public void write( byte[] buf, int off, int len )
        {
            target.writeTestOutput( buf, off, len, isStdout );
        }

        public void write( byte[] b )
            throws IOException
        {
            target.writeTestOutput( b, 0, b.length, isStdout );
        }

        public void close()
        {
        }

        public void flush()
        {
        }
    }

}
