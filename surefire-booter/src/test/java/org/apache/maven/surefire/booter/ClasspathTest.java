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


import junit.framework.TestCase;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 */
public class ClasspathTest
    extends TestCase
{
    public void testGetClassPath()
        throws Exception
    {
        Classpath classpath = new Classpath();
        classpath.addClassPathElementUrl( "foo.jar" );
        classpath.addClassPathElementUrl( "bar.jar" );
        assertEquals( expected, classpath.getClassPathAsString() );
    }

    public void testGetClassPathNoDupes()
        throws Exception
    {
        Classpath classPath = getWith2DistinctElements();
        assertEquals( expected, classPath.getClassPathAsString() );
    }

    public void testGetClassPathNoDupes2()
        throws Exception
    {
        Classpath classpath = getWith2DistinctElements();
        assertEquals( expected, classpath.append( getWith2DistinctElements() ).getClassPathAsString() );
    }

    final String expected = "foo.jar" + File.pathSeparatorChar + "bar.jar" + File.pathSeparatorChar;

    private Classpath getWith2DistinctElements()
    {
        Classpath classpath = new Classpath();
        classpath.addClassPathElementUrl( "foo.jar" );
        classpath.addClassPathElementUrl( "bar.jar" );
        classpath.addClassPathElementUrl( "foo.jar" );
        return classpath;
    }

    public void testGetAsUrlList()
        throws Exception
    {
        final List asUrlList = getWith2DistinctElements().getAsUrlList();
        assertEquals( 2, asUrlList.size() );
        assertTrue( asUrlList.get( 0 ).toString().endsWith( "foo.jar" ) );
        assertTrue( asUrlList.get( 1 ).toString().endsWith( "bar.jar" ) );
    }

    public void testSetForkProperties()
        throws Exception
    {
        Properties properties = new Properties();
        getWith2DistinctElements().setForkProperties( properties, "test" );
        assertEquals( "foo.jar", properties.get( "test0" ) );
        assertEquals( "bar.jar", properties.get( "test1" ) );
    }

}
