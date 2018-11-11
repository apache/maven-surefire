package org.apache.maven.surefire.common.junit4;

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

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Only run test methods in the given failure set.
 *
 * @author mpkorstanje
 */
public final class MatchDescriptions
    extends Filter
{
    private final List<Filter> filters = new ArrayList<>();

    public MatchDescriptions( Iterable<Description> descriptions )
    {
        for ( Description description : descriptions )
        {
            filters.add( matchDescription( description ) );
        }
    }

    @Override
    public boolean shouldRun( Description description )
    {
        for ( Filter filter : filters )
        {
            if ( filter.shouldRun( description ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String describe()
    {
        StringBuilder description = new StringBuilder( "Matching description " );
        for ( int i = 0; i < filters.size(); i++ )
        {
            description.append( filters.get( i ).describe() );
            if ( i != filters.size() - 1 )
            {
                description.append( " OR " );
            }
        }
        return description.toString();
    }

    private static Filter matchDescription( final Description desiredDescription )
    {
        return new Filter()
        {
            @Override
            public boolean shouldRun( Description description )
            {
                if ( description.isTest() )
                {
                    return desiredDescription.equals( description );
                }

                for ( Description each : description.getChildren() )
                {
                    if ( shouldRun( each ) )
                    {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public String describe()
            {
                return String.format( "Method %s", desiredDescription.getDisplayName() );
            }
        };
    }
}
