package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * SUREFIRE-763 Asserts that environment variables are correctly populated using "useSystemClassLoader=false"
 * SUREFIRE-963 Asserts that empty environment variables are read as "".
 * 
 * @author Kristian Rosenvold
 * @author Christophe Deneux
 */
public class EnvironmentVariablesIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testWhenUseSystemClassLoader()
    {
        unpack( "/environment-variables" )
                .debugLogging()
                .addGoal( "-DuseSystemClassLoader=true" )
                .executeTest();
    }

    @Test
    public void testWhenDontUseSystemClassLoader()
    {
        unpack( "/environment-variables" )
                .debugLogging()
                .addGoal( "-DuseSystemClassLoader=false" )
                .executeTest();
    }

    @Test
    public void testExcludedEnv()
    {
        unpack( "/environment-variables" )
                .maven()
                .debugLogging()
                .addEnvVar( "UNDEFINED_VAR", "dwdijoi" )
                .sysProp( "surefire.excludedEnvironmentVariables", "UNDEFINED_VAR" )
                .executeTest();
    }

}