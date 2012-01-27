package org.apache.maven.plugin.surefire.report;

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

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;

/**
 * Base class for console reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractConsoleReporter
    extends AbstractTextReporter
{
    private static final String TEST_SET_STARTING_PREFIX = "Running ";

    private static final String TEST_SET_STARTING_GROUP_PREFIX = " (of ";

    private static final String TEST_SET_STARTING_GROUP_SUFIX = ")";

    private static final int BUFFER_SIZE = 4096;

    private static final PrintStream ORIGINAL_SYSTEM_OUT = System.out;

    AbstractConsoleReporter( boolean trimStackTrace, String format )
    {
        super( getPrintWriter(), trimStackTrace, format );
    }

    private static PrintWriter getPrintWriter()
    {
        return new PrintWriter(
            new OutputStreamWriter( new BufferedOutputStream( ORIGINAL_SYSTEM_OUT, BUFFER_SIZE ) ) );
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        super.testSetStarting( report );

        writeMessage( getTestSetStartingMessage( report ) );
    }

    /**
     * Get the test set starting message for a report.
     * eg. "Running org.foo.BarTest ( of group )"
     *
     * @param report report whose test set is starting
     * @return the message
     */
    static String getTestSetStartingMessage( ReportEntry report )
    {
        StringBuffer message = new StringBuffer();
        message.append( TEST_SET_STARTING_PREFIX );
        message.append( report.getName() );

        if ( report.getGroup() != null && !report.getName().equals( report.getGroup() ) )
        {
            message.append( TEST_SET_STARTING_GROUP_PREFIX );
            message.append( report.getGroup() );
            message.append( TEST_SET_STARTING_GROUP_SUFIX );
        }

        message.append( "\n" );
        return message.toString();
    }

}
