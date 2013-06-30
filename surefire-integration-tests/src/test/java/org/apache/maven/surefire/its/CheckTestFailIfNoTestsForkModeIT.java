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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Test failIfNoTests with various forkModes.
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestFailIfNoTestsForkModeIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void failIfNoTestsForkModeAlways()
    {
        unpack().forkAlways().failIfNoTests( true ).maven().withFailure().executeTest();
    }

    @Test
    public void failIfNoTestsForkModeNever()
    {
        unpack().forkNever().failIfNoTests( true ).maven().withFailure().executeTest();
    }

    @Test
    public void failIfNoTestsForkModeOnce()
    {
        unpack().forkOnce().failIfNoTests( true ).maven().withFailure().executeTest();
    }

    @Test
    public void dontFailIfNoTestsForkModeAlways()
    {
        doTest( unpack().forkAlways().failIfNoTests( false ) );
    }

    @Test
    public void dontFailIfNoTestsForkModeNever()
    {
        doTest( unpack().forkNever().failIfNoTests( false ) );
    }

    @Test
    public void dontFailIfNoTestsForkModeOnce()
    {
        doTest( unpack().forkOnce().failIfNoTests( false ) );
    }

    private void doTest( SurefireLauncher launcher )
    {
            launcher.executeTest().verifyErrorFreeLog().assertTestSuiteResults( 0, 0, 0, 0 );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "default-configuration-classWithNoTests" );
    }

}
