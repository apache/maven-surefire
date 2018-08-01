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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import org.apache.maven.surefire.util.internal.StringUtils.EncodedArray;

import static org.junit.Assert.assertArrayEquals;

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

        EncodedArray encodedArray = StringUtils.escapeBytesToPrintable( new byte[0], input, 0, input.length );

        String escapedString = new String( encodedArray.getArray(), 0, encodedArray.getSize() );

        assertEquals( encodedArray.getSize(), escapedString.length() );

        ByteBuffer unescaped = StringUtils.unescapeBytes( escapedString, Charset.defaultCharset().name() );

        assertEquals( input.length + 1, unescaped.remaining() - unescaped.position() );

        for ( int i = 0; i < input.length; i++ )
        {
            assertEquals( "At position " + i, input[i], unescaped.get() );
        }
    }

    public void testEscapeWithHeader()
    {
        byte[] header = { (byte) 'a' };
        byte[] input = { (byte) '1' };

        EncodedArray encodedArray = StringUtils.escapeBytesToPrintable( header, input, 0, input.length );
        assertEquals( 3, encodedArray.getSize() );

        byte[] expectedResult = new byte[] { (byte) 'a', (byte) '1', (byte) '\n' };
        byte[] actualResult = new byte[encodedArray.getSize()];
        System.arraycopy( encodedArray.getArray(), 0, actualResult, 0, encodedArray.getSize() );

        assertArrayEquals( expectedResult, actualResult );
    }

    public void testEmptyByteArray()
    {
        byte[] header = { (byte) 'a' };
        byte[] input = {};
        EncodedArray encodedArray = StringUtils.escapeBytesToPrintable( header, input, 0, input.length );
        assertEquals( 0, encodedArray.getSize() );
        assertEquals( 0, encodedArray.getArray().length );
    }

    public void testSubstringSmall()
    {
        byte[] header = { (byte) 'a' };
        byte[] input = "PleaseLookAfterThisBear".getBytes();
        EncodedArray encodedArray = StringUtils.escapeBytesToPrintable( header, input,
                "Please".length(), "Look".length() );
        assertEquals( "Look",
                new String( encodedArray.getArray(), 1, encodedArray.getArray().length-1).trim() );
    }

    public void testSubstringLarge()
    {
        byte[] header = { (byte) 'a' };
        byte[] input = "TheQuickBrownFoxJumpsOverTheLazyDog".getBytes();
        EncodedArray encodedArray = StringUtils.escapeBytesToPrintable( header, input,
                "The".length(), "QuickBrownFoxJumpsOverTheLazy".length() );
        assertEquals( "QuickBrownFoxJumpsOverTheLazy",
                new String( encodedArray.getArray(), 1, encodedArray.getArray().length-1).trim() );
    }
}
