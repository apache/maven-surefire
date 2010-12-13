package org.apache.maven.surefire.junit4;

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

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.AbstractTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.util.ArrayList;
import java.util.List;

public class JUnit4TestSet
    extends AbstractTestSet
{
    private final List<RunListener> customRunListeners;

    /**
     * Constructor.
     *
     * @param testClass          the class to be run as a test
     * @param customRunListeners the custom run listeners to add
     */
    protected JUnit4TestSet( Class testClass, List<RunListener> customRunListeners )
    {
        super( testClass );
        this.customRunListeners = customRunListeners;
    }

    // Bogus constructor so we can build with 2.5. Remove for 2.7.1
    protected JUnit4TestSet( Class testClass )
    {
        super( testClass );
        this.customRunListeners = new ArrayList();
    }

    /**
     * Actually runs the test and adds the tests results to the <code>reportManager</code>.
     *
     * @see org.apache.maven.surefire.testset.SurefireTestSet#execute(org.apache.maven.surefire.report.ReporterManager, java.lang.ClassLoader)
     */
    public void execute( ReporterManager reportManager, ClassLoader loader )
        throws TestSetFailedException
    {
        List<RunListener> listeners = new ArrayList<RunListener>();
        listeners.add( new JUnit4TestSetReporter( getTestClass(), reportManager ) );
        listeners.addAll( customRunListeners );
        execute( getTestClass(), listeners );
    }

    /**
     * Actually runs the test and adds the tests results to the <code>reportManager</code>.
     *
     * @param testClass    The test class to run
     * @param runListeners The run listeners to attach
     * @see org.apache.maven.surefire.testset.SurefireTestSet#execute(org.apache.maven.surefire.report.ReporterManager, java.lang.ClassLoader)
     */
    public static void execute( Class testClass, List<RunListener> runListeners )
        throws TestSetFailedException
    {
        RunNotifier fNotifier = new RunNotifier();
        for ( RunListener listener : runListeners )
        {
            fNotifier.addListener( listener );
        }
        try
        {
            execute( testClass, fNotifier );
        }
        finally
        {
            for ( RunListener listener : runListeners )
            {
                fNotifier.removeListener( listener );
            }
        }
    }

    public static void execute( Class testClass, RunNotifier fNotifier )
        throws TestSetFailedException
    {
        Runner junitTestRunner = Request.aClass( testClass ).getRunner();

        junitTestRunner.run( fNotifier );
    }
}

