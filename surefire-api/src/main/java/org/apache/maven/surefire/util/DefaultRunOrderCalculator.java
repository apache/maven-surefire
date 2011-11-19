package org.apache.maven.surefire.util;

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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Applies the final unorder of the tests
 * 
 * @author Kristian Rosenvold
 */
public class DefaultRunOrderCalculator
    implements RunOrderCalculator
{
    private final Comparator sortOrder;

    private final RunOrder runOrder;

    public DefaultRunOrderCalculator( RunOrder runOrder )
    {
        this.runOrder = runOrder;
        this.sortOrder = getSortOrderComparator();
    }

  public TestsToRun orderTestClasses( TestsToRun scannedClasses ){
    List result = new ArrayList(Arrays.asList(scannedClasses.getLocatedClasses()));
    orderTestClasses(result);
    return new TestsToRun( result );

  }

  private void orderTestClasses( List testClasses )
    {
        if ( RunOrder.RANDOM.equals( runOrder ) )
        {
            Collections.shuffle( testClasses );
        }
        else if ( sortOrder != null )
        {
            Collections.sort( testClasses, sortOrder );
        }
    }

    private Comparator getSortOrderComparator()
    {
        if ( RunOrder.ALPHABETICAL.equals( runOrder ) )
        {
            return getAlphabeticalComparator();
        }
        else if ( RunOrder.REVERSE_ALPHABETICAL.equals( runOrder ) )
        {
            return getReverseAlphabeticalComparator();
        }
        else if ( RunOrder.HOURLY.equals( runOrder ) )
        {
            final int hour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY );
            return ( ( hour % 2 ) == 0 ) ? getAlphabeticalComparator() : getReverseAlphabeticalComparator();
        }
        else
        {
            return null;
        }
    }

    private Comparator getReverseAlphabeticalComparator()
    {
        return new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                return ( (Class) o2 ).getName().compareTo( ( (Class) o1 ).getName() );
            }
        };
    }

    private Comparator getAlphabeticalComparator()
    {
        return new Comparator()
        {
            public int compare( Object o1, Object o2 )
            {
                return ( (Class) o1 ).getName().compareTo( ( (Class) o2 ).getName() );
            }
        };
    }

}
