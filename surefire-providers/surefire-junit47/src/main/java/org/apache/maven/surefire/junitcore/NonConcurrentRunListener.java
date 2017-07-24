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
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.TestSetReportEntry;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.Collections;
import java.util.Map;

import static org.apache.maven.surefire.util.internal.ObjectUtils.systemProps;

/**
 * A class to be used when there is no JUnit parallelism (methods or/and class). This allow to workaround JUnit
 * limitation a la Junit4 provider. Specifically, we can redirect properly the output even if we don't have class
 * demarcation in JUnit. It works when if there is a JVM instance per test run, i.e. with forkMode=always or perthread.
 */
public class NonConcurrentRunListener
    extends JUnit4RunListener
    implements ConsoleOutputReceiver
{
    private Description currentTestSetDescription;

    private Description lastFinishedDescription;

    public NonConcurrentRunListener( RunListener reporter )
        throws TestSetFailedException
    {
        super( reporter );
    }

    @Override
    public synchronized void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        // We can write immediately: no parallelism and a single class.
        ( (ConsoleOutputReceiver) reporter ).writeTestOutput( buf, off, len, stdout );
    }

    @Override
    protected SimpleReportEntry createReportEntry( Description description )
    {
        return new SimpleReportEntry( extractDescriptionClassName( description ), description.getDisplayName() );
    }

    private TestSetReportEntry createReportEntryForTestSet( Description description, Map<String, String> systemProps )
    {
        String testClassName = extractDescriptionClassName( description );
        return new SimpleReportEntry( testClassName, testClassName, systemProps );
    }

    private TestSetReportEntry createTestSetReportEntryStarted( Description description )
    {
        return createReportEntryForTestSet( description, Collections.<String, String>emptyMap() );
    }

    private TestSetReportEntry createTestSetReportEntryFinished( Description description )
    {
        return createReportEntryForTestSet( description, systemProps() );
    }

    @Override
    protected String extractDescriptionClassName( Description description )
    {
        return description.getClassName();
    }

    @Override
    protected String extractDescriptionMethodName( Description description )
    {
        return description.getMethodName();
    }

    @Override
    public void testStarted( Description description )
        throws Exception
    {
        finishLastTestSetIfNecessary( description );
        super.testStarted( description );
    }

    private void finishLastTestSetIfNecessary( Description description )
    {
        if ( describesNewTestSet( description ) )
        {
            currentTestSetDescription = description;
            if ( lastFinishedDescription != null )
            {
                TestSetReportEntry reportEntry = createTestSetReportEntryFinished( lastFinishedDescription );
                reporter.testSetCompleted( reportEntry );
                lastFinishedDescription = null;
            }
            reporter.testSetStarting( createTestSetReportEntryStarted( description ) );
        }
    }

    private boolean describesNewTestSet( Description description )
    {
        if ( currentTestSetDescription != null )
        {
            if ( null != description.getTestClass() )
            {
                return !description.getTestClass().equals( currentTestSetDescription.getTestClass() );
            }
            else if ( description.isSuite() )
            {
                return description.getChildren().equals( currentTestSetDescription.getChildren() );
            }

            return false;
        }

        return true;
    }

    @Override
    public void testFinished( Description description )
        throws Exception
    {
        super.testFinished( description );
        lastFinishedDescription = description;
    }

    @Override
    public void testIgnored( Description description )
        throws Exception
    {
        finishLastTestSetIfNecessary( description );

        super.testIgnored( description );
        lastFinishedDescription = description;
    }

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        finishLastTestSetIfNecessary( failure.getDescription() );

        super.testFailure( failure );
        lastFinishedDescription = failure.getDescription();
    }

    @Override
    public void testAssumptionFailure( Failure failure )
    {
        super.testAssumptionFailure( failure );
        lastFinishedDescription = failure.getDescription();
    }

    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
    }

    @Override
    public void testRunFinished( Result result )
        throws Exception
    {
        if ( lastFinishedDescription != null )
        {
            reporter.testSetCompleted( createTestSetReportEntryFinished( lastFinishedDescription ) );
            lastFinishedDescription = null;
        }
    }
}
