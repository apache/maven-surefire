package jira2117;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName( "Display name of the main test class" )
class NestedDisplayNameTest
{
    @Nested
    @DisplayName( "Display name of level 1 nested class A" )
    class A
    {
        @Test
        void level1_test_without_display_name()
        {
        }

        @Test
        @DisplayName( "Display name of level 1 test method" )
        void level1_test_with_display_name()
        {
        }
    }

    @Nested
    @DisplayName( "Display name of level 1 nested class B" )
    class B
    {
        @Nested
        @DisplayName( "Display name of level 2 nested class C" )
        class C
        {
            @Test
            @DisplayName( "Display name of non-parameterized level 2 test method" )
            void level2_test_nonparameterized()
            {
            }

            @ParameterizedTest
            @ValueSource(strings = {"paramValue1", "paramValue2"})
            @DisplayName( "Display name of parameterized level 2 test method" )
            void level2_test_parameterized(String paramValue)
            {
            }
        }
    }
}
