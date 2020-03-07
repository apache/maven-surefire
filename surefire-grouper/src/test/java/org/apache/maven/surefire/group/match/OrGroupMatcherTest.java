package org.apache.maven.surefire.group.match;

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

import junit.framework.TestCase;

/**
 *
 */
public class OrGroupMatcherTest
    extends TestCase
{

    public void testMatchOneInOredGroup()
    {
        OrGroupMatcher matcher = new OrGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                                     new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertTrue( matcher.enabled( InverseGroupMatcher.class, AndGroupMatcher.class ) );
    }

    public void testMatchBothInOredGroup()
    {
        OrGroupMatcher matcher = new OrGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                                     new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertTrue( matcher.enabled( InverseGroupMatcher.class, SingleGroupMatcher.class ) );
    }

    public void testMatchNoneInOredGroup()
    {
        OrGroupMatcher matcher = new OrGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                                     new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertFalse( matcher.enabled( OrGroupMatcher.class, AndGroupMatcher.class ) );
    }

}
