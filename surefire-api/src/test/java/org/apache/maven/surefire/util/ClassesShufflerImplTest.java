package org.apache.maven.surefire.util;

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

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2018-06-13
 */
public class ClassesShufflerImplTest
{

    @Test
    public void testShuffle()
    {
        // given
        String seed = "123456";
        Randomizer randomizer = new Randomizer( seed );
        ClassesShuffler shuffler = new ClassesShufflerImpl( randomizer );
        List<Class<?>> list = asList( A.class, B.class, C.class );

        // when
        shuffler.shuffle( list );

        // then
        assertEquals( asList( C.class, B.class, A.class ), list );
    }

    private static final class A
    {

    }

    private static final class B
    {

    }

    private static final class C
    {

    }
}