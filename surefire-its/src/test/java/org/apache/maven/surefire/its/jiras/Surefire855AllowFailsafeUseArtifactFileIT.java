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
import org.junit.Test;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-855">SUREFIRE-855</a>
 * @since 2.19
 */
public class Surefire855AllowFailsafeUseArtifactFileIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void warShouldUseClasses()
    {
        unpack( "surefire-855-failsafe-use-war" ).maven().executeVerify().verifyErrorFree( 2 );
    }

    @Test
    public void jarShouldUseFile()
    {
        unpack( "surefire-855-failsafe-use-jar" )
            .maven().sysProp( "forkMode", "once" ).executeVerify().assertIntegrationTestSuiteResults( 3, 0, 0, 1 );
    }

    @Test
    public void jarNotForkingShouldUseFile()
    {
        unpack( "surefire-855-failsafe-use-jar" )
            .maven().sysProp( "forkMode", "never" ).executeVerify().assertIntegrationTestSuiteResults( 3, 0, 0, 1 );
    }

    @Test
    public void osgiBundleShouldUseFile()
    {
        unpack( "surefire-855-failsafe-use-bundle" ).maven().executeVerify().verifyErrorFree( 2 );
    }
}