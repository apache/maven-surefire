package org.apache.maven.surefire.junitcore;

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

import org.apache.maven.surefire.report.ConsoleOutputReceiver;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A stream-like object that preserves ordering between stdout/stderr
 */
public final class LogicalStream
{
    private final Queue<Entry> output = new ConcurrentLinkedQueue<>();

    private static final class Entry
    {
        private final boolean stdout;

        private final byte[] b;

        private final int off;

        private final int len;

        private Entry( boolean stdout, byte[] b, int off, int len )
        {
            this.stdout = stdout;
            this.b = Arrays.copyOfRange( b, off, off + len );
            this.off = 0;
            this.len = len;
        }

        private void writeDetails( ConsoleOutputReceiver outputReceiver )
        {
            outputReceiver.writeTestOutput( b, off, len, stdout );
        }
    }

    public void write( boolean stdout, byte b[], int off, int len )
    {
        if ( !isBlankLine( b, len ) )
        {
            Entry entry = new Entry( stdout, b, off, len );
            output.add( entry );
        }
    }

    public void writeDetails( ConsoleOutputReceiver outputReceiver )
    {
        for ( Entry entry = output.poll(); entry != null; entry = output.poll() )
        {
            entry.writeDetails( outputReceiver );
        }
    }

    private static boolean isBlankLine( byte[] b, int len )
    {
        return b == null || len == 0;
    }
}
