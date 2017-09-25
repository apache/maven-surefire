package org.apache.maven.surefire.testng.utils;

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

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Sends an even in {@link FailFastEventsSingleton} that failure has appeared.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class FailFastListener
    implements ITestListener
{
    private final Stoppable stoppable;

    public FailFastListener( Stoppable stoppable )
    {
        this.stoppable = stoppable;
    }

    @Override
    public void onTestStart( ITestResult result )
    {

    }

    @Override
    public void onTestSuccess( ITestResult result )
    {

    }

    @Override
    public void onTestFailure( ITestResult result )
    {
        stoppable.fireStopEvent();
    }

    @Override
    public void onTestSkipped( ITestResult result )
    {

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {

    }

    @Override
    public void onStart( ITestContext context )
    {

    }

    @Override
    public void onFinish( ITestContext context )
    {

    }
}
