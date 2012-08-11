package org.apache.maven.plugin.surefire;

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

import org.apache.maven.surefire.suite.RunResult;

public class Summary
{
    private boolean forking = false;

    private RunResult runResult;

    private Exception exception;

    public void reportForkConfiguration( boolean isForking )
    {
        forking = isForking;
    }

    public void registerException( Exception exception )
    {
        if ( this.exception == null )
        {
            this.exception = exception;
        }
    }

    public void registerRunResult( RunResult result )
    {
        runResult = result;
    }

    public boolean isErrorFree()
    {
        return exception == null;
    }

    public boolean isFailureOrTimeout()
    {
        return runResult != null && runResult.isFailureOrTimeout();
    }

    public boolean isForking()
    {
        return forking;
    }

    public Exception getFirstException()
    {
        return exception;
    }

    public RunResult getResultOfLastSuccessfulRun()
    {
        return runResult;
    }

    public static Summary notests(){
        return new Summary();
    }
}

