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

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2016-04-02
 */
public class RandomizerTest
{

    @Test
    public void testGetSeed()
    {
        // given
        String seed = "123123";
        Randomizer randomizer = new Randomizer( seed );
        // when
        long seedAsLong = randomizer.getSeed();

        // then
        assertEquals( 123123, seedAsLong );
    }

    @Test
    public void testGetGivenSeed()
    {
        // given
        String seed = "678";
        Randomizer randomizer = new Randomizer( seed );

        // when
        String given = randomizer.getGivenSeed();

        // then
        assertSame( seed, given );
    }

    @Test
    public void testGetRandom()
    {
        // given
        String seed = "fdfrty";
        Randomizer randomizer = new Randomizer( seed );

        // when
        Random random = randomizer.getRandom();
        assertNotNull( random );
        int first = random.nextInt();
        randomizer = new Randomizer( seed );
        random = randomizer.getRandom();
        assertNotNull( random );
        int second = random.nextInt();

        // then
        assertEquals( first, second );
    }

    @Test
    public void testGetRandomRandomly()
    {
        // given
        Randomizer randomizer = new Randomizer();

        // when
        Random random = randomizer.getRandom();
        assertNotNull( random );
        int first = random.nextInt();
        randomizer = new Randomizer();
        Random secrandom = randomizer.getRandom();
        assertNotNull( secrandom );
        int second = random.nextInt();

        // then
        assertNotSame( first, second );
    }

    @Test
    public void testCopyConstructor()
    {
        // given
        Randomizer first = new Randomizer();
        String asString = String.valueOf( first.getSeed() );
        Randomizer second = new Randomizer( asString );

        // when
        int firstInt = first.getRandom().nextInt();
        int secondInt = second.getRandom().nextInt();

        // then
        assertEquals( firstInt, secondInt );
    }
}