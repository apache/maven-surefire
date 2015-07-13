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

import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.junitcore.pc.ParallelComputer;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunListener;

import java.util.List;

/**
 * Encapsulates access to JUnitCore
 *
 * @author Kristian Rosenvold
 */

class JUnitCoreWrapper
{
    public static void execute( ConsoleLogger logger, TestsToRun testsToRun, JUnitCoreParameters jUnitCoreParameters,
                                List<RunListener> listeners, Filter filter )
        throws TestSetFailedException
    {
        JUnitCore junitCore = createJUnitCore( listeners );
        if ( testsToRun.allowEagerReading() )
        {
            executeEager( logger, testsToRun, filter, jUnitCoreParameters, junitCore );
        }
        else
        {
            executeLazy( logger, testsToRun, filter, jUnitCoreParameters, junitCore );
        }
    }

    private static JUnitCore createJUnitCore( List<RunListener> listeners )
    {
        JUnitCore junitCore = new JUnitCore();
        for ( RunListener runListener : listeners )
        {
            junitCore.addListener( runListener );
        }
        return junitCore;
    }

    private static void executeEager( ConsoleLogger logger, TestsToRun testsToRun, Filter filter,
                                      JUnitCoreParameters jUnitCoreParameters, JUnitCore junitCore )
        throws TestSetFailedException
    {
        Class<?>[] tests = testsToRun.getLocatedClasses();
        Computer computer = createComputer( logger, jUnitCoreParameters );
        createRequestAndRun( filter, computer, junitCore, tests );
    }

    private static void executeLazy( ConsoleLogger logger, TestsToRun testsToRun, Filter filter,
                                     JUnitCoreParameters jUnitCoreParameters, JUnitCore junitCore )
        throws TestSetFailedException
    {
        // in order to support LazyTestsToRun, the iterator must be used
        for ( Class clazz : testsToRun )
        {
            Computer computer = createComputer( logger, jUnitCoreParameters );
            createRequestAndRun( filter, computer, junitCore, clazz );
        }
    }

    private static void createRequestAndRun( Filter filter, Computer computer, JUnitCore junitCore,
                                             Class<?>... classesToRun )
        throws TestSetFailedException
    {
        Request req = Request.classes( computer, classesToRun );
        if ( filter != null )
        {
            req = new FilteringRequest( req, filter );
            if ( req.getRunner() == null )
            {
                // nothing to run
                return;
            }
        }

        Result run = junitCore.run( req );
        JUnit4RunListener.rethrowAnyTestMechanismFailures( run );

        if ( computer instanceof ParallelComputer )
        {
            String timeoutMessage = ( (ParallelComputer) computer ).describeElapsedTimeout();
            if ( timeoutMessage.length() != 0 )
            {
                throw new TestSetFailedException( timeoutMessage );
            }
        }
    }

    private static Computer createComputer( ConsoleLogger logger, JUnitCoreParameters parameters )
        throws TestSetFailedException
    {
        return parameters.isNoThreading()
            ? Computer.serial()
            : new ParallelComputerBuilder( logger, parameters ).buildComputer();
    }

    private static class FilteringRequest
        extends Request
    {
        private Runner filteredRunner;

        public FilteringRequest( Request req, Filter filter )
        {
            try
            {
                Runner runner = req.getRunner();
                filter.apply( runner );
                filteredRunner = runner;
            }
            catch ( NoTestsRemainException e )
            {
                filteredRunner = null;
            }
        }

        @Override
        public Runner getRunner()
        {
            return filteredRunner;
        }
    }
}
