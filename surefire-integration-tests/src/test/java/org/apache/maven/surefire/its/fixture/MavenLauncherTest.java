package org.apache.maven.surefire.its.fixture;

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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public class MavenLauncherTest
{
    @Test
    public void shouldNotDuplicateSystemProperties()
    {
        MavenLauncher launcher = new MavenLauncher( getClass(), "", "" )
                                         .addGoal( "-DskipTests" )
                                         .addGoal( "-Dx=a" )
                                         .addGoal( "-DskipTests" )
                                         .addGoal( "-Dx=b" );

        assertThat( launcher.getGoals(), hasItems( "-Dx=b", "-DskipTests" ) );

        assertThat( launcher.getGoals().size(), is( 2 ) );
    }
}
