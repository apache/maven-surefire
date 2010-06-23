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

import org.apache.maven.surefire.report.ReporterManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A stream-like object that preserves ordering between stdout/stderr
 */
public class LogicalStream
{

    private final List<Entry> output = new ArrayList<Entry>();

    class Entry
    {
        final boolean stdout;

        final String value;

        Entry( boolean stdout, byte[] b, int off, int len )
        {
            this.stdout = stdout;
            value = new String( b, off, len ).intern();
        }

        public boolean isStdout()
        {
            return stdout;
        }

        public void writeTo( StringBuilder stringBuilder )
        {
            stringBuilder.append( value );
        }


        public void writeToConsole( ReporterManager reporter )
        {
            reporter.writeConsoleMessage( value );
        }

        @Override
        public String toString()
        {
            return value;
        }

        public boolean isBlankLine()
        {
            return "\n".equals( value );
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


    public void writeToConsole( ReporterManager reporter )
    {
        for ( Entry entry : output )
        {
            entry.writeToConsole( reporter );
        }
    }

    public String getOutput( boolean stdOut )
    {
        StringBuilder stringBuilder = new StringBuilder();
        for ( Entry entry : output )
        {
            if ( stdOut == entry.isStdout() )
            {
                entry.writeTo( stringBuilder );
            }
        }
        return stringBuilder.toString();
    }

}
