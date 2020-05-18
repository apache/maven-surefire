package org.apache.maven.surefire.api.util.internal;

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

import static java.lang.System.lineSeparator;

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
}
