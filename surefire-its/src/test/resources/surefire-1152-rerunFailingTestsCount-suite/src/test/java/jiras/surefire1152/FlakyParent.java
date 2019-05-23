package jiras.surefire1152;

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

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

public class FlakyParent
{
    // set of test classes which have previously invoked testFlakyParent
    private static final Set<Class<?>> PREVIOUSLY_RUN = new HashSet<>();

    @Test
    public void testFlakyParent()
    {
        Class<?> clazz = getClass();
        if ( !PREVIOUSLY_RUN.contains( clazz ) )
        {
            PREVIOUSLY_RUN.add( clazz );
            fail( "deliberately flaky test (should pass the next time)" );
        }
    }
}
