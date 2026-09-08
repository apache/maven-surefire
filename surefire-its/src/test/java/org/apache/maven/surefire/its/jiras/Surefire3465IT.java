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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

/**
 * Integration Test for #3465.
 */
public class Surefire3465IT extends SurefireJUnit4IntegrationTestCase {
    @Test
    void shouldRunTestsIncludedByPatternWithPackageDirectory() {
        testWithProfile("directory-include");
    }

    @Test
    void shouldRunTestsIncludedByPatternWithWildcardDirectory() {
        testWithProfile("wildcard-directory-include");
    }

    @Test
    void shouldRunTestsIncludedByRegex() {
        testWithProfile("regex-include");
    }

    @Test
    void shouldKeepRunningTestsNotMatchedByExcludePatternWithPackageDirectory() {
        testWithProfile("directory-exclude");
    }

    private void testWithProfile(String profile) {
        OutputValidator outputValidator = unpack("surefire-3465-scan-list-patterns")
                .activateProfile(profile)
                .executeTest()
                .verifyErrorFree(2);

        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3465.inner.IncludedTest.xml")
                .assertFileExists();
        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3465.inner.deep.DeepTest.xml")
                .assertFileExists();
        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3465.other.OtherTest.xml")
                .assertFileNotExists();
    }
}
