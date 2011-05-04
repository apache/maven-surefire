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
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.StringTokenizer;
import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * <p>Common <code>String</code> manipulation routines.</p>
 * <p/>
 * <p>Originally from
 * <a href="http://jakarta.apache.org/turbine/">Turbine</a> and the
 * GenerationJavaCore library.</p>
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
 * <p/>
 * A quick borrow from plexus-utils by Kristian Rosenvold, to restore jdk1.3 compat
 * Threw away all the unused stuff.
 * <p/>
 * NOTE: This class is not part of any api and is public purely for technical reasons !
 * @since 1.0
 */
public class StringUtils
{

    // Splitting
    //--------------------------------------------------------------------------

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
     * <p>Checks if a (trimmed) String is <code>null</code> or blank.</p>
     *
     * @param str the String to check
     * @return <code>true</code> if the String is <code>null</code>, or
     *         length zero once trimmed
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

    // Ripped from commons-lang StringEscapeUtils. Maybe Use dependency instead
    public static int unescapeJava( byte[] out, String str )
    {
        int outPos = 0;
        if ( out == null )
        {
            throw new IllegalArgumentException( "The Writer must not be null" );
        }
        if ( str == null )
        {
            return 0;
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
                        out[outPos++] = (byte) value;
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
                        out[outPos++] = '\\';
                        break;
                    case '\'':
                        out[outPos++] = '\'';
                        break;
                    case '\"':
                        out[outPos++] = '"';
                        break;
                    case 'r':
                        out[outPos++] = '\r';
                        break;
                    case 'f':
                        out[outPos++] = '\f';
                        break;
                    case 't':
                        out[outPos++] = '\t';
                        break;
                    case 'n':
                        out[outPos++] = '\n';
                        break;
                    case 'b':
                        out[outPos++] = '\b';
                        break;
                    case 'u':
                    {
                        // uh-oh, we're in unicode country....
                        inUnicode = true;
                        break;
                    }
                    default:
                        out[outPos++] = (byte) ch;
                        break;
                }
                continue;
            }
            else if ( ch == '\\' )
            {
                hadSlash = true;
                continue;
            }
            out[outPos++] = (byte) ch;
        }
        if ( hadSlash )
        {
            // then we're in the weird case of a \ at the end of the
            // string, let's output it anyway.
            out[outPos++] = '\\';
        }
        return outPos;
    }

    // Ripped from commons-lang StringEscapeUtils. With a minor modification, we unicode-quote commas
    // to avoid csv decoding problems ;)

    /**
     * @param out               write to receieve the escaped string
     * @param str               String to escape values in, may be null
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
            {    // Kr - this line modified from commons
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

    public static void escapeJavaStyleString( ByteBuffer out, byte[] str, int off, int len )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "The Writer must not be null" );
        }
        final int inputLength = str.length;
        if ( str == null || inputLength == 0 )
        {
            return;
        }
        int outputPos = 0;
        int end = off + len;
        for ( int i = off; i < end; i++ )
        {
            char ch = (char) str[i];

            // handle unicode
            if ( ch > 0xfff )
            {
                outputPos = writeOut( out, outputPos, "\\u" + hex( ch ) );
            }
            else if ( ch > 0xff )
            {
                outputPos = writeOut( out, outputPos, "\\u0" + hex( ch ) );
            }
            else if ( ch > 0x7f || ch == ',' )
            {    // Kr - this line modified from commons
                outputPos = writeOut( out, outputPos, "\\u00" + hex( ch ) );
            }
            else if ( ch < 32 )
            {
                switch ( ch )
                {
                    case '\b':
                        out.append( '\\' );
                        out.append( 'b' );
                        break;
                    case '\n':
                        out.append( '\\' );
                        out.append( 'n' );
                        break;
                    case '\t':
                        out.append( '\\' );
                        out.append( 't' );
                        break;
                    case '\f':
                        out.append( '\\' );
                        out.append( 'f' );
                        break;
                    case '\r':
                        out.append( '\\' );
                        out.append( 'r' );
                        break;
                    default:
                        if ( ch > 0xf )
                        {
                            outputPos = writeOut( out, outputPos, "\\u00" + hex( ch ) );
                        }
                        else
                        {
                            outputPos = writeOut( out, outputPos, "\\u000" + hex( ch ) );
                        }
                        break;
                }
            }
            else
            {
                switch ( ch )
                {
                    case '\'':
                        out.append( '\\' );
                        out.append( '\'' );
                        break;
                    case '"':
                        out.append( '\\' );
                        out.append( '"' );
                        break;
                    case '\\':
                        out.append( '\\' );
                        out.append( '\\' );
                        break;
                    case '/':
                        out.append( '\\' );
                        out.append( '/' );
                        break;
                    default:
                        out.append( ch );
                        break;
                }
            }
        }
    }

    public static void escapeJavaStyleString( PrintStream out, byte[] str, int off, int len )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "The Writer must not be null" );
        }
        final int inputLength = str.length;
        if ( str == null || inputLength == 0 )
        {
            return;
        }
        int outputPos = 0;
        int end = off + len;
        for ( int i = off; i < end; i++ )
        {
            char ch = (char) str[i];

            // handle unicode
            if ( ch > 0xfff )
            {
                outputPos = writeOut( out, outputPos, "\\u" + hex( ch ) );
            }
            else if ( ch > 0xff )
            {
                outputPos = writeOut( out, outputPos, "\\u0" + hex( ch ) );
            }
            else if ( ch > 0x7f || ch == ',' )
            {    // Kr - this line modified from commons
                outputPos = writeOut( out, outputPos, "\\u00" + hex( ch ) );
            }
            else if ( ch < 32 )
            {
                switch ( ch )
                {
                    case '\b':
                        out.append( '\\' );
                        out.append( 'b' );
                        break;
                    case '\n':
                        out.append( '\\' );
                        out.append( 'n' );
                        break;
                    case '\t':
                        out.append( '\\' );
                        out.append( 't' );
                        break;
                    case '\f':
                        out.append( '\\' );
                        out.append( 'f' );
                        break;
                    case '\r':
                        out.append( '\\' );
                        out.append( 'r' );
                        break;
                    default:
                        if ( ch > 0xf )
                        {
                            outputPos = writeOut( out, outputPos, "\\u00" + hex( ch ) );
                        }
                        else
                        {
                            outputPos = writeOut( out, outputPos, "\\u000" + hex( ch ) );
                        }
                        break;
                }
            }
            else
            {
                switch ( ch )
                {
                    case '\'':
                        out.append( '\\' );
                        out.append( '\'' );
                        break;
                    case '"':
                        out.append( '\\' );
                        out.append( '"' );
                        break;
                    case '\\':
                        out.append( '\\' );
                        out.append( '\\' );
                        break;
                    case '/':
                        out.append( '\\' );
                        out.append( '/' );
                        break;
                    default:
                        out.append( ch );
                        break;
                }
            }
        }
    }

    public static int escapeJavaStyleString( byte[] out, int outoff, byte[] str, int off, int len )
    {
        if ( out == null )
        {
            throw new IllegalArgumentException( "The Writer must not be null" );
        }
        final int inputLength = str.length;
        if ( str == null || inputLength == 0 )
        {
            return 0;
        }
        int outputPos = outoff;
        int end = off + len;
        for ( int i = off; i < end; i++ )
        {
            char ch = (char) str[i];

            // handle unicode
            if ( ch > 0xfff )
            {
                outputPos = writeOut( out, outputPos, "\\u" + hex( ch ) );
            }
            else if ( ch > 0xff )
            {
                outputPos = writeOut( out, outputPos, "\\u0" + hex( ch ) );
            }
            else if ( ch > 0x7f || ch == ',' )
            {    // Kr - this line modified from commons
                outputPos = writeOut( out, outputPos, "\\u00" + hex( ch ) );
            }
            else if ( ch < 32 )
            {
                switch ( ch )
                {
                    case '\b':
                        out[outputPos++] = '\\';
                        out[outputPos++] = 'b';
                        break;
                    case '\n':
                        out[outputPos++] = '\\';
                        out[outputPos++] = 'n';
                        break;
                    case '\t':
                        out[outputPos++] = '\\';
                        out[outputPos++] = 't';
                        break;
                    case '\f':
                        out[outputPos++] = '\\';
                        out[outputPos++] = 'f';
                        break;
                    case '\r':
                        out[outputPos++] = '\\';
                        out[outputPos++] = 'r';
                        break;
                    default:
                        if ( ch > 0xf )
                        {
                            outputPos = writeOut( out, outputPos, "\\u00" + hex( ch ) );
                        }
                        else
                        {
                            outputPos = writeOut( out, outputPos, "\\u000" + hex( ch ) );
                        }
                        break;
                }
            }
            else
            {
                switch ( ch )
                {
                    case '\'':
                        out[outputPos++] = '\\';
                        out[outputPos++] = '\'';
                        break;
                    case '"':
                        out[outputPos++] = '\\';
                        out[outputPos++] = '"';
                        break;
                    case '\\':
                        out[outputPos++] = '\\';
                        out[outputPos++] = '\\';
                        break;
                    case '/':
                        out[outputPos++] = '\\';
                        out[outputPos++] = '/';
                        break;
                    default:
                        out[outputPos++] = (byte) ch;
                        break;
                }
            }
        }
        return outputPos - outoff;
    }

    private static int writeOut( ByteBuffer out, int outputPos, final String msg )
    {
        byte[] bytes = msg.getBytes();
        for ( int cnt = 0; cnt < bytes.length; cnt++ )
        {
            out.append( bytes[cnt] );
        }
        return outputPos;
    }

    private static int writeOut( PrintStream out, int outputPos, final String msg )
    {
        byte[] bytes = msg.getBytes();
        for ( int cnt = 0; cnt < bytes.length; cnt++ )
        {
            out.write( bytes[cnt] );
        }
        return outputPos;
    }


    private static int writeOut( byte[] out, int outputPos, final String msg )
    {
        byte[] bytes = msg.getBytes();
        for ( int cnt = 0; cnt < bytes.length; cnt++ )
        {
            out[outputPos++] = bytes[cnt];
        }
        return outputPos;
    }


    public static String hex( char ch )
    {
        return Integer.toHexString( ch ).toUpperCase();
    }

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

    public static void escapeJavaStyleString( PrintStream target, String str )
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
}

