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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.junitcore.pc.ParallelComputer;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder;
import org.junit.Ignore;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.StoppedByUserException;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.createDescription;
import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.createIgnored;
import static org.apache.maven.surefire.common.junit4.JUnit4RunListener.rethrowAnyTestMechanismFailures;
import static org.junit.runner.Computer.serial;
import static org.junit.runner.Request.classes;

/**
 * Encapsulates access to JUnitCore
 *
 * @author Kristian Rosenvold
 */
final class JUnitCoreWrapper
{
    private final Notifier notifier;
    private final JUnitCoreParameters jUnitCoreParameters;
    private final ConsoleLogger consoleStream;

    JUnitCoreWrapper( Notifier notifier, JUnitCoreParameters jUnitCoreParameters, ConsoleLogger consoleStream )
    {
        this.notifier = notifier;
        this.jUnitCoreParameters = jUnitCoreParameters;
        this.consoleStream = consoleStream;
    }

    void execute( TestsToRun testsToRun, Filter filter )
        throws TestSetFailedException
    {
        execute( testsToRun, true, Collections.<RunListener>emptyList(), filter );
    }

    void execute( TestsToRun testsToRun, Collection<RunListener> listeners, Filter filter )
            throws TestSetFailedException
    {
        execute( testsToRun, false, listeners, filter );
    }

    private void execute( TestsToRun testsToRun, boolean useIterated, Collection<RunListener> listeners, Filter filter )
        throws TestSetFailedException
    {
        if ( testsToRun.allowEagerReading() )
        {
            executeEager( testsToRun, filter, listeners );
        }
        else
        {
            executeLazy( testsToRun, useIterated, filter, listeners );
        }
    }

    private JUnitCore createJUnitCore( Notifier notifier, Collection<RunListener> listeners )
    {
        JUnitCore junitCore = new JUnitCore();

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

    private void executeLazy( TestsToRun testsToRun, boolean useIterated, Filter filter,
                              Collection<RunListener> listeners )
        throws TestSetFailedException
    {
        JUnitCore junitCore = createJUnitCore( notifier, listeners );
        for ( Iterator<Class<?>> it = useIterated ? testsToRun.iterated() : testsToRun.iterator(); it.hasNext(); )
        {
            Class<?> clazz = it.next();
            Computer computer = createComputer();
            createRequestAndRun( filter, computer, junitCore.withReportedTests( clazz ), clazz );
        }
    }

    private void createRequestAndRun( Filter filter, Computer computer, JUnitCore junitCore, Class<?>... classesToRun )
        throws TestSetFailedException
    {
        Request req = classes( computer, classesToRun );
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
            if ( !timeoutMessage.isEmpty() )
            {
                throw new TestSetFailedException( timeoutMessage );
            }
        }
    }

    private Computer createComputer()
    {
        return jUnitCoreParameters.isNoThreading()
            ? serial()
            : new ParallelComputerBuilder( consoleStream, jUnitCoreParameters ).buildComputer();
    }

    private final class JUnitCore
        extends org.apache.maven.surefire.junitcore.JUnitCore
    {
        JUnitCore()
        {
            super( JUnitCoreWrapper.this.notifier );
        }

        JUnitCore withReportedTests( Class<?>... tests )
        {
            Queue<String> stoppedTests = JUnitCoreWrapper.this.notifier.getRemainingTestClasses();
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
            if ( JUnitCoreWrapper.this.notifier.isFailFast() && e instanceof StoppedByUserException )
            {
                Queue<String> stoppedTests = JUnitCoreWrapper.this.notifier.getRemainingTestClasses();
                if ( stoppedTests != null )
                {
                    String reason = e.getClass().getName();
                    Ignore reasonForSkippedTest = createIgnored( reason );
                    for ( String clazz; ( clazz = stoppedTests.poll() ) != null; )
                    {
                        Description skippedTest = createDescription( clazz, reasonForSkippedTest );
                        JUnitCoreWrapper.this.notifier.fireTestIgnored( skippedTest );
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
            Queue<String> stoppedTests = JUnitCoreWrapper.this.notifier.getRemainingTestClasses();
            if ( stoppedTests != null )
            {
                stoppedTests.clear();
            }
        }
    }
}
