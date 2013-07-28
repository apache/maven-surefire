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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.StringTokenizer;

import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * <p>
 * Common <code>String</code> manipulation routines.
 * </p>
 * <p/>
 * <p>
 * Originally from <a href="http://jakarta.apache.org/turbine/">Turbine</a> and the GenerationJavaCore library.
 * </p>
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:gcoladonato@yahoo.com">Greg Coladonato</a>
 * @author <a href="mailto:bayard@generationjava.com">Henri Yandell</a>
 * @author <a href="mailto:ed@codehaus.org">Ed Korthof</a>
 * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
 * @author Stephen Colebourne
 * @author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck</a>
 * @author Holger Krauth
 * @author <a href="mailto:alex@purpletech.com">Alexander Day Chaffee</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: StringUtils.java 8001 2009-01-03 13:17:09Z vsiveton $
 * @noinspection JavaDoc
 *               <p/>
 *               A quick borrow from plexus-utils by Kristian Rosenvold, to restore jdk1.3 compat Threw away all the
 *               unused stuff.
 *               <p/>
 *               NOTE: This class is not part of any api and is public purely for technical reasons !
 * @since 1.0
 */
public class StringUtils
{
    private static final byte[] HEX_CHARS = new byte[] {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F' };


    public static String[] split( String text, String separator )
    {
        int max = -1;
        StringTokenizer tok;
        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( text );
        }
        else
        {
            tok = new StringTokenizer( text, separator );
        }

        int listSize = tok.countTokens();
        if ( ( max > 0 ) && ( listSize > max ) )
        {
            listSize = max;
        }

        String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin;
        int lastTokenEnd = 0;
        while ( tok.hasMoreTokens() )
        {
            if ( ( max > 0 ) && ( i == listSize - 1 ) )
            {
                // In the situation where we hit the max yet have
                // tokens left over in our input, the last list
                // element gets all remaining text.
                String endToken = tok.nextToken();
                lastTokenBegin = text.indexOf( endToken, lastTokenEnd );
                list[i] = text.substring( lastTokenBegin );
                break;
            }
            else
            {
                list[i] = tok.nextToken();
                lastTokenBegin = text.indexOf( list[i], lastTokenEnd );
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }
        return list;
    }

    /**
     * <p>
     * Checks if a (trimmed) String is <code>null</code> or blank.
     * </p>
     *
     * @param str the String to check
     * @return <code>true</code> if the String is <code>null</code>, or length zero once trimmed
     */
    public static boolean isBlank( String str )
    {
        return ( ( str == null ) || ( str.trim().length() == 0 ) );
    }

    // Ripped from commons-lang StringEscapeUtils. Maybe Use dependency instead
    public static void unescapeJava( StringWriter out, String str )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "The Writer must not be null" );
        }
        if ( str == null )
        {
            return;
        }
        int sz = str.length();
        StringBuffer unicode = new StringBuffer( 4 );
        boolean hadSlash = false;
        boolean inUnicode = false;
        for ( int i = 0; i < sz; i++ )
        {
            char ch = str.charAt( i );
            if ( inUnicode )
            {
                // if in unicode, then we're reading unicode
                // values in somehow
                unicode.append( ch );
                if ( unicode.length() == 4 )
                {
                    // unicode now contains the four hex digits
                    // which represents our unicode character
                    try
                    {
                        int value = Integer.parseInt( unicode.toString(), 16 );
                        out.write( (char) value );
                        unicode.setLength( 0 );
                        inUnicode = false;
                        hadSlash = false;
                    }
                    catch ( NumberFormatException nfe )
                    {
                        throw new NestedRuntimeException( "Unable to parse unicode value: " + unicode, nfe );
                    }
                }
                continue;
            }
            if ( hadSlash )
            {
                // handle an escaped value
                hadSlash = false;
                switch ( ch )
                {
                    case '\\':
                        out.write( '\\' );
                        break;
                    case '\'':
                        out.write( '\'' );
                        break;
                    case '\"':
                        out.write( '"' );
                        break;
                    case 'r':
                        out.write( '\r' );
                        break;
                    case 'f':
                        out.write( '\f' );
                        break;
                    case 't':
                        out.write( '\t' );
                        break;
                    case 'n':
                        out.write( '\n' );
                        break;
                    case 'b':
                        out.write( '\b' );
                        break;
                    case 'u':
                    {
                        // uh-oh, we're in unicode country....
                        inUnicode = true;
                        break;
                    }
                    default:
                        out.write( ch );
                        break;
                }
                continue;
            }
            else if ( ch == '\\' )
            {
                hadSlash = true;
                continue;
            }
            out.write( ch );
        }
        if ( hadSlash )
        {
            // then we're in the weird case of a \ at the end of the
            // string, let's output it anyway.
            out.write( '\\' );
        }
    }



    // Ripped from commons-lang StringEscapeUtils. With a minor modification, we unicode-quote commas
    // to avoid csv decoding problems ;)

    /**
     * Courtesy of commons-lang StringEscapeUtils, slightly modified, see below
     *
     * @param str String to escape values in, may be null
     * @return the escaped string
     */
    public static void escapeJavaStyleString( StringBuffer target, String str )
    {
        if ( str == null )
        {
            return;
        }
        try
        {
            StringWriter writer = new StringWriter( str.length() * 2 );
            escapeJavaStyleString( writer, str, true );
            target.append( writer.toString() ); // todo: be bit smarter
        }
        catch ( IOException ioe )
        {
            // this should never ever happen while writing to a StringWriter
            ioe.printStackTrace();
        }
    }

    /**
     * @param out write to receieve the escaped string
     * @param str String to escape values in, may be null
     * @param escapeSingleQuote escapes single quotes if <code>true</code>
     * @throws java.io.IOException if an IOException occurs
     */
    public static void escapeJavaStyleString( Writer out, String str, boolean escapeSingleQuote )
        throws IOException
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "The Writer must not be null" );
        }
        if ( str == null )
        {
            return;
        }
        int sz;
        sz = str.length();
        for ( int i = 0; i < sz; i++ )
        {
            char ch = str.charAt( i );

            // handle unicode
            if ( ch > 0xfff )
            {
                out.write( "\\u" + hex( ch ) );
            }
            else if ( ch > 0xff )
            {
                out.write( "\\u0" + hex( ch ) );
            }
            else if ( ch > 0x7f || ch == ',' )
            { // Kr - this line modified from commons
                out.write( "\\u00" + hex( ch ) );
            }
            else if ( ch < 32 )
            {
                switch ( ch )
                {
                    case '\b':
                        out.write( '\\' );
                        out.write( 'b' );
                        break;
                    case '\n':
                        out.write( '\\' );
                        out.write( 'n' );
                        break;
                    case '\t':
                        out.write( '\\' );
                        out.write( 't' );
                        break;
                    case '\f':
                        out.write( '\\' );
                        out.write( 'f' );
                        break;
                    case '\r':
                        out.write( '\\' );
                        out.write( 'r' );
                        break;
                    default:
                        if ( ch > 0xf )
                        {
                            out.write( "\\u00" + hex( ch ) );
                        }
                        else
                        {
                            out.write( "\\u000" + hex( ch ) );
                        }
                        break;
                }
            }
            else
            {
                switch ( ch )
                {
                    case '\'':
                        if ( escapeSingleQuote )
                        {
                            out.write( '\\' );
                        }
                        out.write( '\'' );
                        break;
                    case '"':
                        out.write( '\\' );
                        out.write( '"' );
                        break;
                    case '\\':
                        out.write( '\\' );
                        out.write( '\\' );
                        break;
                    case '/':
                        out.write( '\\' );
                        out.write( '/' );
                        break;
                    default:
                        out.write( ch );
                        break;
                }
            }
        }
    }


    public static String hex( char ch )
    {
        return Integer.toHexString( ch ).toUpperCase();
    }


    /**
     * Escapes the bytes in the array {@code str} to contain only 'printable' bytes.
     * <p>
     * Escaping is done by encoding the non-nicely printable bytes to {@code '\' + upperCaseHexBytes(byte)}.
     * <p>
     * A save length of {@code out} is {@code len * 3 + outoff}.
     * <p>
     * The reverse-method is {@link #unescapeBytes(byte[], String)}.
     *
     * @param out output buffer
     * @param outoff offset in the output buffer
     * @param input input buffer
     * @param off offset in the input buffer
     * @param len number of bytes to copy from the input buffer
     * @return number of bytes written to {@code out}
     */
    public static int escapeBytesToPrintable( byte[] out, int outoff, byte[] input, int off, int len )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "The output array must not be null" );
        }
        if ( input == null || input.length == 0 )
        {
            return 0;
        }
        int outputPos = outoff;
        int end = off + len;
        for ( int i = off; i < end; i++ )
        {
            byte b = input[i];

            // handle non-nicely printable bytes
            if ( b < 32 || b > 126 || b == '\\' )
            {
                int upper = ( 0xF0 & b ) >> 4;
                int lower = ( 0x0F & b );
                out[outputPos++] = '\\';
                out[outputPos++] = HEX_CHARS[upper];
                out[outputPos++] = HEX_CHARS[lower];
            }
            else
            {
                out[outputPos++] = b;
            }
        }

        return outputPos - outoff;
    }


    /**
     * Reverses the effect of {@link #escapeBytesToPrintable(byte[], int, byte[], int, int)}.
     * <p>
     * A save length of {@code out} is {@code str.length()}
     *
     * @param out the target byte array
     * @param str the input String
     * @return the number of bytes written to {@code out}
     */
    public static int unescapeBytes( byte[] out, String str )
    {
        int outPos = 0;
        if ( out == null )
        {
            throw new IllegalArgumentException( "The output array must not be null" );
        }
        if ( str == null )
        {
            return 0;
        }
        for ( int i = 0; i < str.length(); i++ )
        {
            char ch = str.charAt( i );

            if (ch == '\\') {
                int upper = fromHex( str.charAt( ++i ));
                int lower = fromHex( str.charAt( ++i ));
                out[outPos++] = (byte) (upper << 4 | lower);
            }
            else {
                out[outPos++] = (byte) ch;
            }
        }
        return outPos;
    }

    private static int fromHex( char c )
    {
        if ( c <= '9' ) {
            return c - '0';
        } else{
            return (c - 'A') + 10;
        }
    }
}
