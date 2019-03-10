package org.apache.maven.surefire.testng.utils;

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
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Method selector delegating to {@link GroupMatcher} to decide if a method is included or not.
 *
 */
public class GroupMatcherMethodSelector
    implements IMethodSelector
{

    private static final long serialVersionUID = 1L;

    private static GroupMatcher matcher;

    private Map<ITestNGMethod, Boolean> answers = new HashMap<>();

    @Override
    public boolean includeMethod( IMethodSelectorContext context, ITestNGMethod method, boolean isTestMethod )
    {
        Boolean result = answers.get( method );
        if ( result != null )
        {
            return result;
        }

        if ( matcher == null )
        {
            return true;
        }

        String[] groups = method.getGroups();
        result = matcher.enabled( groups );
        answers.put( method, result );
        return result;
    }

    @Override
    public void setTestMethods( List<ITestNGMethod> testMethods )
    {
    }

    public static void setGroups( String groups, String excludedGroups )
    {
        // System.out.println( "Processing group includes: '" + groups + "'\nExcludes: '" + excludedGroups + "'" );

        try
        {
            AndGroupMatcher matcher = new AndGroupMatcher();
            GroupMatcher in = null;
            if ( groups != null && !groups.trim().isEmpty() )
            {
                in = new GroupMatcherParser( groups ).parse();
            }

            if ( in != null )
            {
                matcher.addMatcher( in );
            }

            GroupMatcher ex = null;
            if ( excludedGroups != null && !excludedGroups.trim().isEmpty() )
            {
                ex = new GroupMatcherParser( excludedGroups ).parse();
            }

            if ( ex != null )
            {
                matcher.addMatcher( new InverseGroupMatcher( ex ) );
            }

            if ( in != null || ex != null )
            {
                // System.out.println( "Group matcher: " + matcher );
                GroupMatcherMethodSelector.matcher = matcher;
            }
        }
        catch ( ParseException e )
        {
            throw new IllegalArgumentException(
                "Cannot parse group includes/excludes expression(s):\nIncludes: " + groups + "\nExcludes: "
                    + excludedGroups, e );
        }
    }

    public static void setGroupMatcher( GroupMatcher matcher )
    {
        GroupMatcherMethodSelector.matcher = matcher;
    }

}
