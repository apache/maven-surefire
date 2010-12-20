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

import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Contains all the tests that have been found according to specified include/exclude
 * specification for a given surefire run.
 *
 * @author Kristian Rosenvold (junit core adaption)
 */
public class TestsToRun
{
    private final List locatedClasses;

    /**
     * Constructor
     *
     * @param locatedClasses A list of java.lang.Class objects representing tests to run
     */
    public TestsToRun( List locatedClasses )
    {
        this.locatedClasses = Collections.unmodifiableList( locatedClasses );
        Set testSets = new HashSet();

        for ( Iterator iterator = locatedClasses.iterator(); iterator.hasNext(); )
        {
            Class testClass = (Class) iterator.next();
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
        return new TestsToRun( Arrays.asList( new Class[]{ clazz } ) );
    }

    public int size()
    {
        return locatedClasses.size();
    }

    public Class[] getLocatedClasses()
    {
        return (Class[]) locatedClasses.toArray( new Class[locatedClasses.size()] );
    }

    /**
     * Returns an iterator over the located java.lang.Class objects
     *
     * @return an unmodifiable iterator
     */
    public Iterator iterator()
    {
        return locatedClasses.iterator();
    }
}
