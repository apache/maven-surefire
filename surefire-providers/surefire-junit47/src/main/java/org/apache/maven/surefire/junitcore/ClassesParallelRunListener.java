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

import java.util.Map;

import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * @author Kristian Rosenvold
 */
@ThreadSafe
public class ClassesParallelRunListener
    extends ConcurrentRunListener
{
    public ClassesParallelRunListener( Map<String, TestSet> classMethodCounts, ReporterFactory reporterFactory,
                                       ConsoleStream consoleStream )
        throws TestSetFailedException
    {
        super( reporterFactory, consoleStream, false, classMethodCounts );
    }

    @Override
    protected void checkIfTestSetCanBeReported( TestSet testSetForTest )
    {
        TestSet currentlyAttached = TestSet.getThreadTestSet();
        if ( currentlyAttached != null && currentlyAttached != testSetForTest )
        {
            currentlyAttached.setAllScheduled( getRunListener() );
        }
    }
}
