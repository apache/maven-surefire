package org.apache.maven.surefire.util;

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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.BitSet;

/**
 * Utility for dealing with URLs in pre-JDK 1.4.
 */
public class UrlUtils
{
    private static final BitSet UNRESERVED = new BitSet( Byte.MAX_VALUE - Byte.MIN_VALUE + 1 );

    private static final int RADIX = 16;

    private static final int MASK = 0xf;

    private UrlUtils()
    {
    }

    private static final String ENCODING = "UTF-8";

    static
    {
        try
        {
            byte[] bytes =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'():/".getBytes( ENCODING );
            for ( byte aByte : bytes )
            {
                UNRESERVED.set( aByte );
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            // can't happen as UTF-8 must be present
        }
    }

    public static URL getURL( File file )
        throws MalformedURLException
    {
        // with JDK 1.4+, code would be: return new URL( file.toURI().toASCIIString() );
        //noinspection deprecation
        URL url = file.toURL();
        // encode any characters that do not comply with RFC 2396
        // this is primarily to handle Windows where the user's home directory contains spaces
        try
        {
            byte[] bytes = url.toString().getBytes( ENCODING );
            StringBuilder buf = new StringBuilder( bytes.length );
            for ( byte b : bytes )
            {
                if ( b > 0 && UNRESERVED.get( b ) )
                {
                    buf.append( (char) b );
                }
                else
                {
                    buf.append( '%' );
                    buf.append( Character.forDigit( b >>> 4 & MASK, RADIX ) );
                    buf.append( Character.forDigit( b & MASK, RADIX ) );
                }
            }
            return new URL( buf.toString() );
        }
        catch ( UnsupportedEncodingException e )
        {
            // should not happen as UTF-8 must be present
            throw new NestedRuntimeException( e );
        }
    }
}
