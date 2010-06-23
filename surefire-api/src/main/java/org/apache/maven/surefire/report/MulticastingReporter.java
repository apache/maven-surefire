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

import java.util.Iterator;
import java.util.List;

/**
 * A reporter that broadcasts to other reporters
 *
 * @author Kristian Rosenvold
 */
public class MulticastingReporter
    implements Reporter
{
    private final List target;

    public MulticastingReporter( List target )
    {
        this.target = target;
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).testSetStarting( report );
        }
    }

    public void testSetCompleted( ReportEntry report )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            try
            {
                ( (Reporter) it.next() ).testSetCompleted( report );
            }
            catch ( ReporterException e )
            {
                // Added in commit r331325 in ReporterManager. This smells fishy. What's this about ?
            }

        }
    }


    public void runStarting()
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).runStarting();
        }
    }

    public void runCompleted()
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).runCompleted();
        }
    }


    public void testStarting( ReportEntry report )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).testStarting( report );
        }
    }

    public void testSucceeded( ReportEntry report )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).testSucceeded( report );
        }
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).testError( report, stdOut, stdErr );
        }
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).testFailed( report, stdOut, stdErr );
        }
    }

    public void testSkipped( ReportEntry report )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).testSkipped( report );
        }
    }

    public void writeConsoleMessage( String message )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            Reporter reporter = ( (Reporter) it.next() );
            // Todo: Really need to find out how the surefire4.x provider manages to avoid printing to this one.
            if (!(reporter instanceof BriefFileReporter)){
               reporter.writeMessage( message);
            }
        }
    }

    public void writeMessage( String message )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).writeMessage( message );
        }
    }

    public void writeFooter( String footer )
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).writeFooter( footer );
        }
    }

    public void reset()
    {
        for ( Iterator it = target.iterator(); it.hasNext(); )
        {
            ( (Reporter) it.next() ).reset();
        }
    }
}
