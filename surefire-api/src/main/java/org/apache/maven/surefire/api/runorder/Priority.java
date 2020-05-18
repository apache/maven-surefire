package org.apache.maven.surefire.api.runorder;

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

/**
 * @author Kristian Rosenvold
 */
public class Priority
{
    private final String className;

    int priority;

    int totalRuntime = 0;

    int minSuccessRate = Integer.MAX_VALUE;

    public Priority( String className )
    {
        this.className = className;
    }

    /**
     * Returns a priority that applies to a new testclass (that has never been run/recorded)
     *
     * @param className The class name
     * @return A priority
     */
    public static Priority newTestClassPriority( String className )
    {
        Priority priority1 = new Priority( className );
        priority1.setPriority( 0 );
        priority1.minSuccessRate = 0;
        return priority1;
    }

    public void addItem( RunEntryStatistics itemStat )
    {
        totalRuntime += itemStat.getRunTime();
        minSuccessRate = Math.min( minSuccessRate, itemStat.getSuccessfulBuilds() );
    }


    public int getTotalRuntime()
    {
        return totalRuntime;
    }

    public int getMinSuccessRate()
    {
        return minSuccessRate;
    }

    public String getClassName()
    {
        return className;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority( int priority )
    {
        this.priority = priority;
    }
}
