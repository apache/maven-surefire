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

import static java.util.Collections.emptyMap;
import static org.apache.maven.surefire.util.internal.ObjectUtils.systemProps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private final ConcurrentMap<TestIdentifier, Long> testStartTime = new ConcurrentHashMap<>();
    private final RunListener runListener;
    private volatile TestPlan testPlan;

    RunListenerAdapter( RunListener runListener )
    {
        this.runListener = runListener;
    }

    @Override
    public void testPlanExecutionStarted( TestPlan testPlan )
    {
        this.testPlan = testPlan;
    }

    @Override
    public void testPlanExecutionFinished( TestPlan testPlan )
    {
        this.testPlan = null;
        testStartTime.clear();
    }

    @Override
    public void executionStarted( TestIdentifier testIdentifier )
    {
        if ( testIdentifier.isContainer()
                        && testIdentifier.getSource().filter( ClassSource.class::isInstance ).isPresent() )
        {
            testStartTime.put( testIdentifier, System.currentTimeMillis() );
            runListener.testSetStarting( createReportEntry( testIdentifier ) );
        }
        else if ( testIdentifier.isTest() )
        {
            testStartTime.put( testIdentifier, System.currentTimeMillis() );
            runListener.testStarting( createReportEntry( testIdentifier ) );
        }
    }

    @Override
    public void executionFinished( TestIdentifier testIdentifier, TestExecutionResult testExecutionResult )
    {
        boolean isClass = testIdentifier.isContainer()
                && testIdentifier.getSource().filter( ClassSource.class::isInstance ).isPresent();

        boolean isTest = testIdentifier.isTest();

        if ( isClass || isTest )
        {
            Integer elapsed = computeElapsedTime( testIdentifier );
            switch ( testExecutionResult.getStatus() )
            {
                case ABORTED:
                    if ( isTest )
                    {
                        runListener.testAssumptionFailure(
                                createReportEntry( testIdentifier, testExecutionResult, elapsed ) );
                    }
                    else
                    {
                        runListener.testSetCompleted( createReportEntry( testIdentifier, testExecutionResult,
                                systemProps(), null, elapsed ) );
                    }
                    break;
                case FAILED:
                    if ( !isTest )
                    {
                        runListener.testSetCompleted( createReportEntry( testIdentifier, testExecutionResult,
                                systemProps(), null, elapsed ) );
                    }
                    else if ( testExecutionResult.getThrowable()
                            .filter( AssertionError.class::isInstance ).isPresent() )
                    {
                        runListener.testFailed( createReportEntry( testIdentifier, testExecutionResult, elapsed ) );
                    }
                    else
                    {
                        runListener.testError( createReportEntry( testIdentifier, testExecutionResult, elapsed ) );
                    }
                    break;
                default:
                    if ( isTest )
                    {
                        runListener.testSucceeded( createReportEntry( testIdentifier, null, elapsed ) );
                    }
                    else
                    {
                        runListener.testSetCompleted(
                                createReportEntry( testIdentifier, null, systemProps(), null, elapsed ) );
                    }
            }
        }
    }

    private Integer computeElapsedTime( TestIdentifier testIdentifier )
    {
        Long startTime = testStartTime.remove( testIdentifier );
        long endTime = System.currentTimeMillis();
        return startTime == null ? null : (int) ( endTime - startTime );
    }

    @Override
    public void executionSkipped( TestIdentifier testIdentifier, String reason )
    {
        testStartTime.remove( testIdentifier );
        runListener.testSkipped( createReportEntry( testIdentifier, null, emptyMap(), reason, null ) );
    }

    private SimpleReportEntry createReportEntry( TestIdentifier testIdentifier,
                                                 TestExecutionResult testExecutionResult,
                                                 Map<String, String> systemProperties,
                                                 String reason,
                                                 Integer elapsedTime )
    {
        String[] classMethodName = toClassMethodName( testIdentifier );
        String className = classMethodName[0];
        String classText = classMethodName[1];
        if ( Objects.equals( className, classText ) )
        {
            classText = null;
        }
        String methodName = testIdentifier.isTest() ? classMethodName[2] : null;
        String methodText = testIdentifier.isTest() ? classMethodName[3] : null;
        if ( Objects.equals( methodName, methodText ) )
        {
            methodText = null;
        }
        StackTraceWriter stw =
                testExecutionResult == null ? null : toStackTraceWriter( className, methodName, testExecutionResult );
        return new SimpleReportEntry( className, classText, methodName, methodText,
                stw, elapsedTime, reason, systemProperties );
    }

    private SimpleReportEntry createReportEntry( TestIdentifier testIdentifier )
    {
        return createReportEntry( testIdentifier, null, null );
    }

    private SimpleReportEntry createReportEntry( TestIdentifier testIdentifier,
                                                 TestExecutionResult testExecutionResult, Integer elapsedTime )
    {
        return createReportEntry( testIdentifier, testExecutionResult, emptyMap(), null, elapsedTime );
    }

    private StackTraceWriter toStackTraceWriter( String realClassName, String realMethodName,
                                                 TestExecutionResult testExecutionResult )
    {
        switch ( testExecutionResult.getStatus() )
        {
            case ABORTED:
            case FAILED:
                // Failed tests must have a StackTraceWriter, otherwise Surefire will fail
                Throwable exception = testExecutionResult.getThrowable().orElse( null );
                return toStackTraceWriter( realClassName, realMethodName, exception );
            default:
                return testExecutionResult.getThrowable()
                        .map( t -> toStackTraceWriter( realClassName, realMethodName, t ) )
                        .orElse( null );
        }
    }

    private StackTraceWriter toStackTraceWriter( String realClassName, String realMethodName, Throwable throwable )
    {
        return new PojoStackTraceWriter( realClassName, realMethodName, throwable );
    }

    /**
     * <ul>
     *     <li>[0] class name - used in stacktrace parser</li>
     *     <li>[1] class display name</li>
     *     <li>[2] method signature - used in stacktrace parser</li>
     *     <li>[3] method display name</li>
     * </ul>
     *
     * @param testIdentifier a class or method
     * @return 4 elements string array
     */
    private String[] toClassMethodName( TestIdentifier testIdentifier )
    {
        Optional<TestSource> testSource = testIdentifier.getSource();
        String display = testIdentifier.getDisplayName();

        if ( testSource.filter( MethodSource.class::isInstance ).isPresent() )
        {
            MethodSource methodSource = testSource.map( MethodSource.class::cast ).get();
            String realClassName = methodSource.getClassName();

            String[] source = testPlan.getParent( testIdentifier )
                    .map( this::toClassMethodName )
                    .map( s -> new String[] { s[0], s[1] } )
                    .orElse( new String[] { realClassName, realClassName } );

            String methodName = methodSource.getMethodName();
            boolean useMethod = display.equals( methodName ) || display.equals( methodName + "()" );
            String resolvedMethodName = useMethod ? methodName : display;

            return new String[] { source[0], source[1], methodName, resolvedMethodName };
        }
        else if ( testSource.filter( ClassSource.class::isInstance ).isPresent() )
        {
            ClassSource classSource = testSource.map( ClassSource.class::cast ).get();
            String className = classSource.getClassName();
            String simpleClassName = className.substring( 1 + className.lastIndexOf( '.' ) );
            String source = display.equals( simpleClassName ) ? className : display;
            return new String[] { className, source, null, null };
        }
        else
        {
            String source = testPlan.getParent( testIdentifier )
                    .map( TestIdentifier::getDisplayName )
                    .orElse( display );
            return new String[] { source, source, display, display };
        }
    }
}
