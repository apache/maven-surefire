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

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Before;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;

/**
 * Verify that Surefire adds Maven dependencies to the Module Path if they <code>provide</code> a service that is
 * <code>use</code> 'd by another JPMS module that is already on the Module Path.
 *
 * @author mthmulders
 */
public class Surefire1993JpmsProvidingModulesIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Before
    public void setUp()
    {
        assumeJavaVersion( 9 );
    }

    @Test
    public void verify() throws VerificationException
    {
        SurefireLauncher launcher = unpack( "surefire-1993-jpms-providing-modules" );

        launcher.debugLogging()
                .executeVerify();

        launcher.getSubProjectValidator( "application" )
                .verifyErrorFreeLog()
                .assertIntegrationTestSuiteResults( 1, 0, 0, 0 );
    }
}
