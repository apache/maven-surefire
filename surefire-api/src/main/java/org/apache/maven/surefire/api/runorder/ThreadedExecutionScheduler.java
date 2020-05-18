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

import java.util.ArrayList;
import java.util.List;


/**
 * @author Kristian Rosenvold
 */
public class ThreadedExecutionScheduler
{
    private final int numThreads;

    private final int runTime[];

    private final List<Class<?>>[] lists;

    @SuppressWarnings( "unchecked" )
    public ThreadedExecutionScheduler( int numThreads )
    {
        this.numThreads = numThreads;
        runTime = new int[numThreads];
        lists = new List[numThreads];
        for ( int i = 0; i < numThreads; i++ )
        {
            lists[i] = new ArrayList<>();
        }
    }

    public void addTest( PrioritizedTest prioritizedTest )
    {
        final int leastBusySlot = findLeastBusySlot();
        runTime[leastBusySlot] += prioritizedTest.getTotalRuntime();
        //noinspection unchecked
        lists[leastBusySlot].add( prioritizedTest.getClazz() );
    }

    public List<Class<?>> getResult()
    {
        List<Class<?>> result = new ArrayList<>();
        int index = 0;
        boolean added;
        do
        {
            added = false;
            for ( int i = 0; i < numThreads; i++ )
            {
                if ( lists[i].size() > index )
                {
                    result.add( lists[i].get( index ) );
                    added = true;
                }
            }
            index++;
        }
        while ( added );
        return result;
    }

    private int findLeastBusySlot()
    {
        int leastBusy = 0;
        int minRuntime = runTime[0];
        for ( int i = 1; i < numThreads; i++ )
        {
            if ( runTime[i] < minRuntime )
            {
                leastBusy = i;
                minRuntime = runTime[i];
            }
        }
        return leastBusy;
    }
}
