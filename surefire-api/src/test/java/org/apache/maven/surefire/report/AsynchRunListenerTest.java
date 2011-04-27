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

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class AsynchRunListenerTest
    extends TestCase
{

    class MockConsoleOutputReceiver
        implements ConsoleOutputReceiver
    {
        byte[] buf;

        int off;

        int len;

        boolean stdout;

        public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
        {
            this.buf = buf;
            this.off = off;
            this.len = len;
            this.stdout = stdout;
        }

        public byte[] getBuf()
        {
            return buf;
        }

        public int getLen()
        {
            return len;
        }
    }

    public void testCombiner()
    {
        final MockConsoleOutputReceiver consoleOutputReceiver = new MockConsoleOutputReceiver();
        AsynchRunListener.JoinableTestOutput joinableTestOutput =
            new AsynchRunListener.JoinableTestOutput( "ABC".getBytes(), 0, 3, true, consoleOutputReceiver );
        AsynchRunListener.JoinableTestOutput joinableTestOutput2 =
            new AsynchRunListener.JoinableTestOutput( "DEF".getBytes(), 0, 3, true, consoleOutputReceiver );

        final AsynchRunListener.JoinableTestOutput append = joinableTestOutput.append( joinableTestOutput2 );

        append.run();
        final byte[] expected = "ABCDEF".getBytes();
        for ( int i = 0; i < expected.length; i++ )
        {
            assertEquals( expected[i], consoleOutputReceiver.getBuf()[i] );
        }
        assertEquals( expected.length, consoleOutputReceiver.getLen() );
    }
}
