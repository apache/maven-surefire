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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Single group matcher
 *
 */
public class SingleGroupMatcher
    implements GroupMatcher
{
    private final String enabled;
    private final Pattern pattern;

    private Class<?> enabledClass;

    public SingleGroupMatcher( String enabled )
    {
        this.enabled = enabled.endsWith( ".class" ) ? enabled.substring( 0, enabled.length() - 6 ) : enabled;
        Pattern p;
        try
        {
            p = Pattern.compile( enabled );
        }
        catch ( PatternSyntaxException e )
        {
            p = null;
        }
        pattern = p;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + enabled.hashCode();
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
        SingleGroupMatcher other = (SingleGroupMatcher) obj;
        return enabled.equals( other.enabled );
    }

    @Override
    public String toString()
    {
        return "*" + enabled;
    }

    public boolean enabled( Class<?>... cats )
    {
        if ( cats != null )
        {
            for ( Class<?> cls : cats )
            {
                if ( enabledClass != null && enabledClass.isAssignableFrom( cls ) )
                {
                    return true;
                }

                String name = cls.getName();
                if ( name.endsWith( enabled ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean enabled( String... cats )
    {
        for ( String cat : cats )
        {
            if ( cat == null || cat.trim().length() < 1 )
            {
                continue;
            }

            if ( cat.endsWith( enabled ) )
            {
                return true;
            }

            if ( pattern != null && pattern.matcher( cat ).matches() )
            {
                return true;
            }
        }

        return false;
    }

    public void loadGroupClasses( ClassLoader classLoader )
    {
        try
        {
            enabledClass = classLoader.loadClass( enabled );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "Unable to load category: " + enabled, e );
        }
    }
}
