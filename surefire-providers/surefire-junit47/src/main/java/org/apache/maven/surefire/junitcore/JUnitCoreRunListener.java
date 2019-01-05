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
import org.apache.maven.surefire.common.junit4.JUnit4StackTraceWriter;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.util.Map;

import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractClassName;

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
        try
        {
            reporter.testSetCompleted( null );
        }
        finally
        {
            classMethodCounts.clear();
        }
    }

    private void fillTestCountMap( Description testDesc )
    {
        for ( Description child : testDesc.getChildren() )
        {
            if ( !asTestLeaf( child ) )
            {
                fillTestCountMap( child );
            }
        }
    }

    private boolean asTestLeaf( Description description )
    {
        if ( description.isTest() )
        {
            final String testClassName = extractClassName( description.getDisplayName() );
            if ( testClassName != null )
            {
                final TestSet testSet;
                if ( classMethodCounts.containsKey( testClassName ) )
                {
                    testSet = classMethodCounts.get( testClassName );
                }
                else
                {
                    testSet = new TestSet( testClassName );
                    classMethodCounts.put( testClassName, testSet );
                }
                testSet.incrementTestMethodCount();
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected StackTraceWriter createStackTraceWriter( Failure failure )
    {
        return new JUnit4StackTraceWriter( failure );
    }
}
