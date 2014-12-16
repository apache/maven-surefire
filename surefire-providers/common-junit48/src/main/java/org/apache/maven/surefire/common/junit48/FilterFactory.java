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

import org.apache.maven.shared.utils.io.SelectorUtils;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

        GroupMatcher included = null;
        if ( groups != null && groups.trim().length() > 0 )
        {
            try
            {
                included = new GroupMatcherParser( groups ).parse();
            }
            catch ( ParseException e )
            {
                throw new IllegalArgumentException( "Invalid group expression: '" + groups + "'. Reason: "
                    + e.getMessage(), e );
            }
        }

        GroupMatcher excluded = null;
        if ( excludedGroups != null && excludedGroups.trim().length() > 0 )
        {
            try
            {
                excluded = new GroupMatcherParser( excludedGroups ).parse();
            }
            catch ( ParseException e )
            {
                throw new IllegalArgumentException( "Invalid group expression: '" + excludedGroups + "'. Reason: "
                    + e.getMessage(), e );
            }
        }

        if ( included != null && testClassLoader != null )
        {
            included.loadGroupClasses( testClassLoader );
        }

        if ( excluded != null && testClassLoader != null )
        {
            excluded.loadGroupClasses( testClassLoader );
        }

        return new GroupMatcherCategoryFilter( included, excluded );
    }

    public Filter createMethodFilter( String requestedTestMethod )
    {
        return new MethodFilter( requestedTestMethod );
    }

    public Filter createFailingMethodFilter( Map<Class<?>, Set<String>> failingClassMethodMap )
    {
        return new FailingMethodFilter( failingClassMethodMap );
    }

    public Filter and( Filter filter1, Filter filter2 )
    {
        return new AndFilter( filter1, filter2 );
    }

    private static class MethodFilter
        extends Filter
    {

        private static class RequestedTestMethod
        {
            final String className;
            final String methodName;

            private RequestedTestMethod( String className, String methodName )
            {
                this.className = className;
                this.methodName = methodName;
            }

            private boolean isDescriptionmatch( Description description )
            {
                String describedClassName = description.getClassName();
                String describedMethodName = description.getMethodName();

                System.out.println( "current description " + describedClassName + " " + describedMethodName );
                System.out.println( "trying to match against " + this.className + " "
                        + this.methodName );

                if ( describedClassName != null )
                {
                    if ( this.className.indexOf( '*' ) < 0 && this.className.indexOf( '?' ) < 0 )
                    {
                        // existing implementation seems to be a simple contains check
                        if ( !describedClassName.contains( this.className ) )
                        {
                            return false;
                        }
                    }
                    else
                    {
                        if ( !SelectorUtils.match( this.className, describedClassName ) )
                        {
                            return false;
                        }
                    }
                }

                if ( describedMethodName != null && !SelectorUtils.match( this.methodName, describedMethodName ) )
                {
                    return false;
                }

                return true;
            }
        }

        private final String requestString;
        private final List<RequestedTestMethod> requestedTestMethods;

        public MethodFilter( String requestString )
        {
            List<RequestedTestMethod> requestedTestMethods = new ArrayList<RequestedTestMethod>();

            if ( requestString.indexOf( '#' ) < 0 )
            {
                // old way before SUREFIRE-745, filter only by method name
                // class name filtering is done separately
                requestedTestMethods.add( new RequestedTestMethod( null, requestString ) );
            }
            else
            {
                // possibly several classes and methods separated by comma
                // several methods in the same class separated by plus

                for ( String requestedTestMethod : requestString.split( "," ) )
                {
                    int index = requestedTestMethod.indexOf( '#' );
                    if ( index < 0 )
                    {
                        requestedTestMethods.add( new RequestedTestMethod( null, requestedTestMethod ) );
                    }
                    else
                    {
                        String className = index == 0 ? null : requestedTestMethod.substring( 0, index );
                        for ( String methodName : requestedTestMethod.substring( index + 1 ).split( "\\+" ) )
                        {
                            requestedTestMethods.add( new RequestedTestMethod( className, methodName ) );
                        }
                    }
                }
            }
            System.out.println( requestString );

            this.requestString = requestString;
            this.requestedTestMethods = requestedTestMethods;
        }

        @Override
        public boolean shouldRun( Description description )
        {
            for ( Description o : description.getChildren() )
            {
                if ( isDescriptionMatch( o ) || shouldRun( o ) )
                {
                    return true;
                }

            }
            return isDescriptionMatch( description );
        }

        private boolean isDescriptionMatch( Description description )
        {
            for ( RequestedTestMethod requestedTestMethod : requestedTestMethods )
            {
                if ( requestedTestMethod.isDescriptionmatch( description ) )
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String describe()
        {
            return "By method " + requestString;
        }
    }

    // Only run test methods in the given input map, indexed by test class
    private static class FailingMethodFilter
        extends Filter
    {
        // Map from Class -> List of method names. Are the method names hashed to include the signature?
        private final Map<Class<?>, Set<String>> failingClassMethodMap;

        public FailingMethodFilter( Map<Class<?>, Set<String>> failingClassMethodMap )
        {
            this.failingClassMethodMap = failingClassMethodMap;
        }

        @Override
        public boolean shouldRun( Description description )
        {
            return isDescriptionMatch( description );
        }

        private boolean isDescriptionMatch( Description description )
        {
            if ( description.getTestClass() == null || description.getMethodName() == null )
            {
                for ( Description childrenDescription : description.getChildren() )
                {
                    if ( isDescriptionMatch( childrenDescription ) )
                    {
                        return true;
                    }
                }
                return false;
            }

            Set<String> testMethods = failingClassMethodMap.get( description.getTestClass() );
            return testMethods != null && testMethods.contains( description.getMethodName() );
        }

        @Override
        public String describe()
        {
            return "By failing class method";
        }
    }

    private static class GroupMatcherCategoryFilter
        extends Filter
    {

        private final AndGroupMatcher matcher;

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
            else
            {
                matcher = null;
            }
        }

        @Override
        public boolean shouldRun( Description description )
        {
            if ( description.getMethodName() == null || description.getTestClass() == null )
            {
                return shouldRun( description, null, null );
            }
            else
            {
                return shouldRun( description, Description.createSuiteDescription( description.getTestClass() ),
                                  description.getTestClass() );
            }
        }

        private static void findSuperclassCategories( Set<Class<?>> cats, Class<?> clazz )
        {
            if ( clazz != null && clazz.getSuperclass() != null )
            {
                Category cat = clazz.getSuperclass().getAnnotation( Category.class );
                if ( cat != null )
                {
                    Collections.addAll( cats, cat.value() );
                }
                else
                {
                    findSuperclassCategories( cats, clazz.getSuperclass() );
                }
            }
        }

        private boolean shouldRun( Description description, Description parent, Class<?> parentClass )
        {
            if ( matcher == null )
            {
                return true;
            }
            else
            {
                Set<Class<?>> cats = new HashSet<Class<?>>();
                Category cat = description.getAnnotation( Category.class );
                if ( cat != null )
                {
                    Collections.addAll( cats, cat.value() );
                }

                if ( parent != null )
                {
                    cat = parent.getAnnotation( Category.class );
                    if ( cat != null )
                    {
                        Collections.addAll( cats, cat.value() );
                    }
                }

                if ( parentClass != null )
                {
                    findSuperclassCategories( cats, parentClass );
                }

                Class<?> testClass = description.getTestClass();
                if ( testClass != null )
                {
                    cat = testClass.getAnnotation( Category.class );
                    if ( cat != null )
                    {
                        Collections.addAll( cats, cat.value() );
                    }
                }

                cats.remove( null );

                boolean result = matcher.enabled( cats.toArray( new Class<?>[cats.size()] ) );

                if ( !result )
                {
                    ArrayList<Description> children = description.getChildren();
                    if ( children != null )
                    {
                        for ( Description child : children )
                        {
                            if ( shouldRun( child, description, null ) )
                            {
                                result = true;
                                break;
                            }
                        }
                    }
                }

                return result;
            }
        }

        @Override
        public String describe()
        {
            return matcher == null ? "ANY" : matcher.toString();
        }
    }

    private static class AndFilter
        extends Filter
    {
        private final Filter filter1;

        private final Filter filter2;

        public AndFilter( Filter filter1, Filter filter2 )
        {
            this.filter1 = filter1;
            this.filter2 = filter2;
        }

        @Override
        public boolean shouldRun( Description description )
        {
            return filter1.shouldRun( description ) && filter2.shouldRun( description );
        }

        @Override
        public String describe()
        {
            return filter1.describe() + " AND " + filter2.describe();
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
            return ( includedFilters.isEmpty() || inOneOfFilters( includedFilters, description ) )
                && ( excludedFilters.isEmpty() || !inOneOfFilters( excludedFilters, description ) );
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
}
