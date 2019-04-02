package junitplatformenginejupiter;

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
import org.junit.jupiter.api.Test;

@DisplayName( "<< ✨ >>" )
class DisplayNameTest
{
    @Test
    @DisplayName( "73$71 ✔" )
    void test1()
            throws Exception
    {
        System.out.println( getClass().getDeclaredMethod( "test1" ).getAnnotation( DisplayName.class ).value() );
        System.out.println( getClass().getAnnotation( DisplayName.class ).value() );
    }

    @Test
    @DisplayName( "73$72 ✔" )
    void test2()
            throws Exception
    {
        System.out.println( getClass().getDeclaredMethod( "test2" ).getAnnotation( DisplayName.class ).value() );
        System.out.println( getClass().getAnnotation( DisplayName.class ).value() );
    }
}
