package org.apache.maven.surefire.cli;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * CLI options.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 * @see <a href="http://books.sonatype.com/mvnref-book/reference/running-sect-options.html">command line options</a>
 */
public enum CommandLineOption
{
    REACTOR_FAIL_FAST , REACTOR_FAIL_AT_END, REACTOR_FAIL_NEVER,
    SHOW_ERRORS,
    LOGGING_LEVEL_WARN, LOGGING_LEVEL_INFO, LOGGING_LEVEL_ERROR, LOGGING_LEVEL_DEBUG;

    public static List<CommandLineOption> fromStrings( Collection<String> elements )
    {
        List<CommandLineOption> options = new ArrayList<>( elements.size() );
        for ( String element : elements )
        {
            options.add( valueOf( element ) );
        }
        return options;
    }

    public static List<String> toStrings( Collection<CommandLineOption> options )
    {
        List<String> elements = new ArrayList<>( options.size() );
        for ( CommandLineOption option : options )
        {
            elements.add( option.name() );
        }
        return elements;
    }
}
