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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.maven.surefire.report.ConsoleOutputReceiver;

/**
 * A stream-like object that preserves ordering between stdout/stderr
 */
public final class LogicalStream
{
    private final Collection<Entry> output = new ConcurrentLinkedQueue<Entry>();

    static final class Entry
    {
        private final boolean stdout;
        private final String text;

        Entry( boolean stdout, String text )
        {
            this.stdout = stdout;
            this.text = text;
        }

        public void writeDetails( ConsoleOutputReceiver outputReceiver )
        {
            outputReceiver.writeTestOutput( text, stdout );
        }

        @Override
        public String toString()
        {
            return text;
        }

        public boolean isBlankLine()
        {
            return "\n".equals( toString() );
        }
    }

    public synchronized void write( boolean stdout, String text )
    {
        Entry entry = new Entry( stdout, text );
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
