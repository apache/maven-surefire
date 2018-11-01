package org.apache.maven.surefire.booter;

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

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;

/**
 * @author Kristian Rosenvold
 */
public class ClasspathTest
    extends TestCase
{
    private static final String DUMMY_PROPERTY_NAME = "dummyProperty";

    private static final String DUMMY_URL_1 = "foo.jar";

    private static final String DUMMY_URL_2 = "bar.jar";

    public void testShouldWriteEmptyPropertyForEmptyClasspath()
    {
        Classpath classpath = Classpath.emptyClasspath();
        classpath.writeToSystemProperty( DUMMY_PROPERTY_NAME );
        assertEquals( "", System.getProperty( DUMMY_PROPERTY_NAME ) );
    }

    public void testShouldWriteSeparatedElementsAsSystemProperty()
    {
        Classpath classpath = Classpath.emptyClasspath().addClassPathElementUrl( DUMMY_URL_1 ).addClassPathElementUrl( DUMMY_URL_2 );
        classpath.writeToSystemProperty( DUMMY_PROPERTY_NAME );
        assertEquals( DUMMY_URL_1 + File.pathSeparatorChar + DUMMY_URL_2 + File.pathSeparatorChar,
                      System.getProperty( DUMMY_PROPERTY_NAME ) );
    }

    public void testShouldAddNoDuplicateElements()
    {
        Classpath classpath =
            emptyClasspath().addClassPathElementUrl( DUMMY_URL_1 ).addClassPathElementUrl( DUMMY_URL_1 );
        assertClasspathConsistsOfElements( classpath, new String[]{ DUMMY_URL_1 } );
    }

    public void testShouldJoinTwoNullClasspaths()
    {
        Classpath joinedClasspath = Classpath.join( null, null );
        assertEmptyClasspath( joinedClasspath );
    }

    public void testShouldHaveAllElementsAfterJoiningTwoDifferentClasspaths()
    {
        Classpath firstClasspath = Classpath.emptyClasspath();
        Classpath secondClasspath = firstClasspath.addClassPathElementUrl( DUMMY_URL_1 ).addClassPathElementUrl( DUMMY_URL_2 );
        Classpath joinedClasspath = Classpath.join( firstClasspath, secondClasspath );
        assertClasspathConsistsOfElements( joinedClasspath, new String[]{ DUMMY_URL_1, DUMMY_URL_2 } );
    }

    public void testShouldNotHaveDuplicatesAfterJoiningTowClasspathsWithEqualElements()
    {
        Classpath firstClasspath = Classpath.emptyClasspath().addClassPathElementUrl( DUMMY_URL_1 );
        Classpath secondClasspath = Classpath.emptyClasspath().addClassPathElementUrl( DUMMY_URL_1 );
        Classpath joinedClasspath = Classpath.join( firstClasspath, secondClasspath );
        assertClasspathConsistsOfElements( joinedClasspath, new String[]{ DUMMY_URL_1 } );
    }

    public void testShouldNotBeAbleToRemoveElement()
    {
        try
        {
            Classpath classpath = createClasspathWithTwoElements();
            classpath.getClassPath().remove( 0 );
        }
        catch ( java.lang.UnsupportedOperationException ignore )
        {

        }
    }

    private void assertClasspathConsistsOfElements( Classpath classpath, String[] elements )
    {
        List classpathElements = classpath.getClassPath();
        for ( String element : elements )
        {
            assertTrue( "The element '" + element + " is missing.", classpathElements.contains( element ) );
        }
        assertEquals( "Wrong number of classpath elements.", elements.length, classpathElements.size() );
    }

    private void assertEmptyClasspath( Classpath classpath )
    {
        List classpathElements = classpath.getClassPath();
        assertEquals( "Wrong number of classpath elements.", 0, classpathElements.size() );
    }

    private Classpath createClasspathWithTwoElements()
    {
        Classpath classpath = Classpath.emptyClasspath();
        return classpath.addClassPathElementUrl( DUMMY_URL_1 ).addClassPathElementUrl( DUMMY_URL_2 );
    }

    public void testShouldThrowIllegalArgumentExceptionWhenNullIsAddedAsClassPathElementUrl()
    {
        Classpath classpath = Classpath.emptyClasspath();
        try
        {
            classpath.addClassPathElementUrl( null );
            fail( "IllegalArgumentException not thrown." );
        }
        catch ( IllegalArgumentException expected )
        {
        }
    }

    public void testShouldNotAddNullAsClassPathElementUrl()
    {
        Classpath classpath = Classpath.emptyClasspath();
        try
        {
            classpath.addClassPathElementUrl( null );
        }
        catch ( IllegalArgumentException ignored )
        {
        }
        assertEmptyClasspath( classpath );
    }
}
