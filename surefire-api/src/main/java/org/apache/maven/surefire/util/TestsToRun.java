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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * Contains all the tests that have been found according to specified include/exclude
 * specification for a given surefire run.
 *
 * @author Kristian Rosenvold (junit core adaption)
 */
public class TestsToRun implements Iterable<Class>
{
    private final List<Class> locatedClasses;

    /**
     * Constructor
     *
     * @param locatedClasses A list of java.lang.Class objects representing tests to run
     */
    public TestsToRun( List<Class> locatedClasses )
    {
        this.locatedClasses = Collections.unmodifiableList( locatedClasses );
        Set<Class> testSets = new HashSet<Class>();

        for ( Class testClass : locatedClasses )
        {
            if ( testSets.contains( testClass ) )
            {
                throw new RuntimeException( "Duplicate test set '" + testClass.getName() + "'" );
            }
            testSets.add( testClass );
        }
    }

    public static TestsToRun fromClass( Class clazz )
        throws TestSetFailedException
    {
        return new TestsToRun( Arrays.<Class>asList( clazz ) );
    }

    /**
     * Returns an iterator over the located java.lang.Class objects
     *
     * @return an unmodifiable iterator
     */
    public Iterator<Class> iterator()
    {
        return locatedClasses.iterator();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "TestsToRun: [" );
        Iterator it = iterator();
        while ( it.hasNext() )
        {
            Class clazz = (Class) it.next();
            sb.append( " " ).append( clazz.getName() );
        }

        sb.append( ']' );
        return sb.toString();
    }

    public boolean containsAtLeast( int atLeast )
    {
        return containsAtLeast( iterator(), atLeast );
    }

    private boolean containsAtLeast( Iterator it, int atLeast )
    {
        for ( int i = 0; i < atLeast; i++ )
        {
            if ( !it.hasNext() )
            {
                return false;
            }

            it.next();
        }

        return true;
    }

    public boolean containsExactly( int items )
    {
        Iterator it = iterator();
        return containsAtLeast( it, items ) && !it.hasNext();
    }

    /**
     * @return {@code true}, if the classes may be read eagerly. {@code false},
     *         if the classes must only be read lazy.
     */
    public boolean allowEagerReading()
    {
        return true;
    }

    public Class[] getLocatedClasses()
    {
        if ( !allowEagerReading() )
        {
            throw new IllegalStateException( "Cannot eagerly read" );
        }
        List<Class> result = new ArrayList<Class>();
        Iterator<Class> it = iterator();
        while ( it.hasNext() )
        {
            result.add( it.next() );
        }
        return result.toArray( new Class[result.size()] );
    }
}
