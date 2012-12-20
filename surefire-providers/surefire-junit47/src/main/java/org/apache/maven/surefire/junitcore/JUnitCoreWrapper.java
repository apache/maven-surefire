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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;

import org.junit.experimental.ParallelComputer;
import org.junit.runner.Computer;
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
        Computer computer = getComputer( jUnitCoreParameters );

        JUnitCore junitCore = createJUnitCore( listeners );

        try
        {
            if ( testsToRun.allowEagerReading() )
            {
                executeEager( testsToRun, filter, computer, junitCore );
            }
            else
            {
                exeuteLazy( testsToRun, filter, computer, junitCore );
            }
        }
        finally
        {
            closeIfConfigurable( computer );
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
        createReqestAndRun( filter, computer, junitCore, tests );
    }

    private static void exeuteLazy(TestsToRun testsToRun, Filter filter, Computer computer, JUnitCore junitCore)
            throws TestSetFailedException
    {
        // in order to support LazyTestsToRun, the iterator must be used
        Iterator<?> classIter = testsToRun.iterator();
        while ( classIter.hasNext() )
        {
            createReqestAndRun( filter, computer, junitCore, new Class[]{ (Class<?>) classIter.next() } );
        }
    }

    private static void createReqestAndRun( Filter filter, Computer computer, JUnitCore junitCore, Class<?>[] classesToRun )
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

        final Result run = junitCore.run(req);
        JUnit4RunListener.rethrowAnyTestMechanismFailures(run);
    }

    private static void closeIfConfigurable( Computer computer )
        throws TestSetFailedException
    {
        if ( computer instanceof ConfigurableParallelComputer )
        {
            try
            {
                ( (ConfigurableParallelComputer) computer ).close();
            }
            catch ( ExecutionException e )
            {
                throw new TestSetFailedException( e );
            }
        }
    }

    private static Computer getComputer( JUnitCoreParameters jUnitCoreParameters )
        throws TestSetFailedException
    {
        if ( jUnitCoreParameters.isNoThreading() )
        {
            return new Computer();
        }
        return getConfigurableParallelComputer( jUnitCoreParameters );
    }

    private static Computer getConfigurableParallelComputer( JUnitCoreParameters jUnitCoreParameters )
        throws TestSetFailedException
    {
        if ( jUnitCoreParameters.isUseUnlimitedThreads() )
        {
            return new ConfigurableParallelComputer();
        }
        else
        {
            if ( JUnitUtils.isCompatibleVersionWith(4, 12) )
            {
                return createJUnitParallelComputer( jUnitCoreParameters );
            }
            else
            {
                return new ConfigurableParallelComputer(
                        jUnitCoreParameters.isParallelClasses() | jUnitCoreParameters.isParallelBoth(),
                        jUnitCoreParameters.isParallelMethod() | jUnitCoreParameters.isParallelBoth(),
                        jUnitCoreParameters.getThreadCount(), jUnitCoreParameters.isPerCoreThreadCount() );
            }
        }
    }

    private static Computer createJUnitParallelComputer( JUnitCoreParameters jUnitCoreParameters )
    {
        if ( jUnitCoreParameters.isParallelClasses() )
        {
            return jUnitCoreParameters.isUseUnlimitedThreads() ? ParallelComputer.classes()
                    : ParallelComputer.classes( createFixedPool( jUnitCoreParameters ) );
        }
        else if ( jUnitCoreParameters.isParallelMethod() )
        {
            return jUnitCoreParameters.isUseUnlimitedThreads() ? ParallelComputer.methods()
                    : ParallelComputer.methods( createFixedPool( jUnitCoreParameters ) );
        }
        else if ( jUnitCoreParameters.isParallelBoth() )
        {
            int totalThreads = countAllThreads( jUnitCoreParameters );
            // minConcurrentMethods should come from JUnitCoreParameters if specified, otherwise 50% of all threads
            int minConcurrentMethods = (int) StrictMath.ceil( (double) totalThreads / 2 );
            return jUnitCoreParameters.isUseUnlimitedThreads() ? ParallelComputer.classesAndMethodsUnbounded()
                    : ParallelComputer.classesAndMethods( createFixedPool( jUnitCoreParameters ), minConcurrentMethods );
        }
        else
        {
            return new Computer();
        }
    }

    private static ThreadPoolExecutor createFixedPool( JUnitCoreParameters jUnitCoreParameters )
    {
        int numberOfThreads = countAllThreads( jUnitCoreParameters );
        return new ThreadPoolExecutor( numberOfThreads, numberOfThreads, 0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>() );
    }

    private static int countAllThreads( JUnitCoreParameters jUnitCoreParameters )
    {
        int numberOfThreads = jUnitCoreParameters.getThreadCount();

        if ( jUnitCoreParameters.isPerCoreThreadCount() )
        {
            numberOfThreads *= Runtime.getRuntime().availableProcessors();
        }

        return numberOfThreads;
    }
}
