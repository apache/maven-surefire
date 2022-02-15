package org.apache.maven.surefire.api.util;

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

import org.apache.maven.surefire.api.runorder.RunEntryStatisticsMap;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestListResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

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

    private final Random random;

    private final TestListResolver testListResolver;

    public DefaultRunOrderCalculator( RunOrderParameters runOrderParameters, int threadCount )
    {
        this.runOrderParameters = runOrderParameters;
        this.threadCount = threadCount;
        this.runOrder = runOrderParameters.getRunOrder();
        this.sortOrder = this.runOrder.length > 0 ? getSortOrderComparator( this.runOrder[0] ) : null;
        Long runOrderRandomSeed = runOrderParameters.getRunOrderRandomSeed();
        if ( runOrderRandomSeed == null )
        {
            runOrderRandomSeed = System.nanoTime();
            runOrderParameters.setRunOrderRandomSeed( runOrderRandomSeed );
        }
        this.random = new Random( runOrderRandomSeed );
        if ( RunOrder.TESTORDER.equals( getRunOrderMethod() ) )
        {
            this.testListResolver = getTestListResolver();
        }
        else
        {
            this.testListResolver = null;
        }
    }

    @Override
    @SuppressWarnings( "checkstyle:magicnumber" )
    public TestsToRun orderTestClasses( TestsToRun scannedClasses )
    {
        List<Class<?>> result = new ArrayList<>( 512 );

        for ( Class<?> scannedClass : scannedClasses )
        {
            result.add( scannedClass );
        }

        orderTestClasses( result, runOrder.length != 0 ? runOrder[0] : null );
        return new TestsToRun( new LinkedHashSet<>( result ) );
    }

    @Override
    public Comparator<String> comparatorForTestMethods()
    {
        RunOrder methodRunOrder = getRunOrderMethod();
        if ( RunOrder.TESTORDER.equals( methodRunOrder ) )
        {
            return new Comparator<String>()
            {
                @Override
                public int compare( String o1, String o2 )
                {
                    String[] classAndMethod1 = getClassAndMethod( o1 );
                    String className1 = classAndMethod1[0];
                    String methodName1 = classAndMethod1[1];
                    String[] classAndMethod2 = getClassAndMethod( o2 );
                    String className2 = classAndMethod2[0];
                    String methodName2 = classAndMethod2[1];
                    return testListResolver.testOrderComparator( className1, className2, methodName1, methodName2 );
                }
            };
        }
        else
        {
            return null;
        }
    }

    public TestListResolver getTestListResolver()
    {
        String orderParam = System.getProperty( "test" );
        if ( orderParam == null  )
        {
            throw new IllegalStateException( "TestListResolver in RunOrderCalculator should be used only when "
                    + "system property -Dtest is set and runOrder is testorder" );
        }
        return new TestListResolver( Arrays.asList( orderParam.split( "," ) ) );
    }

    public String[] getClassAndMethod( String request )
    {
        String[] classAndMethod = { request, request };
        if ( request.contains( "(" ) )
        {
            String[] nameSplit1 = request.split( "\\(" );
            classAndMethod[0] = nameSplit1[1].substring( 0, nameSplit1[1].length() - 1 );
            classAndMethod[1] = nameSplit1[0];
        }
        return classAndMethod;
    }

    private RunOrder getRunOrderMethod()
    {
        if ( runOrder.length > 1 && Arrays.asList( runOrder ).contains( RunOrder.TESTORDER ) )
        {
            // Use of testorder and other runOrders are currently not supported
            throw new IllegalStateException( "Expected only testorder. Got: " + runOrder.length );
        }
        return runOrder[0];
    }

    private void orderTestClasses( List<Class<?>> testClasses, RunOrder runOrder )
    {
        if ( RunOrder.TESTORDER.equals( runOrder ) )
        {
            Collections.sort( testClasses, new Comparator<Class<?>>()
                    {
                        @Override
                        public int compare( Class<?> o1, Class<?> o2 )
                        {
                            return testListResolver.testOrderComparator( o1.getName(), o2.getName(), null, null );
                        }
                    } );
        }
        else if ( RunOrder.RANDOM.equals( runOrder ) )
        {
            Collections.shuffle( testClasses, random );
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
