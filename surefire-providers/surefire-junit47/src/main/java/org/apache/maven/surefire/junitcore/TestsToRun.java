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
package org.apache.maven.surefire.junitcore;

import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Contains all the tests that have been found according to specified include/exclude
 * specification for a given surefire run.
 *
 * @author Kristian Rosenvold (junit core adaption)
 */
class TestsToRun
{
    private final Class[] locatedClasses;

    private final Set<Class> testSets;

    public TestsToRun( Class... locatedClasses )
    {
        this.locatedClasses = locatedClasses;
        testSets = new HashSet<Class>();
        for ( Class testClass : locatedClasses )
        {
            if ( testSets.contains( testClass ) )
            {
                throw new RuntimeException( "Duplicate test set '" + testClass.getName() + "'" );
            }
            testSets.add( testClass );
        }
    }

    private TestsToRun( String className, ClassLoader classLoader )
        throws ClassNotFoundException
    {
        this( classLoader.loadClass( className ) );
    }

    public static TestsToRun fromClassName( String className, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            return new TestsToRun( className, classLoader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( e );
        }
    }

    public Set<Class> getTestSets()
    {
        return Collections.unmodifiableSet( testSets );
    }

    public int size()
    {
        return testSets.size();
    }

    public Class[] getLocatedClasses()
    {
        return locatedClasses;
    }

    public Iterator iterator()
    {
        return testSets.iterator();
    }
}
