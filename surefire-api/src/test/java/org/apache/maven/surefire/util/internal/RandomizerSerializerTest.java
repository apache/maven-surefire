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

import org.apache.maven.surefire.util.Randomizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2016-04-11
 */
public class RandomizerSerializerTest
{

    @Test
    public void testDeserialize()
    {
        // given
        String serialized = "\u001E\u0003123456";

        // when
        Randomizer randomizer = RandomizerSerializer.deserialize( serialized );

        // then
        assertTrue( StringUtils.isBlank( randomizer.getGivenSeed() ) );
        assertEquals( 123456L, randomizer.getSeed() );
    }

    @Test
    public void testSerialize()
    {
        // given
        Randomizer randomizer = new Randomizer( "123456" );

        // when
        String result = RandomizerSerializer.serialize( randomizer );

        // then
        assertEquals( "123456\u001E\u0003123456", result );
    }
}