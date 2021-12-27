package org.apache.maven.surefire.shared.lang3;

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

/**
 * Delegate for {@link org.apache.commons.lang3.StringUtils}
 */
public class StringUtils
{
    public static boolean isBlank( CharSequence cs )
    {
        return org.apache.commons.lang3.StringUtils.isBlank( cs );
    }

    public static boolean isNotBlank( final CharSequence cs )
    {
        return org.apache.commons.lang3.StringUtils.isNotBlank( cs );
    }

    public static boolean isNumeric( final CharSequence cs )
    {
        return org.apache.commons.lang3.StringUtils.isNumeric( cs );
    }

    public static String substringBeforeLast( final String str, final String separator )
    {
        return org.apache.commons.lang3.StringUtils.substringBeforeLast( str, separator );
    }

    public static String trimToNull( final String str )
    {
        return org.apache.commons.lang3.StringUtils.trimToNull( str );
    }

    public static String removeEnd( final String str, final String remove )
    {
        return org.apache.commons.lang3.StringUtils.removeEnd( str, remove );
    }
}
