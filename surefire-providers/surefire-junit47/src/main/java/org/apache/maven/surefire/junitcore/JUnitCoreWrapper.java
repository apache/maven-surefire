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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.junitcore.pc.ParallelComputer;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;

import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunListener;

/**
 * Encapsulates access to JUnitCore
 *
 * @author Kristian Rosenvold
 */

class JUnitCoreWrapper
{
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

    public static void execute( TestsToRun testsToRun, JUnitCoreParameters jUnitCoreParameters,
                                List<RunListener> listeners, Filter filter )
        throws TestSetFailedException
    {
        ComputerWrapper computerWrapper = createComputer( jUnitCoreParameters );
        JUnitCore junitCore = createJUnitCore( listeners );
        if ( testsToRun.allowEagerReading() )
        {
            executeEager( testsToRun, filter, computerWrapper.getComputer(), junitCore );
        }
        else
        {
            exeuteLazy( testsToRun, filter, computerWrapper.getComputer(), junitCore );
        }

        String timeoutMessage = computerWrapper.describeElapsedTimeout();
        if ( timeoutMessage.length() != 0 )
        {
            throw new TestSetFailedException( timeoutMessage );
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

    private static void executeEager(TestsToRun testsToRun, Filter filter, Computer computer, JUnitCore junitCore)
            throws TestSetFailedException 
    {
        Class[] tests = testsToRun.getLocatedClasses();
        createRequestAndRun( filter, computer, junitCore, tests );
    }

    private static void exeuteLazy(TestsToRun testsToRun, Filter filter, Computer computer, JUnitCore junitCore)
            throws TestSetFailedException
    {
        // in order to support LazyTestsToRun, the iterator must be used
        for ( Class clazz : testsToRun )
        {
            createRequestAndRun( filter, computer, junitCore, clazz );
        }
    }

    private static void createRequestAndRun( Filter filter, Computer computer, JUnitCore junitCore, Class<?>... classesToRun )
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

        final Result run = junitCore.run( req );
        JUnit4RunListener.rethrowAnyTestMechanismFailures( run );
    }

    private static ComputerWrapper createComputer( JUnitCoreParameters parameters )
        throws TestSetFailedException
    {
        return parameters.isNoThreading() ? new ComputerWrapper( Computer.serial() ) : createParallelComputer( parameters );
    }

    private static ComputerWrapper createParallelComputer( JUnitCoreParameters parameters )
            throws TestSetFailedException
    {
        ParallelComputer pc = ParallelComputerFactory.createParallelComputer( parameters );

        int timeout = parameters.getParallelTestsTimeoutInSeconds();

        int timeoutForced = parameters.getParallelTestsTimeoutForcedInSeconds();

        Future<Collection<Description>> testsBeforeShutdown =
                timeout > 0 ? pc.scheduleShutdown( timeout, TimeUnit.SECONDS ) : null;

        Future<Collection<Description>> testsBeforeForcedShutdown =
                timeoutForced > 0 ? pc.scheduleForcedShutdown( timeoutForced, TimeUnit.SECONDS ) : null;

        return new ComputerWrapper( pc, timeout, testsBeforeShutdown, timeoutForced, testsBeforeForcedShutdown );
    }

    private static class ComputerWrapper
    {
        private final Computer computer;
        private final int timeout;
        private final int timeoutForced;
        private final Future<Collection<Description>> testsBeforeShutdown;
        private final Future<Collection<Description>> testsBeforeForcedShutdown;

        ComputerWrapper( Computer computer )
        {
            this( computer, 0, null, 0, null );
        }

        ComputerWrapper( Computer computer,
                         int timeout, Future<Collection<Description>> testsBeforeShutdown,
                         int timeoutForced, Future<Collection<Description>> testsBeforeForcedShutdown )
        {
            this.computer = computer;
            this.timeout = timeout;
            this.testsBeforeShutdown = testsBeforeShutdown;
            this.timeoutForced = timeoutForced;
            this.testsBeforeForcedShutdown = testsBeforeForcedShutdown;
        }

        Computer getComputer()
        {
            return computer;
        }

        String describeElapsedTimeout() throws TestSetFailedException
        {
            TreeSet<String> executedTests = new TreeSet<String>();
            if ( timeout > 0 )
            {
                executedTests.addAll( printShutdownHook( testsBeforeShutdown ) );
            }

            if ( timeoutForced > 0 )
            {
                executedTests.addAll( printShutdownHook( testsBeforeForcedShutdown ) );
            }

            StringBuilder msg = new StringBuilder();
            if ( !executedTests.isEmpty() )
            {
                msg.append( "The test run has finished abruptly after timeout of " );
                msg.append( Math.min( timeout, timeoutForced ) );
                msg.append( " seconds.\n" );
                msg.append( "These tests were executed in prior of the shutdown operation:\n" );
                for ( String executedTest : executedTests )
                {
                    msg.append( executedTest ).append( "\n" );
                }
            }
            return msg.toString();
        }

        static Collection<String> printShutdownHook( Future<Collection<Description>> future )
                throws TestSetFailedException
        {
            if ( !future.isCancelled() && future.isDone() )
            {
                try
                {
                    TreeSet<String> executedTests = new TreeSet<String>();
                    for ( Description executedTest : future.get() )
                    {
                        if ( executedTest != null && executedTest.getDisplayName() != null )
                        {
                            executedTests.add( executedTest.getDisplayName() );
                        }
                    }
                    return executedTests;
                }
                catch ( Exception e )
                {
                    throw new TestSetFailedException( e );
                }
            }
            return Collections.emptySet();
        }
    }
}
