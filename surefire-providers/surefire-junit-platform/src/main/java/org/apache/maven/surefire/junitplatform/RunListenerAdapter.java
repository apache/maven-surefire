package org.apache.maven.surefire.junitplatform;

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

import static org.apache.maven.surefire.report.SimpleReportEntry.ignored;
import static org.apache.maven.surefire.report.SimpleReportEntry.withException;
import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractClassName;
import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractMethodName;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 2.22.0
 */
final class RunListenerAdapter
    implements TestExecutionListener
{
    private final RunListener runListener;

    private final Set<TestIdentifier> testSetNodes = ConcurrentHashMap.newKeySet();

    private volatile TestPlan testPlan;

    RunListenerAdapter( RunListener runListener )
    {
        this.runListener = runListener;
    }

    @Override
    public void testPlanExecutionStarted( TestPlan testPlan )
    {
        updateTestPlan( testPlan );
    }

    @Override
    public void testPlanExecutionFinished( TestPlan testPlan )
    {
        updateTestPlan( null );
    }

    @Override
    public void executionStarted( TestIdentifier testIdentifier )
    {
        if ( testIdentifier.isContainer()
                        && testIdentifier.getSource().filter( ClassSource.class::isInstance ).isPresent() )
        {
            startTestSetIfPossible( testIdentifier );
        }
        if ( testIdentifier.isTest() )
        {
            ensureTestSetStarted( testIdentifier );
            runListener.testStarting( createReportEntry( testIdentifier ) );
        }
    }

    @Override
    public void executionSkipped( TestIdentifier testIdentifier, String reason )
    {
        ensureTestSetStarted( testIdentifier );
        String[] classMethodName = toClassMethodName( testIdentifier );
        String className = classMethodName[0];
        String methodName = classMethodName[1];
        runListener.testSkipped( ignored( className, methodName, reason ) );
        completeTestSetIfNecessary( testIdentifier );
    }

    @Override
    public void executionFinished( TestIdentifier testIdentifier, TestExecutionResult testExecutionResult )
    {
        if ( testExecutionResult.getStatus() == ABORTED )
        {
            runListener.testAssumptionFailure( createReportEntry( testIdentifier, testExecutionResult ) );
        }
        else if ( testExecutionResult.getStatus() == FAILED )
        {
            reportFailedTest( testIdentifier, testExecutionResult );
        }
        else if ( testIdentifier.isTest() )
        {
            runListener.testSucceeded( createReportEntry( testIdentifier ) );
        }
        completeTestSetIfNecessary( testIdentifier );
    }

    private void updateTestPlan( TestPlan testPlan )
    {
        this.testPlan = testPlan;
        testSetNodes.clear();
    }

    private void ensureTestSetStarted( TestIdentifier testIdentifier )
    {
        if ( !isTestSetStarted( testIdentifier ) )
        {
            startTestSet( testIdentifier.isTest()
                    ? testPlan.getParent( testIdentifier ).orElse( testIdentifier )
                    : testIdentifier );
        }
    }

    private boolean isTestSetStarted( TestIdentifier testIdentifier )
    {
        return testSetNodes.contains( testIdentifier )
                        || testPlan.getParent( testIdentifier ).map( this::isTestSetStarted ).orElse( false );
    }

    private void startTestSetIfPossible( TestIdentifier testIdentifier )
    {
        if ( !isTestSetStarted( testIdentifier ) )
        {
            startTestSet( testIdentifier );
        }
    }

    private void completeTestSetIfNecessary( TestIdentifier testIdentifier )
    {
        if ( testSetNodes.contains( testIdentifier ) )
        {
            completeTestSet( testIdentifier );
        }
    }

    private void startTestSet( TestIdentifier testIdentifier )
    {
        runListener.testSetStarting( createTestSetReportEntry( testIdentifier ) );
        testSetNodes.add( testIdentifier );
    }

    private void completeTestSet( TestIdentifier testIdentifier )
    {
        runListener.testSetCompleted( createTestSetReportEntry( testIdentifier ) );
        testSetNodes.remove( testIdentifier );
    }

    private void reportFailedTest(
                    TestIdentifier testIdentifier, TestExecutionResult testExecutionResult )
    {
        SimpleReportEntry reportEntry = createReportEntry( testIdentifier, testExecutionResult );
        if ( testExecutionResult.getThrowable().filter( AssertionError.class::isInstance ).isPresent() )
        {
            runListener.testFailed( reportEntry );
        }
        else
        {
            runListener.testError( reportEntry );
        }
    }

    private SimpleReportEntry createTestSetReportEntry( TestIdentifier testIdentifier )
    {
        String[] classMethodName = toClassMethodName( testIdentifier );
        String className = classMethodName[0];
        String methodName = classMethodName[1];
        return new SimpleReportEntry( className, methodName );
    }

    private SimpleReportEntry createReportEntry( TestIdentifier testIdentifier )
    {
        return createReportEntry( testIdentifier, (StackTraceWriter) null );
    }

    private SimpleReportEntry createReportEntry( TestIdentifier testIdentifier,
                                                 TestExecutionResult testExecutionResult )
    {
        return createReportEntry( testIdentifier, toStackTraceWriter( testIdentifier, testExecutionResult ) );
    }

    private SimpleReportEntry createReportEntry( TestIdentifier testIdentifier, StackTraceWriter stackTraceWriter )
    {
        String[] classMethodName = toClassMethodName( testIdentifier );
        String className = classMethodName[0];
        String methodName = classMethodName[1];
        return withException( className, methodName, stackTraceWriter );
    }

    private StackTraceWriter toStackTraceWriter( TestIdentifier testIdentifier,
                                                 TestExecutionResult testExecutionResult )
    {
        Optional<Throwable> throwable = testExecutionResult.getThrowable();
        if ( testExecutionResult.getStatus() == FAILED )
        {
            // Failed tests must have a StackTraceWriter, otherwise Surefire will fail
            return toStackTraceWriter( testIdentifier, throwable.orElse( null ) );
        }
        return throwable.map( t -> toStackTraceWriter( testIdentifier, t ) )
                .orElse( null );
    }

    private StackTraceWriter toStackTraceWriter( TestIdentifier testIdentifier, Throwable throwable )
    {
        String[] classMethodName = toClassMethodName( testIdentifier );
        String className = classMethodName[0];
        String methodName = classMethodName[1];
        return new PojoStackTraceWriter( className, methodName, throwable );
    }

    private String[] toClassMethodName( TestIdentifier testIdentifier )
    {
        String methodDisplayName = testIdentifier.getDisplayName();
        TestSource testSource = testIdentifier.getSource().orElse( null );
        if ( testSource instanceof ClassSource )
        {
            ClassSource classSource = (ClassSource) testSource;
            return new String[] { classSource.getClassName(), methodDisplayName };
        }
        else if ( testSource instanceof MethodSource )
        {
            MethodSource methodSource = (MethodSource) testSource;
            return new String[] { methodSource.getClassName(), methodDisplayName };
        }
        else
        {
            String description = testIdentifier.getLegacyReportingName();
            return testPlan.getParent( testIdentifier )
                    .map( this::toClassMethodName )
                    .orElse( new String[] { extractClassName( description ), extractMethodName( description ) } );
        }
    }
}
