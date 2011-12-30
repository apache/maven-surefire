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

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.util.internal.ByteBuffer;

/**
 * A stream-like object that preserves ordering between stdout/stderr
 */
public class LogicalStream
{
    private final List<Entry> output = new ArrayList<Entry>();

    class Entry
    {
        final boolean stdout;

        final byte[] b;

        final int off;

        final int len;

        Entry( boolean stdout, byte[] b, int off, int len )
        {
            this.stdout = stdout;
            this.b = ByteBuffer.copy( b, off, len );
            this.off = 0;
            this.len = len;
        }


        public void writeDetails( ConsoleOutputReceiver outputReceiver )
        {
            outputReceiver.writeTestOutput( b, off, len, stdout );
        }

        @Override
        public String toString()
        {
            return new String( b, off, len );
        }

        public boolean isBlankLine()
        {
            return "\n".equals( toString() );
        }
    }

    public synchronized void write( boolean stdout, byte b[], int off, int len )
    {
        Entry entry = new Entry( stdout, b, off, len );
        if ( !entry.isBlankLine() )
        {
            output.add( entry );
        }
    }

    public synchronized void writeDetails( ConsoleOutputReceiver outputReceiver )
    {
        for ( Entry entry : output )
        {
            entry.writeDetails( outputReceiver );
        }
    }


}
