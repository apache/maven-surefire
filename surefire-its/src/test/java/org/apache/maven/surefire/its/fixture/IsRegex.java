package org.apache.maven.surefire.its.fixture;

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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * Java Hamcrest Matcher with Regex.
 */
public final class IsRegex
        extends BaseMatcher<Set<String>>
{
    public static Matcher<Set<String>> regex( Set<String> expectedRegex )
    {
        return new IsRegex( expectedRegex );
    }

    public static Matcher<Set<String>> regex( String expectedRegex )
    {
        return new IsRegex( expectedRegex );
    }

    private final Set<String> expectedRegex;

    private IsRegex( String expectedRegex )
    {
        this.expectedRegex = singleton( expectedRegex );
    }

    private IsRegex( Set<String> expectedRegex )
    {
        this.expectedRegex = expectedRegex;
    }

    @Override
    public boolean matches( Object o )
    {
        if ( o != null
                && expectedRegex.size() == 1 ? isStringOrSet( o ) : isSet( o ) )
        {
            //noinspection unchecked
            Set<String> actual = isSet( o ) ? ( Set<String> ) o : singleton( ( String ) o );
            boolean matches = actual.size() == expectedRegex.size();
            Iterator<String> regex = expectedRegex.iterator();
            for ( String s : actual )
            {
                if ( s == null || !regex.hasNext() || !s.matches( regex.next() ) )
                {
                    matches = false;
                }
            }
            return matches;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendValue( expectedRegex );
    }

    private static boolean isStringOrSet( Object o )
    {
        return o instanceof String || o instanceof Set;
    }

    private static boolean isSet( Object o )
    {
        return o instanceof Set;
    }
}
