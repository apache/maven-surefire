package org.apache.maven.surefire.booter;

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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunMode;

import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.util.internal.ObjectUtils.requireNonNull;

/**
 * Encodes the full output of the test run to the stdout stream.
 * <p/>
 * This class and the ForkClient contain the full definition of the
 * "wire-level" protocol used by the forked process. The protocol
 * is *not* part of any public api and may change without further
 * notice.
 * <p/>
 * This class is threadsafe.
 * <p/>
 * The synchronization in the underlying PrintStream (target instance)
 * is used to preserve thread safety of the output stream. To perform
 * multiple writes/prints for a single request, they must
 * synchronize on "target.out" variable in this class.
 *
 * @author Kristian Rosenvold
 */
public class ForkingRunListener
    implements RunListener, ConsoleLogger, ConsoleOutputReceiver, ConsoleStream
{
    private final ForkedChannelEncoder target;

    private final boolean trim;

    private volatile RunMode runMode = NORMAL_RUN;

    public ForkingRunListener( ForkedChannelEncoder target, boolean trim )
    {
        this.target = target;
        this.trim = trim;
        sendProps(); // todo really needed? - sent after every test class
    }

    public void testSetStarting( ReportEntry report )
    {
        target.testSetStarting( report, trim );
    }

    public void testSetCompleted( ReportEntry report )
    {
        target.testSetCompleted( report, trim );
    }

    public void testStarting( ReportEntry report )
    {
        target.testStarting( report, trim );
    }

    public void testSucceeded( ReportEntry report )
    {
        target.testSucceeded( report, trim );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        target.testAssumptionFailure( report, trim );
    }

    public void testError( ReportEntry report )
    {
        target.testError( report, trim );
    }

    public void testFailed( ReportEntry report )
    {
        target.testFailed( report, trim );
    }

    public void testSkipped( ReportEntry report )
    {
        target.testSkipped( report, trim );
    }

    public void testExecutionSkippedByUser()
    {
        target.stopOnNextTest();
    }

    public RunMode markAs( RunMode currentRunMode )
    {
        RunMode runMode = this.runMode;
        this.runMode = requireNonNull( currentRunMode );
        return runMode;
    }

    void sendProps()
    {
        target.sendSystemProperties();
    }

    public void writeTestOutput( String output, boolean stdout )
    {
        if ( stdout )
        {
            target.stdOut( output );
        }
        else
        {
            target.stdErr( output );
        }
    }

    public void debug( String message )
    {
        target.debug( message );
    }

    public void info( String message )
    {
        target.console( message );
    }

    public void warning( String message )
    {
        target.warning( message );
    }

    public void error( String message )
    {
        target.error( message );
    }

    public void error( String message, Throwable t )
    {
        target.error( message, t );
    }

    public void error( Throwable t )
    {
        error( null, t );
    }

    public void println( String message )
    {
        writeTestOutput( message, true );
    }
}
