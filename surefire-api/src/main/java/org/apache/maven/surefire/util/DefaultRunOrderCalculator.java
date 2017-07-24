package org.apache.maven.surefire.util;

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

import org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMap;
import org.apache.maven.surefire.testset.RunOrderParameters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Applies the final runorder of the tests
 *
 * @author Kristian Rosenvold
 */
public class DefaultRunOrderCalculator
    implements RunOrderCalculator
{
    private final Comparator<Class> sortOrder;

    private final RunOrder[] runOrder;

    private final RunOrderParameters runOrderParameters;

    private final int threadCount;

    public DefaultRunOrderCalculator( RunOrderParameters runOrderParameters, int threadCount )
    {
        this.runOrderParameters = runOrderParameters;
        this.threadCount = threadCount;
        this.runOrder = runOrderParameters.getRunOrder();
        this.sortOrder = this.runOrder.length > 0 ? getSortOrderComparator( this.runOrder[0] ) : null;
    }

    @Override
    @SuppressWarnings( "checkstyle:magicnumber" )
    public TestsToRun orderTestClasses( TestsToRun scannedClasses )
    {
        List<Class<?>> result = new ArrayList<Class<?>>( 512 );

        for ( Class<?> scannedClass : scannedClasses )
        {
            result.add( scannedClass );
        }

        orderTestClasses( result, runOrder.length != 0 ? runOrder[0] : null );
        return new TestsToRun( new LinkedHashSet<Class<?>>( result ) );
    }

    private void orderTestClasses( List<Class<?>> testClasses, RunOrder runOrder )
    {
        if ( RunOrder.RANDOM.equals( runOrder ) )
        {
            Collections.shuffle( testClasses );
        }
        else if ( RunOrder.FAILEDFIRST.equals( runOrder ) )
        {
            RunEntryStatisticsMap stat = RunEntryStatisticsMap.fromFile( runOrderParameters.getRunStatisticsFile() );
            List<Class<?>> prioritized = stat.getPrioritizedTestsByFailureFirst( testClasses );
            testClasses.clear();
            testClasses.addAll( prioritized );

        }
        else if ( RunOrder.BALANCED.equals( runOrder ) )
        {
            RunEntryStatisticsMap stat = RunEntryStatisticsMap.fromFile( runOrderParameters.getRunStatisticsFile() );
            List<Class<?>> prioritized = stat.getPrioritizedTestsClassRunTime( testClasses, threadCount );
            testClasses.clear();
            testClasses.addAll( prioritized );

        }
        else if ( sortOrder != null )
        {
            Collections.sort( testClasses, sortOrder );
        }
    }

    private Comparator<Class> getSortOrderComparator( RunOrder runOrder )
    {
        if ( RunOrder.ALPHABETICAL.equals( runOrder ) )
        {
            return getAlphabeticalComparator();
        }
        else if ( RunOrder.REVERSE_ALPHABETICAL.equals( runOrder ) )
        {
            return getReverseAlphabeticalComparator();
        }
        else if ( RunOrder.HOURLY.equals( runOrder ) )
        {
            final int hour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
            return ( ( hour % 2 ) == 0 ) ? getAlphabeticalComparator() : getReverseAlphabeticalComparator();
        }
        else
        {
            return null;
        }
    }

    private Comparator<Class> getReverseAlphabeticalComparator()
    {
        return new Comparator<Class>()
        {
            @Override
            public int compare( Class o1, Class o2 )
            {
                return o2.getName().compareTo( o1.getName() );
            }
        };
    }

    private Comparator<Class> getAlphabeticalComparator()
    {
        return new Comparator<Class>()
        {
            @Override
            public int compare( Class o1, Class o2 )
            {
                return o1.getName().compareTo( o2.getName() );
            }
        };
    }
}
