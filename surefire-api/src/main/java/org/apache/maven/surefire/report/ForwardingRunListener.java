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

/**
 * Deals with system.out/err from single-threaded processes.
 * <p/>
 */
public abstract class ForwardingRunListener
    implements RunListener
{
    private final RunListener target;

    protected ForwardingRunListener( RunListener target )
    {
        this.target = target;
    }

    public void testSetStarting( ReportEntry report )
    {
        target.testSetStarting( report );
    }

    public void testSetCompleted( ReportEntry report )
    {
        target.testSetCompleted( report );
    }

    public void testStarting( ReportEntry report )
    {
        target.testStarting( report );
    }

    public void testSucceeded( ReportEntry report )
    {
        target.testSucceeded( report );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        target.testAssumptionFailure( report );
    }

    public void testError( ReportEntry report )
    {
        target.testError( report );
    }

    public void testFailed( ReportEntry report )
    {
        target.testFailed( report );
    }

    public void testSkipped( ReportEntry report )
    {
        target.testSkipped( report );
    }

}
