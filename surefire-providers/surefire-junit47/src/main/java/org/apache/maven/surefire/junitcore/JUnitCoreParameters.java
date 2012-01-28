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

import java.util.Properties;
import org.apache.maven.surefire.booter.ProviderParameterNames;

/**
 * @author Kristian Rosenvold
 */
class JUnitCoreParameters
{
    private final String parallel;

    private final Boolean perCoreThreadCount;

    private final int threadCount;

    private final Boolean useUnlimitedThreads;

    public static final String PARALLEL_KEY = ProviderParameterNames.PARALLEL_PROP;

    public static final String PERCORETHREADCOUNT_KEY = "perCoreThreadCount";

    public static final String THREADCOUNT_KEY = ProviderParameterNames.THREADCOUNT_PROP;

    public static final String USEUNLIMITEDTHREADS_KEY = "useUnlimitedThreads";


    public JUnitCoreParameters( Properties properties )
    {
        this.parallel = properties.getProperty( PARALLEL_KEY, "none" ).toLowerCase();
        this.perCoreThreadCount = Boolean.valueOf( properties.getProperty( PERCORETHREADCOUNT_KEY, "true" ) );
        this.threadCount = Integer.valueOf( properties.getProperty( THREADCOUNT_KEY, "2" ) );
        this.useUnlimitedThreads = Boolean.valueOf( properties.getProperty( USEUNLIMITEDTHREADS_KEY, "false" ) );
    }

    public boolean isParallelMethod()
    {
        return "methods".equals( parallel );
    }

    public boolean isParallelClasses()
    {
        return "classes".equals( parallel );
    }

    public boolean isParallelBoth()
    {
        return "both".equals( parallel );
    }

    public Boolean isPerCoreThreadCount()
    {
        return perCoreThreadCount;
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    public Boolean isUseUnlimitedThreads()
    {
        return useUnlimitedThreads;
    }

    public boolean isNoThreading()
    {
        return !( isParallelClasses() || isParallelMethod() || isParallelBoth() );
    }

    public boolean isAnyParallelitySelected()
    {
        return !isNoThreading();
    }

    @Override
    public String toString()
    {
        return "parallel='" + parallel + '\'' + ", perCoreThreadCount=" + perCoreThreadCount + ", threadCount="
            + threadCount + ", useUnlimitedThreads=" + useUnlimitedThreads;
    }
}
