package org.apache.maven.surefire.report;

import org.apache.maven.surefire.util.internal.DaemonThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
class RunnableTestClass1
    implements Callable<Object>
{
    @Override
    public Object call()
        throws Exception
    {
        doSomethingThatThrows();
        return "yo";
    }

    private void doSomethingThatThrows()
        throws ExecutionException
    {
        RunnableTestClass2 rt2 = new RunnableTestClass2();
        FutureTask<Object> futureTask = new FutureTask<>( rt2 );
        DaemonThreadFactory.newDaemonThread( futureTask ).start();
        try
        {
            futureTask.get();
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException();
        }
    }

}
