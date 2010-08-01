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

import junit.framework.TestCase;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Test the URL utilities.
 */
public class UrlUtilsTest
    extends TestCase
{
    private String homeDir;

    public void setUp()
        throws Exception
    {
        super.setUp();
        homeDir = System.getProperty( "user.dir" );
        if ( !homeDir.startsWith( "/" ) )
        {
            homeDir = "/" + homeDir;
        }
    }

    private void verifyFileName( String fileName )
        throws Exception
    {
        verifyFileName( fileName, fileName );
    }

    private void verifyFileName( String fileName, String expectedFileName )
        throws Exception
    {
        File f = new File( homeDir, fileName );
        URL u = UrlUtils.getURL( f );
        String url = u.toString();
        assertStartsWith( url, "file:" );
        assertEndsWith( url, expectedFileName );

        try
        {
            // use reflection to do "URI uri = u.toURI()" if JDK 1.5+
            Method toURI = URL.class.getMethod( "toURI", null );
            Object uri = toURI.invoke( u, null );

            // use reflection to do "File urlFile = new File( uri )" if JDK 1.4+
            Constructor newFile = File.class.getConstructor( new Class[] { uri.getClass() } );
            File urlFile = (File) newFile.newInstance( new Object[] { uri } );

            assertEquals( f, urlFile ); 
        }
        catch (NoSuchMethodException e )
        {
            // URL.toURI() method in JDK 1.5+, not available currently
            // we won't be able to check for file equality...
        }
    }

    private void assertStartsWith( String string, String substring )
    {
        assertTrue( "<" + string + "> should start with <" + substring + ">", string.startsWith( substring ) );
    }

    private void assertEndsWith( String string, String substring )
    {
        assertTrue( "<" + string + "> should end with <" + substring + ">", string.endsWith( substring ) );
    }

    public void testTestNoSpecialCharacters()
        throws Exception
    {
        verifyFileName( "foo.txt" );
        verifyFileName( "qwertyuiopasdfghjklzxcvbnm.txt" );
        verifyFileName( "QWERTYUIOPASDFGHJKLZXCVBNM.txt" );
        verifyFileName( "1234567890.txt" );
        verifyFileName( ")('*~!._-.txt" );
    }

    public void testTestWithSpaces()
        throws Exception
    {
        verifyFileName( "foo bar.txt", "foo%20bar.txt" );
    }

    public void testTestWithUmlaut()
        throws Exception
    {
        verifyFileName( "fo\u00DC.txt", "fo%c3%9c.txt" );
    }

}
