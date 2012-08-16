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

import org.apache.maven.surefire.report.ReportEntry;

import java.util.List;

/**
 * A reporter that broadcasts to other reporters
 *
 * @author Kristian Rosenvold
 */
public class MulticastingReporter
    implements Reporter
{
    private final Reporter[] target;

    private final int size;

    public MulticastingReporter( List<Reporter> target )
    {
        size = target.size();
        this.target = target.toArray( new Reporter[target.size()] );
    }

    public void testSetStarting( ReportEntry report )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testSetStarting( report );
        }
    }

    public void testSetCompleted( ReportEntry report, TestSetStats testSetStats )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testSetCompleted( report, testSetStats );
        }
    }


    public void testStarting( ReportEntry report )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testStarting( report );
        }
    }

    public void testSucceeded( ReportEntry report, TestSetStats testSetStats )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testSucceeded( report, testSetStats );
        }
    }

    public void testError( ReportEntry report, String stdOut, String stdErr, TestSetStats testSetStats )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testError( report, stdOut, stdErr, testSetStats );
        }
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr, TestSetStats testSetStats )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testFailed( report, stdOut, stdErr, testSetStats );
        }
    }

    public void testSkipped( ReportEntry report, TestSetStats testSetStats )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].testSkipped( report, testSetStats );
        }
    }

    public void writeMessage( String message )
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].writeMessage( message );
        }
    }

    public void reset()
    {
        for ( int i = 0; i < size; i++ )
        {
            target[i].reset();
        }
    }

}
