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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.apache.maven.surefire.group.match.OrGroupMatcher;
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.codehaus.plexus.util.SelectorUtils;

import org.junit.experimental.categories.Category;
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
        GroupMatcher included = commaSeparatedListToFilters( groups );
        GroupMatcher excluded = commaSeparatedListToFilters( excludedGroups );
        return new GroupMatcherCategoryFilter( included, excluded );
    }

    private GroupMatcher commaSeparatedListToFilters( String str )
    {
        List<GroupMatcher> included = new ArrayList<GroupMatcher>();
        if ( str != null )
        {
            for ( String group : str.split( "," ) )
            {
                group = group.trim();
                if ( group == null || group.length() == 0)
                {
                    continue;
                }

                try
                {
                    GroupMatcher matcher = new GroupMatcherParser( group ).parse();
                    included.add( matcher );
                }
                catch ( ParseException e )
                {
                    // TODO Auto-generated catch block
                    throw new IllegalArgumentException( "Invalid group expression: '" + group + "'. Reason: "
                        + e.getMessage(), e );
                }

                // Class<?> categoryType = classloadCategory( group );
                // included.add( Categories.CategoryFilter.include( categoryType ) );
            }
        }

        return included.isEmpty() ? null : new OrGroupMatcher( included );
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

    private static class GroupMatcherCategoryFilter
        extends Filter
    {

        private AndGroupMatcher matcher;

        public GroupMatcherCategoryFilter( GroupMatcher included, GroupMatcher excluded )
        {
            GroupMatcher invertedExclude = excluded == null ? null : new InverseGroupMatcher( excluded );
            if ( included != null || invertedExclude != null )
            {
                matcher = new AndGroupMatcher();
                if ( included != null )
                {
                    matcher.addMatcher( included );
                }

                if ( invertedExclude != null )
                {
                    matcher.addMatcher( invertedExclude );
                }
            }
        }

        @Override
        public boolean shouldRun( Description description )
        {
            if ( matcher == null )
            {
                return true;
            }

            Category cat = description.getAnnotation( Category.class );
            if ( cat == null )
            {
                cat = description.getTestClass().getAnnotation( Category.class );
            }

            System.out.println( "\n\nMatcher: " + matcher );

            if ( cat == null )
            {
                System.out.println( "\tChecking: " + description.getClassName() + "#" + description.getMethodName()
                    + " with no category annotations... Enabled? " + ( matcher.enabled( (String) null ) ) );
                return matcher.enabled( (String) null );
            }

            System.out.println( "\tChecking: " + description.getClassName() + "#" + description.getMethodName()
                + " with category annotations:\n\t" + Arrays.toString( cat.value() ) + "\nEnabled? "
                + ( matcher.enabled( cat.value() ) ) );

            return matcher.enabled( cat.value() );
        }

        @Override
        public String describe()
        {
            return matcher == null ? "ANY" : matcher.toString();
        }

    }


    @SuppressWarnings( "unused" )
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
