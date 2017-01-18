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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

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

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    // 8-bit charset Latin-1
    public static final String FORK_STREAM_CHARSET_NAME = "ISO-8859-1";

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
        if ( max > 0 && listSize > max )
        {
            listSize = max;
        }

        String[] list = new String[listSize];
        int i = 0;
        int lastTokenBegin;
        int lastTokenEnd = 0;
        while ( tok.hasMoreTokens() )
        {
            if ( max > 0 && i == listSize - 1 )
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
        return str == null || str.trim().length() == 0;
    }

    /**
     * <p>
     * Checks if a (trimmed) String is not <code>null</code> and not blank.
     * </p>
     *
     * @param str the String to check
     * @return <code>true</code> if the String is not <code>null</code> and length of trimmed
     * <code>str</code> is not zero.
     */
    public static boolean isNotBlank( String str )
    {
        return !isBlank( str );
    }

    /**
     * Escape the specified string to a representation that only consists of nicely printable characters, without any
     * newlines and without a comma.
     * <p>
     * The reverse-method is {@link #unescapeString(StringBuilder, CharSequence)}.
     *
     * @param target target string buffer. The required space will be up to {@code str.getBytes().length * 5} chars.
     * @param str String to escape values in, may be {@code null}.
     */
    @SuppressWarnings( "checkstyle:magicnumber" )
    public static void escapeToPrintable( StringBuilder target, CharSequence str )
    {
        if ( target == null )
        {
            throw new IllegalArgumentException( "The target buffer must not be null" );
        }
        if ( str == null )
        {
            return;
        }

        for ( int i = 0; i < str.length(); i++ )
        {
            char c = str.charAt( i );

            // handle non-nicely printable chars and the comma
            if ( c < 32 || c > 126 || c == '\\' || c == ',' )
            {
                target.append( '\\' );
                target.append( (char) HEX_CHARS[( 0xF000 & c ) >> 12] );
                target.append( (char) HEX_CHARS[( 0x0F00 & c ) >> 8] );
                target.append( (char) HEX_CHARS[( 0x00F0 & c ) >> 4] );
                target.append( (char) HEX_CHARS[( 0x000F & c )] );
            }
            else
            {
                target.append( c );
            }
        }
    }

    /**
     * Reverses the effect of {@link #escapeToPrintable(StringBuilder, CharSequence)}.
     *
     * @param target target string buffer
     * @param str the String to un-escape, as created by {@link #escapeToPrintable(StringBuilder, CharSequence)}
     */
    public static void unescapeString( StringBuilder target, CharSequence str )
    {
        if ( target == null )
        {
            throw new IllegalArgumentException( "The target buffer must not be null" );
        }
        if ( str == null )
        {
            return;
        }

        for ( int i = 0; i < str.length(); i++ )
        {
            char ch = str.charAt( i );

            if ( ch == '\\' )
            {
                target.append( (char) (
                                  digit( str.charAt( ++i ) ) << 12
                                | digit( str.charAt( ++i ) ) << 8
                                | digit( str.charAt( ++i ) ) << 4
                                | digit( str.charAt( ++i ) )
                                ) );
            }
            else
            {
                target.append( ch );
            }
        }
    }

    private static int digit( char ch )
    {
        if ( ch >= 'a' )
        {
            return 10 + ch - 'a';
        }
        else if ( ch >= 'A' )
        {
            return 10 + ch - 'A';
        }
        else
        {
            return ch - '0';
        }
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
    @SuppressWarnings( "checkstyle:magicnumber" )
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
            if ( b < 32 || b > 126 || b == '\\' || b == ',' )
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
     *
     * @param str the input String
     * @param charsetName the charset name
     * @return the number of bytes written to {@code out}
     */
    public static ByteBuffer unescapeBytes( String str, String charsetName  )
    {
        int outPos = 0;

        if ( str == null )
        {
            return ByteBuffer.wrap( new byte[0] );
        }

        byte[] out = new byte[str.length()];
        for ( int i = 0; i < str.length(); i++ )
        {
            char ch = str.charAt( i );

            if ( ch == '\\' )
            {
                int upper = digit( str.charAt( ++i ) );
                int lower = digit( str.charAt( ++i ) );
                out[outPos++] = (byte) ( upper << 4 | lower );
            }
            else
            {
                out[outPos++] = (byte) ch;
            }
        }

        Charset sourceCharset = Charset.forName( charsetName );
        if ( !DEFAULT_CHARSET.equals( sourceCharset ) )
        {
            CharBuffer decodedFromSourceCharset;
            try
            {
                decodedFromSourceCharset = sourceCharset.newDecoder().decode( ByteBuffer.wrap( out, 0, outPos ) );
                ByteBuffer defaultEncoded = DEFAULT_CHARSET.encode( decodedFromSourceCharset );

                return defaultEncoded;
            }
            catch ( CharacterCodingException e )
            {
                // ignore and fall through to the non-recoded version
            }
        }

        return ByteBuffer.wrap( out, 0, outPos );
    }

    public static String decode( byte[] toDecode, Charset charset )
    {
        try
        {
            // @todo use new JDK 1.6 constructor String(byte bytes[], Charset charset)
            return new String( toDecode, charset.name() );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( "The JVM must support Charset " + charset, e );
        }
    }

    public static byte[] encode( String toEncode, Charset charset )
    {
        try
        {
            // @todo use new JDK 1.6 method getBytes(Charset charset)
            return toEncode.getBytes( charset.name() );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new RuntimeException( "The JVM must support Charset " + charset, e );
        }
    }

    public static byte[] encodeStringForForkCommunication( String string )
    {
        try
        {
            return string.getBytes( FORK_STREAM_CHARSET_NAME );
        }
        catch ( UnsupportedEncodingException e )
        {
           throw new RuntimeException( "The JVM must support Charset " + FORK_STREAM_CHARSET_NAME, e );
        }
    }

    /*
    * In JDK7 use java.util.Objects instead.
    * */
    public static <T> T requireNonNull( T obj, String message )
    {
        if ( obj == null )
        {
            throw new NullPointerException( message );
        }
        return obj;
    }

    /*
    * In JDK7 use java.util.Objects instead.
    * */
    public static <T> T requireNonNull( T obj )
    {
        return requireNonNull( obj, null );
    }
}
