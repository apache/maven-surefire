package org.apache.maven.plugin.surefire.runorder;

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

import org.apache.maven.surefire.util.internal.ClassMethod;

/**
 * @author Kristian Rosenvold
 */
public class RunEntryStatistics
{
    private final int runTime;

    private final int successfulBuilds;

    private final ClassMethod classMethod;

    RunEntryStatistics( int runTime, int successfulBuilds, String clazz, String method )
    {
        this( runTime, successfulBuilds, new ClassMethod( clazz, method ) );
    }

    RunEntryStatistics( int runTime, int successfulBuilds, ClassMethod classMethod )
    {
        this.runTime = runTime;
        this.successfulBuilds = successfulBuilds;
        this.classMethod = classMethod;
    }

    public ClassMethod getClassMethod()
    {
        return classMethod;
    }

    public RunEntryStatistics nextGeneration( int runTime )
    {
        return new RunEntryStatistics( runTime, successfulBuilds + 1, classMethod );
    }

    public RunEntryStatistics nextGenerationFailure( int runTime )
    {
        return new RunEntryStatistics( runTime, 0, classMethod );
    }

    public int getRunTime()
    {
        return runTime;
    }

    public int getSuccessfulBuilds()
    {
        return successfulBuilds;
    }
}
