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

import java.util.*;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class PropertiesWrapperTest
    extends TestCase
{
    public void testAddList()
        throws Exception
    {
        PropertiesWrapper propertiesWrapper = new PropertiesWrapper( new HashMap<String, String>() );
        List<String> items = new ArrayList<>();
        items.add( "String1" );
        items.add( "String2,String3" );
        items.add( "String4" );
        items.add( "String5," );
        propertiesWrapper.addList( items, "Test" );

        final List test = propertiesWrapper.getStringList( "Test" );
        assertEquals( 5, test.size() );
        assertEquals( "String5", test.get( 4 ) );
        assertEquals( "String3", test.get( 2 ) );
        assertEquals( "String2", test.get( 1 ) );

    }

    private static final String DUMMY_PREFIX = "dummyPrefix";

    private static final String FIRST_ELEMENT = "foo0";

    private static final String SECOND_ELEMENT = "foo1";

    private final Map<String, String> properties = new HashMap<>();

    private final PropertiesWrapper mapper = new PropertiesWrapper( properties );

    private final Classpath classpathWithTwoElements = createClasspathWithTwoElements();

    public void testReadFromProperties()
        throws Exception
    {
        properties.put( DUMMY_PREFIX + "0", FIRST_ELEMENT );
        properties.put( DUMMY_PREFIX + "1", SECOND_ELEMENT );
        Classpath recreatedClasspath = readClasspathFromProperties();
        assertEquals( classpathWithTwoElements, recreatedClasspath );
    }

    public void testReadFromPropertiesWithEmptyProperties()
        throws Exception
    {
        Classpath recreatedClasspath = readClasspathFromProperties();
        assertTrue( recreatedClasspath.getClassPath().isEmpty() );
    }

    public void testWriteToProperties()
        throws Exception
    {
        mapper.setClasspath( DUMMY_PREFIX, classpathWithTwoElements );
        assertEquals( FIRST_ELEMENT, mapper.getProperty( DUMMY_PREFIX + "0" ) );
        assertEquals( SECOND_ELEMENT, mapper.getProperty( DUMMY_PREFIX + "1" ) );
    }

    public void testRoundtrip()
        throws Exception
    {
        mapper.setClasspath( DUMMY_PREFIX, classpathWithTwoElements );
        Classpath recreatedClasspath = readClasspathFromProperties();
        assertEquals( classpathWithTwoElements, recreatedClasspath );
    }

    private Classpath createClasspathWithTwoElements()
    {
        return Classpath.emptyClasspath()
                        .addClassPathElementUrl( FIRST_ELEMENT )
                        .addClassPathElementUrl( SECOND_ELEMENT );
    }

    private Classpath readClasspathFromProperties()
    {
        return mapper.getClasspath( DUMMY_PREFIX );
    }
}
