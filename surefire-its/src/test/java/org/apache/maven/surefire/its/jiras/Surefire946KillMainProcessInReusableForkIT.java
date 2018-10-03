package org.apache.maven.surefire.its.jiras;

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
import org.junit.BeforeClass;
import org.junit.Test;

public class Surefire946KillMainProcessInReusableForkIT
    extends SurefireJUnit4IntegrationTestCase
{
    // there are 10 test classes that each would wait 2 seconds.
    private static final int TEST_SLEEP_TIME = 2000;

    @BeforeClass
    public static void installSelfdestructPlugin()
    {
        unpack( Surefire946KillMainProcessInReusableForkIT.class, "surefire-946-self-destruct-plugin", "plugin" ).executeInstall();
    }

    @Test( timeout = 30000 )
    public void testHalt()
    {
        doTest( "halt" );
    }

    @Test( timeout = 30000 )
    public void testExit()
    {
        doTest( "exit" );
    }

    @Test( timeout = 30000 )
    public void testInterrupt()
    {
        doTest( "interrupt" );
    }

    private void doTest( String method )
    {
        unpack( "surefire-946-killMainProcessInReusableFork" )
            .sysProp( "selfdestruct.timeoutInMillis", "5000" )
            .sysProp( "selfdestruct.method", method )
            .sysProp( "testSleepTime", String.valueOf( TEST_SLEEP_TIME ) )
            .addGoal( "org.apache.maven.plugins.surefire:maven-selfdestruct-plugin:selfdestruct" )
            .setForkJvm()
            .forkPerThread().threadCount( 1 ).reuseForks( true ).maven().withFailure().executeTest();
    }
}
