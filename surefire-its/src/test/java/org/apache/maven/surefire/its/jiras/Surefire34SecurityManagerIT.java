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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaMaxVersion;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SUREFIRE-621 Asserts proper test counts when running junit 3 tests in parallel
 *
 * @author Kristian Rosenvold
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire34SecurityManagerIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testSecurityManager()
    {
        assumeJavaMaxVersion( 17 );
        SurefireLauncher surefireLauncher = unpack( "surefire-34-securityManager" ).failNever();
        surefireLauncher.executeTest().assertTestSuiteResults( 2, 1, 0, 0 );
    }

    @Test
    public void testSecurityManagerSuccessful()
    {
        assumeJavaMaxVersion( 17 );
        SurefireLauncher surefireLauncher = unpack( "surefire-34-securityManager-success" );
        surefireLauncher.executeTest().assertTestSuiteResults( 2, 0, 0, 0 );
    }

    @Test
    public void shouldFailOnJDK()
    {
        assumeJavaVersion( 18 );

        OutputValidator validator = unpack( "surefire-34-securityManager" )
            .failNever()
            .executeTest()
            .verifyTextInLog( "JDK does not support overriding Security Manager with "
                + "a value in system property 'surefire.security.manager'." );

        TestFile xmlReport = validator.getSurefireReportsFile( "junit4.SecurityManagerTest.xml" );

        assertThat( xmlReport.exists() )
            .describedAs( "junit4.SecurityManagerTest.xml should not exist. "
                + "The provider should fail before starting any test." )
            .isFalse();
    }
}
