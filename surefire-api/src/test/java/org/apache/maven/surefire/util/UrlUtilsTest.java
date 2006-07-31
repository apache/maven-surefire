package org.apache.maven.surefire.util;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public void testTestNoSpecialCharacters()
        throws IOException
    {
        File f = new File( "C:/Temp/foo.txt" );
        assertEquals( new URL( "file:/C:/Temp/foo.txt" ), UrlUtils.getURL( f ) );
        f = new File( "C:/Temp/qwertyuiopasdfghjklzxcvbnm.txt" );
        assertEquals( new URL( "file:/C:/Temp/qwertyuiopasdfghjklzxcvbnm.txt" ), UrlUtils.getURL( f ) );
        f = new File( "C:/Temp/QWERTYUIOPASDFGHJKLZXCVBNM.txt" );
        assertEquals( new URL( "file:/C:/Temp/QWERTYUIOPASDFGHJKLZXCVBNM.txt" ), UrlUtils.getURL( f ) );
        f = new File( "C:/Temp/1234567890.txt" );
        assertEquals( new URL( "file:/C:/Temp/1234567890.txt" ), UrlUtils.getURL( f ) );
        f = new File( "C:/Temp/)('*~!._-.txt" );
        assertEquals( new URL( "file:/C:/Temp/)('*~!._-.txt" ), UrlUtils.getURL( f ) );
    }

    public void testTestWithSpaces()
        throws IOException
    {
        File f = new File( "C:/Temp/foo bar.txt" );
        assertEquals( new URL( "file:/C:/Temp/foo%20bar.txt" ), UrlUtils.getURL( f ) );
    }

}
