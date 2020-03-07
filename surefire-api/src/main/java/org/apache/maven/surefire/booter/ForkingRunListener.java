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
import org.apache.maven.surefire.report.TestSetReportEntry;

import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static java.util.Objects.requireNonNull;

/**
 * Encodes the full output of the test run to the stdout stream.
 * <br>
 * This class and the ForkClient contain the full definition of the
 * "wire-level" protocol used by the forked process. The protocol
 * is *not* part of any public api and may change without further
 * notice.
 * <br>
 * This class is threadsafe.
 * <br>
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
    }

    @Override
    public void testSetStarting( TestSetReportEntry report )
    {
        target.testSetStarting( report, trim );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
        target.sendSystemProperties( report.getSystemProperties() );
        target.testSetCompleted( report, trim );
    }

    @Override
    public void testStarting( ReportEntry report )
    {
        target.testStarting( report, trim );
    }

    @Override
    public void testSucceeded( ReportEntry report )
    {
        target.testSucceeded( report, trim );
    }

    @Override
    public void testAssumptionFailure( ReportEntry report )
    {
        target.testAssumptionFailure( report, trim );
    }

    @Override
    public void testError( ReportEntry report )
    {
        target.testError( report, trim );
    }

    @Override
    public void testFailed( ReportEntry report )
    {
        target.testFailed( report, trim );
    }

    @Override
    public void testSkipped( ReportEntry report )
    {
        target.testSkipped( report, trim );
    }

    @Override
    public void testExecutionSkippedByUser()
    {
        target.stopOnNextTest();
    }

    @Override
    public RunMode markAs( RunMode currentRunMode )
    {
        RunMode runMode = this.runMode;
        this.runMode = requireNonNull( currentRunMode );
        return runMode;
    }

    @Override
    public void writeTestOutput( String output, boolean newLine, boolean stdout )
    {
        if ( stdout )
        {
            target.stdOut( output, newLine );
        }
        else
        {
            target.stdErr( output, newLine );
        }
    }

    @Override
    public boolean isDebugEnabled()
    {
        return true;
    }

    @Override
    public void debug( String message )
    {
        target.consoleDebugLog( message );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return true;
    }

    @Override
    public void info( String message )
    {
        target.consoleInfoLog( message );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return true;
    }

    @Override
    public void warning( String message )
    {
        target.consoleWarningLog( message );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return true;
    }

    @Override
    public void error( String message )
    {
        target.consoleErrorLog( message );
    }

    @Override
    public void error( String message, Throwable t )
    {
        target.consoleErrorLog( message, t );
    }

    @Override
    public void error( Throwable t )
    {
        error( null, t );
    }

    @Override
    public void println( String message )
    {
        writeTestOutput( message, true, true );
    }
}
