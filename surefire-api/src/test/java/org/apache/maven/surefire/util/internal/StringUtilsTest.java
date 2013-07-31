package org.apache.maven.surefire.util.internal;

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

/**
 * @author Andreas Gudian
 */
public class StringUtilsTest
    extends TestCase
{

    public void testUnescapeString()
    {
        CharSequence inputString = createInputString();

        StringBuilder escaped = new StringBuilder( inputString.length() * 5 );
        int initialCapacity = escaped.capacity();

        StringUtils.escapeToPrintable( escaped, inputString );

        assertEquals( initialCapacity, escaped.capacity() );

        StringBuilder unescaped = new StringBuilder( inputString.length() );
        StringUtils.unescapeString( unescaped, escaped );

        assertEquals( inputString.length(), unescaped.length() );

        for ( int i = 0; i < inputString.length(); i++ )
        {
            if ( inputString.charAt( i ) != unescaped.charAt( i ) )
            {
                fail( "Input and Unescaped String are not equal at position " + i );
            }
        }
    }

    private CharSequence createInputString()
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < Character.MAX_CODE_POINT; i++ )
        {
            sb.appendCodePoint( i );
        }

        return sb;
    }

    public void testUnescapeBytes()
    {
        byte[] input = new byte[256];

        for ( int i = 0; i <= 0xFF; i++ )
        {
            byte b = (byte) ( 0xFF & i );
            input[i] = b;
        }

        byte[] escaped = new byte[input.length * 3];

        int escapedBytes = StringUtils.escapeBytesToPrintable( escaped, 0, input, 0, input.length );

        String escapedString = new String( escaped, 0, escapedBytes );

        assertEquals( escapedBytes, escapedString.length() );

        byte[] unescaped = new byte[input.length];
        int unescapeBytes = StringUtils.unescapeBytes( unescaped, escapedString );

        assertEquals( input.length, unescapeBytes );

        for ( int i = 0; i < input.length; i++ )
        {
            assertEquals( "At position " + i, input[i], unescaped[i] );
        }
    }
}
