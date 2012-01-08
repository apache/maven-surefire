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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.util.NestedRuntimeException;

import org.junit.runner.Description;

/**
 * * Represents the test-state of a testset that is run.
 */
public class TestSet
{
    private final Description testSetDescription;

    private final AtomicInteger numberOfCompletedChildren = new AtomicInteger( 0 );

    // While the two parameters below may seem duplicated, it is not entirely the case,
    // since numberOfTests has the correct value from the start, while testMethods grows as method execution starts.

    private final AtomicInteger numberOfTests = new AtomicInteger( 0 );

    private final List<TestMethod> testMethods = Collections.synchronizedList( new ArrayList<TestMethod>() );

    private static final InheritableThreadLocal<TestSet> testSet = new InheritableThreadLocal<TestSet>();

    private final AtomicBoolean allScheduled = new AtomicBoolean();

    private final AtomicBoolean played = new AtomicBoolean();

    private volatile LogicalStream beforeClass;

    private volatile LogicalStream afterClass;

    public TestSet( Description testSetDescription )
    {
        this.testSetDescription = testSetDescription;
    }

    public void replay( RunListener target )
    {
        if ( !played.compareAndSet( false, true ) )
        {
            return;
        }

        try
        {
            int elapsed = 0;
            for ( TestMethod testMethod : testMethods )
            {
                elapsed += testMethod.getElapsed();
            }
            ReportEntry report = createReportEntry( null );

            target.testSetStarting( report );

            if ( beforeClass != null )
            {
                beforeClass.writeDetails( ( (ConsoleOutputReceiver) target ) );
            }

            for ( TestMethod testMethod : testMethods )
            {
                testMethod.replay( target );
            }

            report = createReportEntry( elapsed );

            if ( afterClass != null )
            {
                afterClass.writeDetails( ( (ConsoleOutputReceiver) target ) );
            }
            target.testSetCompleted( report );
        }
        catch ( Exception e )
        {
            throw new NestedRuntimeException( e );
        }
    }

    public TestMethod createTestMethod( ReportEntry description )
    {
        TestMethod testMethod = new TestMethod( description );
        addTestMethod( testMethod );
        return testMethod;
    }

    private ReportEntry createReportEntry( Integer elapsed )
    {
        boolean isJunit3 = testSetDescription.getTestClass() == null;
        String classNameToUse =
            isJunit3 ? testSetDescription.getChildren().get( 0 ).getClassName() : testSetDescription.getClassName();
        return new SimpleReportEntry( classNameToUse, classNameToUse, elapsed );
    }

    public void incrementTestMethodCount()
    {
        numberOfTests.incrementAndGet();
    }

    private void addTestMethod( TestMethod testMethod )
    {
        testMethods.add( testMethod );
    }

    public void incrementFinishedTests( RunListener reporterManager, boolean reportImmediately )
    {
        numberOfCompletedChildren.incrementAndGet();
        if ( allScheduled.get() && isAllTestsDone() && reportImmediately )
        {
            replay( reporterManager );
        }
    }

    public void setAllScheduled( RunListener reporterManager )
    {
        allScheduled.set( true );
        if ( isAllTestsDone() )
        {
            replay( reporterManager );
        }
    }

    private boolean isAllTestsDone()
    {
        return testMethods.size() == numberOfCompletedChildren.get();
    }

    public void attachToThread()
    {
        testSet.set( this );
    }

    public static TestSet getThreadTestSet()
    {
        return testSet.get();
    }

    public LogicalStream getClassLevelLogicalStream()
    {
        if ( numberOfCompletedChildren.get() > 0 )
        {
            if ( afterClass == null )
            {
                afterClass = new LogicalStream();
            }
            return afterClass;
        }
        else
        {
            if ( beforeClass == null )
            {
                beforeClass = new LogicalStream();
            }
            return beforeClass;
        }
    }


}
