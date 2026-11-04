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
package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.VerificationException;
import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;

/**
 * The main module requires the JUnit API, so JUnit lands on the module path while the provider stays on the
 * class path (#3532).
 */
public class Surefire3532MainModuleRequiresJUnitIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    void junit6() throws VerificationException {
        assumeJavaVersion(17);
        runTests("6.0.3", "17");
    }

    @Test
    void junit5() throws VerificationException {
        assumeJavaVersion(11);
        runTests("5.14.4", "11");
    }

    private void runTests(String junitVersion, String release) throws VerificationException {
        unpack("surefire-3532-main-module-requires-junit", "-" + junitVersion)
                .debugLogging()
                .sysProp("junit.version", junitVersion)
                .sysProp("maven.compiler.release", release)
                .executeTest()
                .verifyErrorFree(3)
                // the args file is only logged when the fork runs on the module path
                .verifyTextInLog("org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED")
                .verifyTextNotInLog("WARNING: Unknown module")
                .verifyTextNotInLog("WARNING: package ");
    }
}
