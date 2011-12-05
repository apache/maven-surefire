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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.codehaus.plexus.util.SelectorUtils;

import org.junit.experimental.categories.Categories;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * @author Todd Lipcon
 */
public class FilterFactory
{
    private final ClassLoader testClassLoader;

    public FilterFactory( ClassLoader testClassLoader )
    {
        this.testClassLoader = testClassLoader;
    }

    public Filter createGroupFilter( Properties providerProperties )
    {
        String groups = providerProperties.getProperty( ProviderParameterNames.TESTNG_GROUPS_PROP );
        String excludedGroups = providerProperties.getProperty( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP );
        List<Filter> included = commaSeparatedListToFilters( groups );
        List<Filter> excluded = commaSeparatedListToFilters( excludedGroups );
        return new CombinedCategoryFilter( included, excluded );
    }

    private List<Filter> commaSeparatedListToFilters( String str )
    {
        List<Filter> included = new ArrayList<Filter>();
        if ( str != null )
        {
            for ( String group : str.split( "," ) )
            {
                group = group.trim();
                if ( group == null || group.length() == 0)
                {
                    continue;
                }
                Class<?> categoryType = classloadCategory( group );
                included.add( Categories.CategoryFilter.include( categoryType ) );
            }
        }
        return included;
    }

    public Filter createMethodFilter( String requestedTestMethod )
    {
        return new MethodFilter( requestedTestMethod );
    }

    private static class MethodFilter
        extends Filter
    {
        private final String requestedTestMethod;

        public MethodFilter( String requestedTestMethod )
        {
            this.requestedTestMethod = requestedTestMethod;
        }

        @Override
        public boolean shouldRun( Description description )
        {
            for ( Description o : description.getChildren() )
            {
                if (isDescriptionMatch( o )){
                    return true;
                }
                
            }
            return isDescriptionMatch( description );
        }

        private boolean isDescriptionMatch( Description description )
        {
            return description.getMethodName() != null && SelectorUtils.match( requestedTestMethod,
                                                                               description.getMethodName() );
        }


        @Override
        public String describe()
        {
            return "By method"  + requestedTestMethod;
        }
    }


    private static class CombinedCategoryFilter
        extends Filter
    {
        private final List<Filter> includedFilters;

        private final List<Filter> excludedFilters;

        public CombinedCategoryFilter( List<Filter> includedFilters, List<Filter> excludedFilters )
        {
            this.includedFilters = includedFilters;
            this.excludedFilters = excludedFilters;
        }

        @Override
        public boolean shouldRun( Description description )
        {
            return ( includedFilters.isEmpty() || inOneOfFilters( includedFilters, description ) ) && (
                excludedFilters.isEmpty() || !inOneOfFilters( excludedFilters, description ) );
        }

        private boolean inOneOfFilters( List<Filter> filters, Description description )
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

        private String joinFilters( List<Filter> filters, String sep )
        {
            int i = 0;
            StringBuilder sb = new StringBuilder();
            for ( Filter f : filters )
            {
                if ( i++ > 0 )
                {
                    sb.append( sep );
                }
                sb.append( f.describe() );
            }
            return sb.toString();
        }
    }

    private Class<?> classloadCategory( String category )
    {
        try
        {
            return testClassLoader.loadClass( category );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "Unable to load category: " + category, e );
        }
    }

}
