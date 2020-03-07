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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.maven.surefire.booter.ProviderParameterNames;

/**
 * @author Kristian Rosenvold
 */
public final class JUnitCoreParameters
{
    public static final String PARALLEL_KEY = ProviderParameterNames.PARALLEL_PROP;

    public static final String PERCORETHREADCOUNT_KEY = "perCoreThreadCount";

    public static final String THREADCOUNT_KEY = ProviderParameterNames.THREADCOUNT_PROP;

    public static final String THREADCOUNTSUITES_KEY = ProviderParameterNames.THREADCOUNTSUITES_PROP;

    public static final String THREADCOUNTCLASSES_KEY = ProviderParameterNames.THREADCOUNTCLASSES_PROP;

    public static final String THREADCOUNTMETHODS_KEY = ProviderParameterNames.THREADCOUNTMETHODS_PROP;

    public static final String USEUNLIMITEDTHREADS_KEY = "useUnlimitedThreads";

    public static final String PARALLEL_TIMEOUT_KEY = ProviderParameterNames.PARALLEL_TIMEOUT_PROP;

    public static final String PARALLEL_TIMEOUTFORCED_KEY = ProviderParameterNames.PARALLEL_TIMEOUTFORCED_PROP;

    public static final String PARALLEL_OPTIMIZE_KEY = ProviderParameterNames.PARALLEL_OPTIMIZE_PROP;

    private final String parallel;

    private final boolean perCoreThreadCount;

    private final int threadCount;

    private final int threadCountSuites;

    private final int threadCountClasses;

    private final int threadCountMethods;

    private final double parallelTestsTimeoutInSeconds;

    private final double parallelTestsTimeoutForcedInSeconds;

    private final boolean useUnlimitedThreads;

    private final boolean parallelOptimization;

    public JUnitCoreParameters( Map<String, String> properties )
    {
        parallel = property( properties, PARALLEL_KEY, "none" ).toLowerCase();
        perCoreThreadCount = property( properties, PERCORETHREADCOUNT_KEY, true );
        threadCount = property( properties, THREADCOUNT_KEY, 0 );
        threadCountMethods = property( properties, THREADCOUNTMETHODS_KEY, 0 );
        threadCountClasses = property( properties, THREADCOUNTCLASSES_KEY, 0 );
        threadCountSuites = property( properties, THREADCOUNTSUITES_KEY, 0 );
        useUnlimitedThreads = property( properties, USEUNLIMITEDTHREADS_KEY, false );
        parallelTestsTimeoutInSeconds = Math.max( property( properties, PARALLEL_TIMEOUT_KEY, 0d ), 0 );
        parallelTestsTimeoutForcedInSeconds = Math.max( property( properties, PARALLEL_TIMEOUTFORCED_KEY, 0d ), 0 );
        parallelOptimization = property( properties, PARALLEL_OPTIMIZE_KEY, true );
    }

    private static Collection<String> lowerCase( String... elements )
    {
        ArrayList<String> lowerCase = new ArrayList<>();
        for ( String element : elements )
        {
            lowerCase.add( element.toLowerCase() );
        }
        return lowerCase;
    }

    private boolean isAllParallel()
    {
        return "all".equals( parallel );
    }

    public boolean isParallelMethods()
    {
        return isAllParallel() || lowerCase( "both", "methods", "suitesAndMethods", "classesAndMethods" ).contains(
            parallel );
    }

    public boolean isParallelClasses()
    {
        return isAllParallel() || lowerCase( "both", "classes", "suitesAndClasses", "classesAndMethods" ).contains(
            parallel );
    }

    public boolean isParallelSuites()
    {
        return isAllParallel() || lowerCase( "suites", "suitesAndClasses", "suitesAndMethods" ).contains( parallel );
    }

    /**
     * @deprecated Instead use the expression isParallelMethods() &amp;&amp; isParallelClasses().
     * @return {@code true} if classes and methods are both parallel
     */
    @Deprecated
    @SuppressWarnings( "unused" )
    public boolean isParallelBoth()
    {
        return isParallelMethods() && isParallelClasses();
    }

    public boolean isPerCoreThreadCount()
    {
        return perCoreThreadCount;
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    public int getThreadCountMethods()
    {
        return threadCountMethods;
    }

    public int getThreadCountClasses()
    {
        return threadCountClasses;
    }

    public int getThreadCountSuites()
    {
        return threadCountSuites;
    }

    public boolean isUseUnlimitedThreads()
    {
        return useUnlimitedThreads;
    }

    public double getParallelTestsTimeoutInSeconds()
    {
        return parallelTestsTimeoutInSeconds;
    }

    public double getParallelTestsTimeoutForcedInSeconds()
    {
        return parallelTestsTimeoutForcedInSeconds;
    }

    public boolean isNoThreading()
    {
        return !isParallelismSelected();
    }

    public boolean isParallelismSelected()
    {
        return isParallelSuites() || isParallelClasses() || isParallelMethods();
    }

    public boolean isParallelOptimization()
    {
        return parallelOptimization;
    }

    @Override
    public String toString()
    {
        return "parallel='" + parallel + '\'' + ", perCoreThreadCount=" + perCoreThreadCount + ", threadCount="
            + threadCount + ", useUnlimitedThreads=" + useUnlimitedThreads + ", threadCountSuites=" + threadCountSuites
            + ", threadCountClasses=" + threadCountClasses + ", threadCountMethods=" + threadCountMethods
            + ", parallelOptimization=" + parallelOptimization;
    }

    private static boolean property( Map<String, String> properties, String key, boolean fallback )
    {
        return properties.containsKey( key ) ? Boolean.valueOf( properties.get( key ) ) : fallback;
    }

    private static String property( Map<String, String> properties, String key, String fallback )
    {
        return properties.containsKey( key ) ? properties.get( key ) : fallback;
    }

    private static int property( Map<String, String> properties, String key, int fallback )
    {
        return properties.containsKey( key ) ? Integer.valueOf( properties.get( key ) ) : fallback;
    }

    private static double property( Map<String, String> properties, String key, double fallback )
    {
        return properties.containsKey( key ) ? Double.valueOf( properties.get( key ) ) : fallback;
    }
}
