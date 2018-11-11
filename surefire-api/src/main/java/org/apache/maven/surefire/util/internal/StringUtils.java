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
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * <p>
 * Common {@link String java.lang.String} manipulation routines.
 * </p>
 * <br>
 * <p>
 * Originally from <a href="http://jakarta.apache.org/turbine/">Turbine</a> and the GenerationJavaCore library.
 * </p>
 * <br>
 * NOTE: This class is not part of any api and is public purely for technical reasons !
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
 * @since 1.0
 */
public final class StringUtils
{
    public static final String NL = lineSeparator();

    private static final byte[] HEX_CHARS = {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    private StringUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static String[] split( String text, String separator )
    {
        final StringTokenizer tok;
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

        String[] list = new String[tok.countTokens()];
        for ( int i = 0; tok.hasMoreTokens(); i++ )
        {
            list[i] = tok.nextToken();
        }
        return list;
    }

    /**
     * <p>
     * Checks if a (trimmed) String is {@code null} or blank.
     * </p>
     *
     * @param str the String to check
     * @return {@code true} if the String is {@code null}, or length zero once trimmed
     */
    public static boolean isBlank( String str )
    {
        return str == null || str.trim().isEmpty();
    }

    /**
     * <p>
     * Checks if a (trimmed) String is not {@code null} and not blank.
     * </p>
     *
     * @param str the String to check
     * @return {@code true} if the String is not {@code null} and length of trimmed {@code str} is not zero.
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
     * Escapes the bytes in the array {@code input} to contain only 'printable' bytes.
     * <br>
     * Escaping is done by encoding the non-nicely printable bytes to {@code '\' + upperCaseHexBytes(byte)}.
     * <br>
     * The reverse-method is {@link #unescapeBytes(String, String)}.
     * <br>
     * The returned byte array is started with aligned sequence {@code header} and finished by {@code \n}.
     *
     * @param header prefix header
     * @param input input buffer
     * @param off offset in the input buffer
     * @param len number of bytes to copy from the input buffer
     * @return number of bytes written to {@code out}
     * @throws NullPointerException if the specified parameter {@code header} or {@code input} is null
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is out of range
     *         ({@code off < 0 || len < 0 || off >= input.length || len > input.length || off + len > input.length})
     */
    @SuppressWarnings( "checkstyle:magicnumber" )
    public static EncodedArray escapeBytesToPrintable( final byte[] header, final byte[] input, final int off,
                                                       final int len )
    {
        if ( input.length == 0 )
        {
            return EncodedArray.EMPTY;
        }
        if ( off < 0 || len < 0 || off >= input.length || len > input.length || off + len > input.length )
        {
            throw new IndexOutOfBoundsException(
                    "off < 0 || len < 0 || off >= input.length || len > input.length || off + len > input.length" );
        }
        // Hex-escaping can be up to 3 times length of a regular byte. Last character is '\n', see (+1).
        final byte[] encodeBytes = new byte[header.length + 3 * len + 1];
        System.arraycopy( header, 0, encodeBytes, 0, header.length );
        int outputPos = header.length;
        final int end = off + len;
        for ( int i = off; i < end; i++ )
        {
            final byte b = input[i];

            // handle non-nicely printable bytes
            if ( b < 32 || b > 126 || b == '\\' || b == ',' )
            {
                final int upper = ( 0xF0 & b ) >> 4;
                final int lower = ( 0x0F & b );
                encodeBytes[outputPos++] = '\\';
                encodeBytes[outputPos++] = HEX_CHARS[upper];
                encodeBytes[outputPos++] = HEX_CHARS[lower];
            }
            else
            {
                encodeBytes[outputPos++] = b;
            }
        }
        encodeBytes[outputPos++] = (byte) '\n';

        return new EncodedArray( encodeBytes, outputPos );
    }

    /**
     * Reverses the effect of {@link #escapeBytesToPrintable(byte[], byte[], int, int)}.
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
                return DEFAULT_CHARSET.encode( decodedFromSourceCharset );
            }
            catch ( CharacterCodingException e )
            {
                // ignore and fall through to the non-recoded version
            }
        }

        return ByteBuffer.wrap( out, 0, outPos );
    }

    public static byte[] encodeStringForForkCommunication( String string )
    {
        return string.getBytes( ISO_8859_1 );
    }

    /**
     * Determines if {@code buffer} starts with specific literal(s).
     *
     * @param buffer     Examined StringBuffer
     * @param pattern    a pattern which should start in {@code buffer}
     * @return    {@code true} if buffer's literal starts with given {@code pattern}, or both are empty.
     */
    public static boolean startsWith( StringBuffer buffer, String pattern )
    {
        if ( buffer.length() < pattern.length() )
        {
            return false;
        }
        else
        {
            for ( int i = 0, len = pattern.length(); i < len; i++ )
            {
                if ( buffer.charAt( i ) != pattern.charAt( i ) )
                {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Escaped string to byte array with offset 0 and certain length.
     */
    public static final class EncodedArray
    {
        private static final EncodedArray EMPTY = new EncodedArray( new byte[]{}, 0 );

        private final byte[] array;
        private final int size;

        private EncodedArray( byte[] array, int size )
        {
            this.array = array;
            this.size = size;
        }

        public byte[] getArray()
        {
            return array;
        }

        public int getSize()
        {
            return size;
        }
    }
}
