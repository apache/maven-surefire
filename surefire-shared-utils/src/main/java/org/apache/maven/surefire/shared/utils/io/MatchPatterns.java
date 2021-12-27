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
 * Delegate for {@link org.apache.maven.shared.utils.io.MatchPatterns}
 */
public class MatchPatterns
{

    private final org.apache.maven.shared.utils.io.MatchPatterns matchPatterns;

    public MatchPatterns( org.apache.maven.shared.utils.io.MatchPatterns matchPatterns )
    {
        this.matchPatterns = matchPatterns;
    }

    public static MatchPatterns from( String... sources )
    {
        return new MatchPatterns( org.apache.maven.shared.utils.io.MatchPatterns.from( sources ) );
    }

    public boolean matches( String name, boolean isCaseSensitive )
    {
        return matchPatterns.matches( name, isCaseSensitive );
    }
}
