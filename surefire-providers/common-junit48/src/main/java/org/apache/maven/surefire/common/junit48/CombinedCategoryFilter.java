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

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.Collection;

final class CombinedCategoryFilter
    extends Filter
{
    private final Collection<Filter> includedFilters;

    private final Collection<Filter> excludedFilters;

    CombinedCategoryFilter( Collection<Filter> includedFilters, Collection<Filter> excludedFilters )
    {
        this.includedFilters = includedFilters;
        this.excludedFilters = excludedFilters;
    }

    @Override
    public boolean shouldRun( Description description )
    {
        return ( includedFilters.isEmpty() || anyFilterMatchesDescription( includedFilters, description ) )
            && ( excludedFilters.isEmpty() || allFiltersMatchDescription( excludedFilters, description ) );
    }

    private boolean anyFilterMatchesDescription( Collection<Filter> filters, Description description )
    {
        for ( Filter f : filters )
        {
            if ( f.shouldRun( description ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean allFiltersMatchDescription( Collection<Filter> filters, Description description )
    {
        for ( Filter f : filters )
        {
            if ( !f.shouldRun( description ) )
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public String describe()
    {
        StringBuilder sb = new StringBuilder();
        if ( !includedFilters.isEmpty() )
        {
            sb.append( "(" );
            sb.append( joinFilters( includedFilters, " OR " ) );
            sb.append( ")" );
            if ( !excludedFilters.isEmpty() )
            {
                sb.append( " AND " );
            }
        }

        if ( !excludedFilters.isEmpty() )
        {
            sb.append( "NOT (" );
            sb.append( joinFilters( includedFilters, " OR " ) );
            sb.append( ")" );
        }

        return sb.toString();
    }

    private String joinFilters( Collection<Filter> filters, String sep )
    {
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        for ( Filter f : filters )
        {
            if ( !isFirst )
            {
                sb.append( sep );
            }
            sb.append( f.describe() );
            isFirst = false;
        }
        return sb.toString();
    }
}
