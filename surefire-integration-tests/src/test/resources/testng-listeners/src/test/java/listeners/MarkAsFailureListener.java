package listeners;

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
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Created by etigwuu on 2014-04-26.
 */
public class MarkAsFailureListener implements ITestListener, IInvokedMethodListener {

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onTestStart(ITestResult result) {

    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onTestSuccess(ITestResult result) {

    }

    public static int counter = 0;
    /**
     * I will be called twice in some condition!!!
     * @param result
     */
    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onTestFailure(ITestResult result) {
        System.out.println(++counter);
    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onTestSkipped(ITestResult result) {

    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {

    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onStart(ITestContext context) {

    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void onFinish(ITestContext context) {

    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {

    }

    //todo add @Override in surefire 3.0 running on the top of JDK 6
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        testResult.setStatus(ITestResult.FAILURE);
    }
}
