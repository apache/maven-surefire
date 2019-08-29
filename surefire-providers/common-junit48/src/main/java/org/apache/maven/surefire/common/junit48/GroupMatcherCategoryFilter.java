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

import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.addAll;
import static org.junit.runner.Description.createSuiteDescription;

final class GroupMatcherCategoryFilter extends Filter
{
    /**
     * Only traverse the tree if <code>@Category</code> annotation is inherited (since <code>junit 4.12</code>).
     */
    private static final boolean IS_CATEGORY_INHERITED = Category.class.isAnnotationPresent( Inherited.class );

    private final AndGroupMatcher matcher;

    GroupMatcherCategoryFilter( GroupMatcher included, GroupMatcher excluded )
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
        if ( invalidTestClass( description ) )
        {
            return shouldRun( description, null, null );
        }

        if ( describesTestClass( description ) ) // is a test class
        {
            Class<?> testClass = description.getTestClass();
            return shouldRun( description, null, testClass );
        }
        else
        // is a test method
        {
            Class<?> testClass = description.getTestClass();
            return shouldRun( description, createSuiteDescription( testClass ), testClass );
        }
    }

    private boolean describesTestClass( Description description )
    {
        String methodName = description.getMethodName();
        // Description parser in Junit 4.8 can return "null" String.
        return methodName == null || methodName.equals( "null" );
    }

    private boolean invalidTestClass( Description description )
    {
        return description.getTestClass() == null;
    }

    private static void findSuperclassCategories( Set<Class<?>> cats, Class<?> clazz )
    {
        if ( IS_CATEGORY_INHERITED && hasSuperclass( clazz ) )
        {
            Category cat = clazz.getSuperclass().getAnnotation( Category.class );
            if ( cat != null )
            {
                // Found categories in current superclass
                addAll( cats, cat.value() );
            }
            // Search the hierarchy
            findSuperclassCategories( cats, clazz.getSuperclass() );
        }
    }

    private static boolean hasSuperclass( Class<?> clazz )
    {
        return clazz != null && clazz.getSuperclass() != null;
    }

    private boolean shouldRun( Description description, Description parent, Class<?> parentClass )
    {
        if ( matcher == null )
        {
            return true;
        }
        else
        {
            Set<Class<?>> cats = new HashSet<>();
            Category cat = description.getAnnotation( Category.class );
            if ( cat != null )
            {
                // Found categories in current description
                addAll( cats, cat.value() );
            }

            if ( parent != null )
            {
                cat = parent.getAnnotation( Category.class );
                if ( cat != null )
                {
                    // Found categories in current parent
                    addAll( cats, cat.value() );
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
                    // Found categories in current testClass
                    addAll( cats, cat.value() );
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
