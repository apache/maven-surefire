package org.apache.maven.surefire.common.junit48;

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

import org.apache.maven.surefire.api.testset.ResolvedTest;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

final class RequestedTest
    extends Filter
{
    private static final String CLASS_FILE_EXTENSION = ".class";

    private final ResolvedTest test;
    private final boolean isPositiveFilter;

    RequestedTest( ResolvedTest test, boolean isPositiveFilter )
    {
        this.test = test;
        this.isPositiveFilter = isPositiveFilter;
    }

    @Override
    public boolean shouldRun( Description description )
    {
        Class<?> realTestClass = description.getTestClass();
        String methodName = description.getMethodName();
        if ( realTestClass == null && methodName == null )
        {
            return true;
        }
        else
        {
            String testClass = classFile( realTestClass );
            return isPositiveFilter
                ? test.matchAsInclusive( testClass, methodName )
                : !test.matchAsExclusive( testClass, methodName );
        }
    }

    @Override
    public String describe()
    {
        String description = test.toString();
        return description.isEmpty() ? "*" : description;
    }

    @Override
    public boolean equals( Object o )
    {
        return this == o || o != null && getClass() == o.getClass() && equals( (RequestedTest) o );
    }

    private boolean equals( RequestedTest o )
    {
        return isPositiveFilter == o.isPositiveFilter && test.equals( o.test );
    }

    @Override
    public int hashCode()
    {
        return test.hashCode();
    }

    private String classFile( Class<?> realTestClass )
    {
        return realTestClass == null ? null : realTestClass.getName().replace( '.', '/' ) + CLASS_FILE_EXTENSION;
    }
}
