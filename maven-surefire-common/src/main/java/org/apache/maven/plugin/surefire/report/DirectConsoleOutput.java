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

import java.io.PrintStream;
import org.apache.maven.surefire.report.ReportEntry;

/**
 * Outputs test system out/system err directly to the console
 * <p/>
 * Just a step on the road to getting the separation of reporting concerns
 * operating properly.
 *
 * @author Kristian Rosenvold
 */
public class DirectConsoleOutput
    implements TestcycleConsoleOutputReceiver
{
    private final PrintStream sout;

    private final PrintStream serr;

    public DirectConsoleOutput( PrintStream sout, PrintStream serr )
    {
        this.sout = sout;
        this.serr = serr;
    }


    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        PrintStream stream = stdout ? sout : serr;
        stream.write( buf, off, len );
    }

    public void testSetStarting( ReportEntry reportEntry )
    {
    }

    public void testSetCompleted( ReportEntry report )
    {
    }

    public void close()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
