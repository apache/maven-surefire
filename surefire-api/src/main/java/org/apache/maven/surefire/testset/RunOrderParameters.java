package org.apache.maven.surefire.testset;

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
import org.apache.maven.surefire.util.RunOrder;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderParameters
{
    private final RunOrder[] runOrder;

    private File runStatisticsFile;

    public RunOrderParameters( RunOrder[] runOrder, File runStatisticsFile )
    {
        this.runOrder = runOrder;
        this.runStatisticsFile = runStatisticsFile;
    }

    public RunOrderParameters( String runOrder, File runStatisticsFile )
    {
        this.runOrder = runOrder == null ? RunOrder.DEFAULT : RunOrder.valueOfMulti( runOrder );
        this.runStatisticsFile = runStatisticsFile;
    }

    public static RunOrderParameters alphabetical()
    {
        return new RunOrderParameters( new RunOrder[]{ RunOrder.ALPHABETICAL }, null );
    }

    public RunOrder[] getRunOrder()
    {
        return runOrder;
    }

    public File getRunStatisticsFile()
    {
        return runStatisticsFile;
    }

}
