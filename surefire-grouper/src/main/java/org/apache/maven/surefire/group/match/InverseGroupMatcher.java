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


/**
 * Inverse group matcher
 *
 */
public class InverseGroupMatcher
    implements GroupMatcher
{

    private final GroupMatcher matcher;

    public InverseGroupMatcher( GroupMatcher matcher )
    {
        this.matcher = matcher;
    }

    @Override
    public boolean enabled( Class<?>... cats )
    {
        return cats == null || !matcher.enabled( cats );
    }

    @Override
    public boolean enabled( String... cats )
    {
        return cats == null || !matcher.enabled( cats );
    }

    @Override
    public String toString()
    {
        return "NOT " + matcher;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( matcher == null ? 0 : matcher.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        InverseGroupMatcher other = (InverseGroupMatcher) obj;
        if ( matcher == null )
        {
            if ( other.matcher != null )
            {
                return false;
            }
        }
        else if ( !matcher.equals( other.matcher ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public void loadGroupClasses( ClassLoader cloader )
    {
        matcher.loadGroupClasses( cloader );
    }

}
