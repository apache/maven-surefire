package listenReport;

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
import org.testng.ITestResult;
import org.testng.internal.IResultListener;


public class ResultListener
    implements IResultListener
{

    public void onFinish( ITestContext context )
    {

    }

    public void onStart( ITestContext context )
    {
        FileHelper.writeFile( "resultlistener-output.txt", "This is a result listener" );
    }

    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {

    }

    public void onTestFailure( ITestResult result )
    {

    }

    public void onTestSkipped( ITestResult result )
    {


    }

    public void onTestStart( ITestResult result )
    {


    }

    public void onTestSuccess( ITestResult result )
    {


    }

    public void onConfigurationFailure( ITestResult itr )
    {


    }

    public void onConfigurationSkip( ITestResult itr )
    {


    }

    public void onConfigurationSuccess( ITestResult itr )
    {


    }

}
