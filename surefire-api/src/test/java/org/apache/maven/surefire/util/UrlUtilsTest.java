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
import java.io.IOException;
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

    public void testTestNoSpecialCharacters()
        throws IOException
    {
        File f = new File( homeDir, "foo.txt" );
        assertEquals( new URL( "file:" + homeDir + "/foo.txt" ), UrlUtils.getURL( f ) );
        f = new File( homeDir, "qwertyuiopasdfghjklzxcvbnm.txt" );
        assertEquals( new URL( "file:" + homeDir + "/qwertyuiopasdfghjklzxcvbnm.txt" ), UrlUtils.getURL( f ) );
        f = new File( homeDir, "QWERTYUIOPASDFGHJKLZXCVBNM.txt" );
        assertEquals( new URL( "file:" + homeDir + "/QWERTYUIOPASDFGHJKLZXCVBNM.txt" ), UrlUtils.getURL( f ) );
        f = new File( homeDir, "1234567890.txt" );
        assertEquals( new URL( "file:" + homeDir + "/1234567890.txt" ), UrlUtils.getURL( f ) );
        f = new File( homeDir, ")('*~!._-.txt" );
        assertEquals( new URL( "file:" + homeDir + "/)('*~!._-.txt" ), UrlUtils.getURL( f ) );
    }

    public void testTestWithSpaces()
        throws IOException
    {
        File f = new File( homeDir, "foo bar.txt" );
        assertEquals( new URL( "file:" + homeDir + "/foo%20bar.txt" ), UrlUtils.getURL( f ) );
    }

}
