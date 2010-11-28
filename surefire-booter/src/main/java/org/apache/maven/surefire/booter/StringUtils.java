package org.apache.maven.surefire.booter;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.codehaus.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact codehaus@codehaus.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.codehaus.org/>.
 */

import java.util.StringTokenizer;

/**
 * <p>Common <code>String</code> manipulation routines.</p>
 *
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
 * @since 1.0
 * @version $Id: StringUtils.java 8001 2009-01-03 13:17:09Z vsiveton $
 * @noinspection JavaDoc
 */
public class StringUtils
{
    /**
     * <p><code>StringUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>StringUtils.trim(" foo ");</code>.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * manager to operate.</p>
     */
    public StringUtils()
    {
    }

    public static boolean isEmpty( String str )
    {
        return ( ( str == null ) || ( str.trim().length() == 0 ) );
    }

    // Equals and IndexOf
    //--------------------------------------------------------------------------

    // Substring
    //--------------------------------------------------------------------------

    // Splitting
    //--------------------------------------------------------------------------

    public static String[] split( String text, String separator )
    {
        return split( text, separator, -1 );
    }

    /**
     * <p>Splits the provided text into a array, based on a given separator.</p>
     *
     * <p>The separator is not included in the returned String array. The
     * maximum number of splits to perform can be controlled. A <code>null</code>
     * separator will cause parsing to be on whitespace.</p>
     *
     * <p>This is useful for quickly splitting a String directly into
     * an array of tokens, instead of an enumeration of tokens (as
     * <code>StringTokenizer</code> does).</p>
     *
     * @param str The string to parse.
     * @param separator Characters used as the delimiters. If
     *  <code>null</code>, splits on whitespace.
     * @param max The maximum number of elements to include in the
     *  array.  A zero or negative value implies no limit.
     * @return an array of parsed Strings
     */
    public static String[] split( String str, String separator, int max )
    {
        StringTokenizer tok;
        if ( separator == null )
        {
            // Null separator means we're using StringTokenizer's default
            // delimiter, which comprises all whitespace characters.
            tok = new StringTokenizer( str );
        }
        else
        {
            tok = new StringTokenizer( str, separator );
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
                lastTokenBegin = str.indexOf( endToken, lastTokenEnd );
                list[i] = str.substring( lastTokenBegin );
                break;
            }
            else
            {
                list[i] = tok.nextToken();
                lastTokenBegin = str.indexOf( list[i], lastTokenEnd );
                lastTokenEnd = lastTokenBegin + list[i].length();
            }
            i++;
        }
        return list;
    }

    // Replacing
    //--------------------------------------------------------------------------

    /**
     * <p>Replace all occurrences of a String within another String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @return the text with any replacements processed
     */
    public static String replace( String text, String repl, String with )
    {
        return replace( text, repl, with, -1 );
    }

    /**
     * <p>Replace a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @param max maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed
     */
    public static String replace( String text, String repl, String with, int max )
    {
        if ( ( text == null ) || ( repl == null ) || ( with == null ) || ( repl.length() == 0 ) )
        {
            return text;
        }

        StringBuffer buf = new StringBuffer( text.length() );
        int start = 0, end;
        while ( ( end = text.indexOf( repl, start ) ) != -1 )
        {
            buf.append( text.substring( start, end ) ).append( with );
            start = end + repl.length();

            if ( --max == 0 )
            {
                break;
            }
        }
        buf.append( text.substring( start ) );
        return buf.toString();
    }


    /**
     * <p>Remove whitespace from the front and back of a String.</p>
     *
     * @param str the String to remove whitespace from
     * @return the stripped String
     */
    public static String strip( String str )
    {
        return strip( str, null );
    }

    /**
     * <p>Remove a specified String from the front and back of a
     * String.</p>
     *
     * <p>If whitespace is wanted to be removed, used the
     * {@link #strip(java.lang.String)} method.</p>
     *
     * @param str the String to remove a string from
     * @param delim the String to remove at start and end
     * @return the stripped String
     */
    public static String strip( String str, String delim )
    {
        str = stripStart( str, delim );
        return stripEnd( str, delim );
    }

    /**
     * <p>Strip any of a supplied String from the end of a String.</p>
     *
     * <p>If the strip String is <code>null</code>, whitespace is
     * stripped.</p>
     *
     * @param str the String to remove characters from
     * @param strip the String to remove
     * @return the stripped String
     */
    public static String stripEnd( String str, String strip )
    {
        if ( str == null )
        {
            return null;
        }
        int end = str.length();

        if ( strip == null )
        {
            while ( ( end != 0 ) && Character.isWhitespace( str.charAt( end - 1 ) ) )
            {
                end--;
            }
        }
        else
        {
            while ( ( end != 0 ) && ( strip.indexOf( str.charAt( end - 1 ) ) != -1 ) )
            {
                end--;
            }
        }
        return str.substring( 0, end );
    }

    /**
     * <p>Strip any of a supplied String from the start of a String.</p>
     *
     * <p>If the strip String is <code>null</code>, whitespace is
     * stripped.</p>
     *
     * @param str the String to remove characters from
     * @param strip the String to remove
     * @return the stripped String
     */
    public static String stripStart( String str, String strip )
    {
        if ( str == null )
        {
            return null;
        }

        int start = 0;

        int sz = str.length();

        if ( strip == null )
        {
            while ( ( start != sz ) && Character.isWhitespace( str.charAt( start ) ) )
            {
                start++;
            }
        }
        else
        {
            while ( ( start != sz ) && ( strip.indexOf( str.charAt( start ) ) != -1 ) )
            {
                start++;
            }
        }
        return str.substring( start );
    }
    // Difference
    //--------------------------------------------------------------------------

    /**
     * <p>Checks if String contains a search character, handling <code>null</code>.
     * This method uses {@link String#indexOf(int)}.</p>
     *
     * <p>A <code>null</code> or empty ("") String will return <code>false</code>.</p>
     *
     * <pre>
     * StringUtils.contains(null, *)    = false
     * StringUtils.contains("", *)      = false
     * StringUtils.contains("abc", 'a') = true
     * StringUtils.contains("abc", 'z') = false
     * </pre>
     *
     * @param str  the String to check, may be null
     * @param searchChar  the character to find
     * @return true if the String contains the search character,
     *  false if not or <code>null</code> string input
     * @since 1.5.7
     */
    public static boolean contains( String str, char searchChar )
    {
        //noinspection SimplifiableIfStatement
        if ( isEmpty( str ) )
        {
            return false;
        }
        return str.indexOf( searchChar ) >= 0;
    }

    /**
     * <p>Checks if String contains a search String, handling <code>null</code>.
     * This method uses {@link String#indexOf(int)}.</p>
     *
     * <p>A <code>null</code> String will return <code>false</code>.</p>
     *
     * <pre>
     * StringUtils.contains(null, *)     = false
     * StringUtils.contains(*, null)     = false
     * StringUtils.contains("", "")      = true
     * StringUtils.contains("abc", "")   = true
     * StringUtils.contains("abc", "a")  = true
     * StringUtils.contains("abc", "z")  = false
     * </pre>
     *
     * @param str  the String to check, may be null
     * @param searchStr  the String to find, may be null
     * @return true if the String contains the search String,
     *  false if not or <code>null</code> string input
     * @since 1.5.7
     */
    public static boolean contains( String str, String searchStr )
    {
        //noinspection SimplifiableIfStatement
        if ( str == null || searchStr == null )
        {
            return false;
        }
        return str.indexOf( searchStr ) >= 0;
    }
}

