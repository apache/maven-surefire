package org.apache.maven.surefire.group.match;

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

import java.util.Collection;

/**
 * OR group matcher
 *
 */
public class OrGroupMatcher
    extends JoinGroupMatcher
{

    public OrGroupMatcher( GroupMatcher... matchers )
    {
        for ( GroupMatcher matcher : matchers )
        {
            addMatcher( matcher );
        }
    }

    public OrGroupMatcher( Collection<GroupMatcher> matchers )
    {
        for ( GroupMatcher matcher : matchers )
        {
            addMatcher( matcher );
        }
    }

    @Override
    public boolean enabled( Class<?>... cats )
    {
        for ( GroupMatcher matcher : getMatchers() )
        {
            boolean result = matcher.enabled( cats );
            if ( result )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean enabled( String... cats )
    {
        for ( GroupMatcher matcher : getMatchers() )
        {
            boolean result = matcher.enabled( cats );
            if ( result )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for ( GroupMatcher matcher : getMatchers() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( " OR " );
            }
            sb.append( matcher );
        }

        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 37;
        int result = 1;
        result = prime * result;
        for ( GroupMatcher matcher : getMatchers() )
        {
            result += matcher.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        AndGroupMatcher other = (AndGroupMatcher) obj;
        return getMatchers().equals( other.getMatchers() );
    }
}
