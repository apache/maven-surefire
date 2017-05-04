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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JUnit Description parser.
 * Used by JUnit Version lower than 4.7.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class TestClassMethodNameUtils
{
    /**
     * This pattern is verbatim copy from JUnit's code in class {@code Description}.
     * Parsing class and method from junit description would provide identical result to JUnit internal parser.
     */
    private static final Pattern METHOD_CLASS_PATTERN = Pattern.compile( "([\\s\\S]*)\\((.*)\\)" );

    private TestClassMethodNameUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static String extractClassName( String displayName )
    {
        Matcher m = METHOD_CLASS_PATTERN.matcher( displayName );
        return m.matches() ? m.group( 2 ) : displayName;
    }

    public static String extractMethodName( String displayName )
    {
        Matcher m = METHOD_CLASS_PATTERN.matcher( displayName );
        return m.matches() ? m.group( 1 ) : null;
    }
}
