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

import org.apache.maven.surefire.testset.ResolvedTest;
import org.apache.maven.surefire.testset.TestListResolver;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.Collection;
import java.util.LinkedHashSet;

final class MethodFilter
    extends Filter
{
    private final CombinedCategoryFilter combinedFilter;

    MethodFilter( String requestString )
    {
        this( new TestListResolver( requestString ) );
    }

    MethodFilter( TestListResolver testResolver )
    {
        Collection<Filter> includedFilters = new LinkedHashSet<>();
        Collection<Filter> excludedFilters = new LinkedHashSet<>();
        for ( ResolvedTest test : testResolver.getIncludedPatterns() )
        {
            includedFilters.add( new RequestedTest( test, true ) );
        }
        for ( ResolvedTest test : testResolver.getExcludedPatterns() )
        {
            excludedFilters.add( new RequestedTest( test, false ) );
        }
        combinedFilter = new CombinedCategoryFilter( includedFilters, excludedFilters );
    }

    @Override
    public boolean shouldRun( Description description )
    {
        if ( description.isEmpty() )
        {
            return false;
        }
        else if ( description.isTest() )
        {
            return combinedFilter.shouldRun( description );
        }
        else
        {
            for ( Description o : description.getChildren() )
            {
                if ( combinedFilter.shouldRun( o ) || shouldRun( o ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String describe()
    {
        return combinedFilter.describe();
    }
}
