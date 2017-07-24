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
import org.apache.maven.surefire.common.junit48.JUnit46StackTraceWriter;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.Parameterized;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Noteworthy things about JUnit4 listening:
 * <br>
 * A class that is annotated with @Ignore will have one invocation of "testSkipped" with source==name
 * A method that is annotated with @Ignore will have a invocation of testSkipped with source and name distinct
 * Methods annotated with @Ignore trigger no further events.
 *
 * @see org.apache.maven.surefire.junitcore.ConcurrentRunListener for details about parallel running
 */
public class JUnitCoreRunListener
    extends JUnit4RunListener
{
    private final Map<String, TestSet> classMethodCounts;

    /**
     * @param reporter          the report manager to log testing events to
     * @param classMethodCounts A map of methods
     */
    public JUnitCoreRunListener( RunListener reporter, Map<String, TestSet> classMethodCounts )
    {
        super( reporter );
        this.classMethodCounts = classMethodCounts;
    }

    /**
     * Called right before any tests from a specific class are run.
     *
     * @see org.junit.runner.notification.RunListener#testRunStarted(org.junit.runner.Description)
     */
    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
        fillTestCountMap( description );
        reporter.testSetStarting( null ); // Not entirely meaningful as we can see
    }

    @Override
    public void testRunFinished( Result result )
        throws Exception
    {
        reporter.testSetCompleted( null );
    }

    private void fillTestCountMap( Description testDesc )
    {
        TestSet testSet = new TestSet( testDesc );

        String itemTestClassName =
                isParameterizedRunner( testDesc ) ? testDesc.getClassName() : asSuiteRunner( testDesc, testSet );

        if ( itemTestClassName != null )
        {
            classMethodCounts.put( itemTestClassName, testSet );
        }
    }

    private String asSuiteRunner( Description description, TestSet testSet )
    {
        String itemTestClassName = null;
        for ( Description child : description.getChildren() )
        {
            if ( !child.isTest() )
            {
                fillTestCountMap( child );
            }
            else
            {
                if ( extractDescriptionMethodName( child ) != null )
                {
                    testSet.incrementTestMethodCount();
                    if ( itemTestClassName == null )
                    {
                        itemTestClassName = extractDescriptionClassName( child );
                    }
                }
                else
                {
                    classMethodCounts.put( extractDescriptionClassName( child ), new TestSet( child ) );
                }
            }
        }
        return itemTestClassName;
    }

    private static boolean isParameterizedRunner( Description description )
    {
        for ( Annotation ann : description.getAnnotations() )
        {
            if ( ann.annotationType() == RunWith.class )
            {
                return Parameterized.class.isAssignableFrom( ( (RunWith) ann ).value() );
            }
        }
        return false;
    }

    @Override
    protected StackTraceWriter createStackTraceWriter( Failure failure )
    {
        return new JUnit46StackTraceWriter( failure );
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
}
