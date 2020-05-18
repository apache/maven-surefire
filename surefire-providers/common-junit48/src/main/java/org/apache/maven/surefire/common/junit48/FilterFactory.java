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

import org.apache.maven.surefire.api.booter.ProviderParameterNames;
import org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.Map;

import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_GROUPS_PROP;
import static org.apache.maven.surefire.shared.utils.StringUtils.isNotBlank;

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

    /**
     * @return {@code true} if non-blank
     * {@link ProviderParameterNames#TESTNG_GROUPS_PROP} and/or
     * {@link ProviderParameterNames#TESTNG_EXCLUDEDGROUPS_PROP} exists.
     */
    public boolean canCreateGroupFilter( Map<String, String> providerProperties )
    {
        String groups = providerProperties.get( TESTNG_GROUPS_PROP );
        String excludedGroups = providerProperties.get( TESTNG_EXCLUDEDGROUPS_PROP );
        return isNotBlank( groups ) || isNotBlank( excludedGroups );
    }

    /**
     * Creates filter using he key
     * {@link ProviderParameterNames#TESTNG_GROUPS_PROP} and/or
     * {@link ProviderParameterNames#TESTNG_EXCLUDEDGROUPS_PROP}.
     */
    public Filter createGroupFilter( Map<String, String> providerProperties )
    {
        String groups = providerProperties.get( TESTNG_GROUPS_PROP );

        GroupMatcher included = null;
        if ( isNotBlank( groups ) )
        {
            try
            {
                included = new GroupMatcherParser( groups ).parse();
            }
            catch ( ParseException e )
            {
                throw new IllegalArgumentException(
                    "Invalid group expression: '" + groups + "'. Reason: " + e.getMessage(), e );
            }
        }

        String excludedGroups = providerProperties.get( TESTNG_EXCLUDEDGROUPS_PROP );

        GroupMatcher excluded = null;
        if ( isNotBlank( excludedGroups ) )
        {
            try
            {
                excluded = new GroupMatcherParser( excludedGroups ).parse();
            }
            catch ( ParseException e )
            {
                throw new IllegalArgumentException(
                    "Invalid group expression: '" + excludedGroups + "'. Reason: " + e.getMessage(), e );
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

    public Filter createMethodFilter( TestListResolver resolver )
    {
        return new MethodFilter( resolver );
    }

    public Filter createMatchAnyDescriptionFilter( Iterable<Description> descriptions )
    {
        return JUnit4ProviderUtil.createMatchAnyDescriptionFilter( descriptions );
    }

    public Filter and( Filter filter1, Filter filter2 )
    {
        return new AndFilter( filter1, filter2 );
    }
}
