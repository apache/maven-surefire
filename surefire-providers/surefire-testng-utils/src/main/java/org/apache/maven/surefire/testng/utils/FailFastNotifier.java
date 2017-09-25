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

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

/**
 * Notifies TestNG core skipping remaining tests after first failure has appeared.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class FailFastNotifier
    implements IInvokedMethodListener
{

    @Override
    public void beforeInvocation( IInvokedMethod iInvokedMethod, ITestResult iTestResult )
    {
        if ( FailFastEventsSingleton.getInstance().isSkipAfterFailure() )
        {
            throw new SkipException( "Skipped after failure. See parameter [skipAfterFailureCount] "
                                         + "in surefire or failsafe plugin." );
        }
    }

    @Override
    public void afterInvocation( IInvokedMethod iInvokedMethod, ITestResult iTestResult )
    {

    }
}
