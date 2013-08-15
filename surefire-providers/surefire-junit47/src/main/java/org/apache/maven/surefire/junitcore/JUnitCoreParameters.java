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
import java.util.Properties;

import org.apache.maven.surefire.booter.ProviderParameterNames;

/**
 * @author Kristian Rosenvold
 */
class JUnitCoreParameters
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

    private final String parallel;

    private final Boolean perCoreThreadCount;

    private final int threadCount;

    private final int threadCountSuites;

    private final int threadCountClasses;

    private final int threadCountMethods;

    private final double parallelTestsTimeoutInSeconds;

    private final double parallelTestsTimeoutForcedInSeconds;

    private final Boolean useUnlimitedThreads;

    public JUnitCoreParameters( Properties properties )
    {
        parallel = properties.getProperty( PARALLEL_KEY, "none" ).toLowerCase();
        perCoreThreadCount = Boolean.valueOf( properties.getProperty( PERCORETHREADCOUNT_KEY, "true" ) );
        threadCount = Integer.valueOf( properties.getProperty( THREADCOUNT_KEY, "0" ) );
        threadCountMethods = Integer.valueOf( properties.getProperty( THREADCOUNTMETHODS_KEY, "0" ) );
        threadCountClasses = Integer.valueOf( properties.getProperty( THREADCOUNTCLASSES_KEY, "0" ) );
        threadCountSuites = Integer.valueOf( properties.getProperty( THREADCOUNTSUITES_KEY, "0" ) );
        useUnlimitedThreads = Boolean.valueOf( properties.getProperty( USEUNLIMITEDTHREADS_KEY, "false" ) );
        parallelTestsTimeoutInSeconds =
            Math.max( Double.valueOf( properties.getProperty( PARALLEL_TIMEOUT_KEY, "0" ) ), 0 );
        parallelTestsTimeoutForcedInSeconds =
            Math.max( Double.valueOf( properties.getProperty( PARALLEL_TIMEOUTFORCED_KEY, "0" ) ), 0 );
    }

    private static Collection<String> lowerCase( String... elements )
    {
        ArrayList<String> lowerCase = new ArrayList<String>();
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

    public boolean isParallelMethod()
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
     * @deprecated Instead use the expression ( {@link #isParallelMethod()} && {@link #isParallelClasses()} ).
     */
    @Deprecated
    public boolean isParallelBoth()
    {
        return isParallelMethod() && isParallelClasses();
    }

    public Boolean isPerCoreThreadCount()
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

    public Boolean isUseUnlimitedThreads()
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
        return !isAnyParallelitySelected();
    }

    public boolean isAnyParallelitySelected()
    {
        return isParallelSuites() || isParallelClasses() || isParallelMethod();
    }

    @Override
    public String toString()
    {
        return "parallel='" + parallel + '\'' + ", perCoreThreadCount=" + perCoreThreadCount + ", threadCount="
            + threadCount + ", useUnlimitedThreads=" + useUnlimitedThreads + ", threadCountSuites=" + threadCountSuites
            + ", threadCountClasses=" + threadCountClasses + ", threadCountMethods=" + threadCountMethods;
    }
}
