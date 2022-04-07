package org.apache.maven.surefire.api.testset;

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

import java.io.File;
import org.apache.maven.surefire.api.util.RunOrder;

import static org.apache.maven.surefire.api.util.RunOrder.ALPHABETICAL;
import static org.apache.maven.surefire.api.util.RunOrder.DEFAULT;

/**
 * @author Kristian Rosenvold
 */
public final class RunOrderParameters
{
    private final RunOrder[] runOrder;

    private final File runStatisticsFile;

    private final Long runOrderRandomSeed;

    public RunOrderParameters( RunOrder[] runOrder, File runStatisticsFile )
    {
        this( runOrder, runStatisticsFile, null );
    }

    public RunOrderParameters( String runOrder, File runStatisticsFile )
    {
        this( runOrder, runStatisticsFile, null );
    }

    public RunOrderParameters( String runOrder, File runStatisticsFile, Long runOrderRandomSeed )
    {
        this( runOrder == null ? DEFAULT : RunOrder.valueOfMulti( runOrder ), runStatisticsFile, runOrderRandomSeed );
    }

    public RunOrderParameters( RunOrder[] runOrder, File runStatisticsFile, Long runOrderRandomSeed )
    {
        this.runOrder = runOrder;
        this.runStatisticsFile = runStatisticsFile;
        this.runOrderRandomSeed = runOrderRandomSeed;
    }

    public static RunOrderParameters alphabetical()
    {
        return new RunOrderParameters( new RunOrder[]{ ALPHABETICAL }, null );
    }

    public RunOrder[] getRunOrder()
    {
        return runOrder;
    }

    public Long getRunOrderRandomSeed()
    {
        return runOrderRandomSeed;
    }

    public File getRunStatisticsFile()
    {
        return runStatisticsFile;
    }
}
