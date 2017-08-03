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
public final class StringUtils
{
    public static final String NL = System.getProperty( "line.separator" );

    private static final byte[] HEX_CHARS = {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    // 8-bit charset Latin-1
    public static final String FORK_STREAM_CHARSET_NAME = "ISO-8859-1";

    private StringUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

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
     *
     * @param buffer     Examined StringBuffer
     * @param pattern    a pattern which should start in <code>buffer</code>
     * @return <tt>true</tt> if buffer's literal starts with given pattern
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
}
