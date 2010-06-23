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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * * Represents the test-state of a testset that is run.
 */
public class TestSet
{
    private static ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private final Description testSetDescription;

    private AtomicInteger numberOfCompletedChildren = new AtomicInteger( 0 );

    // While the two parameters below may seem duplicated, it is not entirely the case,
    // since numberOfTests has the correct value from the start, while testMethods grows as method execution starts.

    private final AtomicInteger numberOfTests = new AtomicInteger( 0 );

    private final List<TestMethod> testMethods = Collections.synchronizedList( new ArrayList<TestMethod>() );

    private static final InheritableThreadLocal<TestSet> testSet = new InheritableThreadLocal<TestSet>();

    private AtomicBoolean allScheduled = new AtomicBoolean();
    private AtomicBoolean played = new AtomicBoolean();


    public TestSet( Description testSetDescription )
    {
        this.testSetDescription = testSetDescription;
    }

    public void replay( ReporterManager target )
    {
        if (!played.compareAndSet( false, true )) return;

        try
        {
            ReportEntry report = createReportEntry( "testSetStarting" );

            target.testSetStarting( report );

            for ( TestMethod testMethod : testMethods )
            {
                testMethod.replay( target );
            }
            report = createReportEntry( "testSetCompletedNormally" );

            target.testSetCompleted( report );

            target.reset();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    public TestMethod createTestMethod( Description description )
    {
        TestMethod testMethod = new TestMethod( description );
        addTestMethod( testMethod );
        return testMethod;
    }

    private ReportEntry createReportEntry( String rawString2 )
    {
        String rawString = bundle.getString( rawString2 );
        boolean isJunit3 = testSetDescription.getTestClass() == null;
        String classNameToUse =
            isJunit3 ? testSetDescription.getChildren().get( 0 ).getClassName() : testSetDescription.getClassName();
        return new ReportEntry( classNameToUse, classNameToUse, rawString );
    }

    public void incrementTestMethodCount()
    {
        numberOfTests.incrementAndGet();
    }

    public void addTestMethod( TestMethod testMethod )
    {
        testMethods.add( testMethod );
    }

    public void incrementFinishedTests( ReporterManager reporterManager, boolean reportImmediately )
    {
        numberOfCompletedChildren.incrementAndGet();
        if ( allScheduled.get() && isAllTestsDone() && reportImmediately)
        {
            replay( reporterManager );
        }
    }

    public void setAllScheduled( ReporterManager reporterManager )
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
}
