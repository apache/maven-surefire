package org.apache.maven.surefire.shared.utils;

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

import java.util.Iterator;

/**
 * Delegate for {@link org.apache.maven.shared.utils.StringUtils}
 */
public class StringUtils
{

    public static boolean isBlank( String str )
    {
        return org.apache.maven.shared.utils.StringUtils.isBlank( str );
    }

    public static boolean isNotBlank( String str )
    {
        return org.apache.maven.shared.utils.StringUtils.isNotBlank( str );
    }

    public static boolean isEmpty( String str )
    {
        return org.apache.maven.shared.utils.StringUtils.isEmpty( str );
    }

    public static boolean isNotEmpty( String str )
    {
        return org.apache.maven.shared.utils.StringUtils.isNotEmpty( str );
    }

    public static String capitalizeFirstLetter( String data )
    {
        return org.apache.maven.shared.utils.StringUtils.capitalizeFirstLetter( data );
    }

    public static String join( Object[] array, String separator )
    {
        return org.apache.maven.shared.utils.StringUtils.join( array, separator );
    }

    public static String join( Iterator<?> iterator, String separator )
    {
        return org.apache.maven.shared.utils.StringUtils.join( iterator, separator );
    }

    public static String chompLast( String str, String sep )
    {
        return org.apache.maven.shared.utils.StringUtils.chompLast( str, sep );
    }

    public static String replace( String text, String repl, String with )
    {
        return org.apache.maven.shared.utils.StringUtils.replace( text, repl, with );
    }

    public static String[] split( String text )
    {
        return org.apache.maven.shared.utils.StringUtils.split( text );
    }

    public static String[] split( String text, String separator )
    {
        return org.apache.maven.shared.utils.StringUtils.split( text, separator );
    }
}
