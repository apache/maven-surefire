package org.apache.maven.surefire.junitcore.pc;

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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
final class RangeMatcher
    extends BaseMatcher<Long>
{
    private final long from;

    private final long to;

    private RangeMatcher( long from, long to )
    {
        this.from = from;
        this.to = to;
    }

    public static Matcher<Long> between( long from, long to )
    {
        return new RangeMatcher( from, to );
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendValueList( "between ", " and ", "", from, to );
    }

    @Override
    public boolean matches( Object o )
    {
        long actual = (Long) o;
        return actual >= from && actual <= to;
    }
}