package org.apache.maven.surefire.shared.utils.io;

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
 * Delegate for {@link org.apache.maven.shared.utils.io.SelectorUtils}
 */
public class SelectorUtils
{
    public static final String PATTERN_HANDLER_SUFFIX =
        org.apache.maven.shared.utils.io.SelectorUtils.PATTERN_HANDLER_SUFFIX;

    public static final String REGEX_HANDLER_PREFIX =
        org.apache.maven.shared.utils.io.SelectorUtils.REGEX_HANDLER_PREFIX;


    public static boolean matchPath( String pattern, String str )
    {
        return org.apache.maven.shared.utils.io.SelectorUtils.matchPath( pattern, str );
    }

    public static boolean matchPath( String pattern, String str, boolean isCaseSensitive )
    {
        return org.apache.maven.shared.utils.io.SelectorUtils.matchPath( pattern, str, isCaseSensitive );
    }

}
