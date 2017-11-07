package org.apache.maven.plugin.surefire.runorder.model;

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

import org.apache.maven.plugin.surefire.runorder.impl.statistics.RunEntryStatisticsMap;
import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.surefire.testset.RunOrderParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Dipak Pawar
 */
public class BalancedRunOrder implements RunOrder
{

    @Override
    public String getName()
    {
        return "balanced";
    }

    @Override
    public List<Class<?>> orderTestClasses( Collection<Class<?>> scannedClasses,
                                            RunOrderParameters runOrderParameters, int threadCount )
    {
        final List<Class<?>> testClasses = new ArrayList<Class<?>>( scannedClasses );

        RunEntryStatisticsMap stat = RunEntryStatisticsMap.fromFile( runOrderParameters.getRunStatisticsFile() );
        List<Class<?>> prioritized = stat.getPrioritizedTestsClassRunTime( testClasses,
                threadCount );
        testClasses.clear();
        testClasses.addAll( prioritized );

        return testClasses;
    }
}
