package org.apache.maven.plugin.surefire.log.api;

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

import static org.apache.maven.plugin.surefire.log.api.Level.resolveLevel;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for {@link Level}.
 */
public class LevelTest
{
    @Test
    public void shouldHaveSuccess()
    {
        Level level = resolveLevel( true, false, false, false, false );
        assertThat( level ).isEqualTo( Level.SUCCESS );
    }

    @Test
    public void shouldNotHaveSuccess()
    {
        Level level = resolveLevel( false, false, false, false, false );
        assertThat( level ).isEqualTo( Level.NO_COLOR );
    }

    @Test
    public void shouldBeFailure()
    {
        Level level = resolveLevel( false, true, false, false, false );
        assertThat( level ).isEqualTo( Level.FAILURE );
    }

    @Test
    public void shouldBeError()
    {
        Level level = resolveLevel( false, false, true, false, false );
        assertThat( level ).isEqualTo( Level.FAILURE );
    }

    @Test
    public void shouldBeSkipped()
    {
        Level level = resolveLevel( false, false, false, true, false );
        assertThat( level ).isEqualTo( Level.UNSTABLE );
    }

    @Test
    public void shouldBeFlake()
    {
        Level level = resolveLevel( false, false, false, false, true );
        assertThat( level ).isEqualTo( Level.UNSTABLE );
    }
}
