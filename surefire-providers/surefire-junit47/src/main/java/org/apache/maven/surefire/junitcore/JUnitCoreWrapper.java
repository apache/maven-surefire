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

import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.junitcore.pc.ParallelComputer;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.Ignore;
import org.junit.runner.Computer;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.StoppedByUserException;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Random;

import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.createDescription;
import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.createIgnored;
import static org.apache.maven.surefire.common.junit4.JUnit4RunListener.rethrowAnyTestMechanismFailures;

/**
 * Encapsulates access to JUnitCore
 *
 * @author Kristian Rosenvold
 */
final class JUnitCoreWrapper
{
    private final Notifier notifier;
    private final JUnitCoreParameters jUnitCoreParameters;
    private final ConsoleLogger logger;
    private final boolean failFast;

    private Object o = new Random().nextInt();

    JUnitCoreWrapper( Notifier notifier, JUnitCoreParameters jUnitCoreParameters, ConsoleLogger logger,
                      boolean failFast )
    {
        this.notifier = notifier;
        this.jUnitCoreParameters = jUnitCoreParameters;
        this.logger = logger;
        this.failFast = failFast;
    }

    void execute( TestsToRun testsToRun, Filter filter )
        throws TestSetFailedException
    {
        execute( testsToRun, Collections.<RunListener>emptyList(), filter );
    }

    void execute( TestsToRun testsToRun, Collection<RunListener> listeners, Filter filter )
        throws TestSetFailedException
    {
        if ( testsToRun.allowEagerReading() )
        {
            executeEager( testsToRun, filter, listeners );
        }
        else
        {
            executeLazy( testsToRun, filter, listeners );
        }
    }

    private JUnitCore createJUnitCore( final Notifier notifier, Collection<RunListener> listeners )
    {
        JUnitCore junitCore = new JUnitCore( notifier );

        // custom listeners added last
        notifier.addListeners( listeners );

        return junitCore;
    }

    private void executeEager( TestsToRun testsToRun, Filter filter, Collection<RunListener> listeners )
        throws TestSetFailedException
    {
        JUnitCore junitCore = createJUnitCore( notifier, listeners );
        Class<?>[] tests = testsToRun.getLocatedClasses();
        Computer computer = createComputer();
        createRequestAndRun( filter, computer, junitCore.withReportedTests( tests ), tests );
    }

    private void executeLazy( TestsToRun testsToRun, Filter filter, Collection<RunListener> listeners )
        throws TestSetFailedException
    {
        JUnitCore junitCore = createJUnitCore( notifier, listeners );
        // in order to support LazyTestsToRun, the iterator must be used
        for ( Class<?> clazz : testsToRun )
        {
            Computer computer = createComputer();
            createRequestAndRun( filter, computer, junitCore.withReportedTests( clazz ), clazz );
        }
    }

    private void createRequestAndRun( Filter filter, Computer computer, JUnitCore junitCore, Class<?>... classesToRun )
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

        Result run = junitCore.run( req.getRunner() );
        rethrowAnyTestMechanismFailures( run );

        if ( computer instanceof ParallelComputer )
        {
            String timeoutMessage = ( (ParallelComputer) computer ).describeElapsedTimeout();
            if ( timeoutMessage.length() != 0 )
            {
                throw new TestSetFailedException( timeoutMessage );
            }
        }
    }

    private Computer createComputer()
    {
        return jUnitCoreParameters.isNoThreading()
            ? Computer.serial()
            : new ParallelComputerBuilder( logger, jUnitCoreParameters ).buildComputer();
    }

    private final class JUnitCore
        extends org.apache.maven.surefire.junitcore.JUnitCore
    {
        private final JUnit47FailFastListener failFastListener;

        JUnitCore( Notifier notifier )
        {
            super( notifier );
            failFastListener = failFast ? new JUnit47FailFastListener( this ) : null;
            if ( failFastListener != null )
            {
                notifier.addListener( failFastListener );
            }
        }

        JUnitCore withReportedTests( Class<?>... tests )
        {
            Queue<String> stoppedTests = getRemainingTestClasses();
            if ( stoppedTests != null )
            {
                for ( Class<?> test : tests )
                {
                    stoppedTests.add( test.getName() );
                }
            }
            return this;
        }

        @Override
        @SuppressWarnings( "checkstyle:innerassignment" )
        protected void afterException( Throwable e )
            throws TestSetFailedException
        {
            if ( failFast && e instanceof StoppedByUserException )
            {
                Queue<String> stoppedTests = getRemainingTestClasses();
                if ( stoppedTests != null )
                {
                    String reason = e.getClass().getName();
                    Ignore reasonForIgnoredTest = createIgnored( reason );
                    for ( String clazz; ( clazz = stoppedTests.poll() ) != null; )
                    {
                        notifier.fireTestIgnored( createDescription( clazz, reasonForIgnoredTest ) );
                    }
                }
            }
            else
            {
                super.afterException( e );
            }
        }

        @Override
        protected void afterFinished()
        {
            Queue<String> stoppedTests = getRemainingTestClasses();
            if ( stoppedTests != null )
            {
                stoppedTests.clear();
            }
        }

        private Queue<String> getRemainingTestClasses()
        {
            return failFastListener == null ? null : failFastListener.getRemainingTestClasses();
        }
    }
}
