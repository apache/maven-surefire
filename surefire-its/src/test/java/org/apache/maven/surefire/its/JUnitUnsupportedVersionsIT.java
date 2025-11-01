/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.its;

import java.util.Collection;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import static java.util.Arrays.asList;
import static org.apache.maven.surefire.its.JUnitVersion.JUNIT_3_8_2;
import static org.apache.maven.surefire.its.JUnitVersion.JUNIT_4_11;
import static org.junit.runners.Parameterized.Parameters;

/**
 * Basic suite test using all known versions of JUnit 4.x
 *
 */
@RunWith(Parameterized.class)
public class JUnitUnsupportedVersionsIT extends SurefireJUnit4IntegrationTestCase {

    @Parameters(name = "{index}: JUnit {0}")
    public static Collection<Object[]> junitVersions() {
        return asList(new Object[][] {{JUNIT_3_8_2}, {JUNIT_4_11}});
    }

    @Parameter
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public JUnitVersion version;

    @Test()
    public void testJunitTemp() {
        version.configure(unpack())
                .maven()
                .withFailure()
                .execute("verify")
                .verifyTextInLog(String.format(
                        "The used JUnit Version %s is not supported anymore. Please update to version 4.12+", version));
    }

    private SurefireLauncher unpack() {
        return unpack("/junit-unsupported", version.toString());
    }
}
