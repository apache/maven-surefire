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
 *
 * Also licensed under CPL http://junit.sourceforge.net/cpl-v10.html
 */

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * @author Kristian Rosenvold, kristianAzeniorD0Tno
 */
public class DiagnosticRunListener
    extends RunListener
{
    private final AtomicInteger numTestStarted = new AtomicInteger();

    private final AtomicInteger numTestFailed = new AtomicInteger();

    private final AtomicInteger numTestAssumptionsFailed = new AtomicInteger();

    private final AtomicInteger numTestFinished = new AtomicInteger();

    private final AtomicInteger numTestIgnored = new AtomicInteger();

    private final boolean printToConsole;

    private final RunListener target;


    private void print( String event, Description description )
    {
        if ( printToConsole )
        {
            System.out.println( Thread.currentThread().toString() + ", event = " + event + ", " + description );
        }
    }

    private void print( String event, Result description )
    {
        if ( printToConsole )
        {
            System.out.println( Thread.currentThread().toString() + ", event = " + event + ", " + description );
        }
    }

    private void print( String event, Failure description )
    {
        if ( printToConsole )
        {
            System.out.println( Thread.currentThread().toString() + ", event = " + event + ", " + description );
        }
    }

    public DiagnosticRunListener( boolean printToConsole, RunListener target )
    {
        this.printToConsole = printToConsole;
        this.target = target;
    }

    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
        print( "testRunStarted", description );
        if ( target != null )
        {
            target.testRunStarted( description );
        }
    }

    @Override
    public void testRunFinished( Result result )
        throws Exception
    {
        print( "testRunFinished", result );
        if ( target != null )
        {
            target.testRunFinished( result );
        }
    }

    @Override
    public void testStarted( Description description )
        throws Exception
    {
        numTestStarted.incrementAndGet();
        print( "testStarted", description );
        if ( target != null )
        {
            target.testStarted( description );
        }
    }

    @Override
    public void testFinished( Description description )
        throws Exception
    {
        numTestFinished.incrementAndGet();
        print( "testFinished", description );
        if ( target != null )
        {
            target.testFinished( description );
        }
    }

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        numTestFailed.incrementAndGet();
        print( "testFailure", failure );
        if ( target != null )
        {
            target.testFailure( failure );
        }
    }

    @Override
    public void testAssumptionFailure( Failure failure )
    {
        numTestAssumptionsFailed.incrementAndGet();
        print( "testAssumptionFailure", failure );
        if ( target != null )
        {
            target.testAssumptionFailure( failure );
        }
    }

    @Override
    public void testIgnored( Description description )
        throws Exception
    {
        numTestIgnored.incrementAndGet();
        print( "testIgnored", description );
        if ( target != null )
        {
            target.testIgnored( description );
        }
    }

    @Override
    public String toString()
    {
        return "DiagnosticRunListener{" + "numTestIgnored=" + numTestIgnored + ", numTestStarted=" + numTestStarted
            + ", numTestFailed=" + numTestFailed + ", numTestAssumptionsFailed=" + numTestAssumptionsFailed
            + ", numTestFinished=" + numTestFinished + '}';
    }
}
