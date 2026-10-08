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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/**
 * JUnit 3 suites nested inside one test class, like Guava's testlib builds them, are reported as that one test
 * class. Each nested suite used to be its own test set, which rewrote the class report over and over (#3511).
 */
public class Surefire3511NestedSuitesIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    void reportsNestedSuitesAsOneTestClass() throws Exception {
        OutputValidator validator =
                unpack("surefire-3511-nested-suites").executeTest().verifyErrorFree(9);

        validator
                .assertThatLogLine(containsString("Running issue3511.CollectionSuiteTest"), is(1))
                .assertThatLogLine(containsString("Running issue3511.SizeTester"), is(0))
                .assertThatLogLine(containsString("Running issue3511.ContainsTester"), is(0))
                .assertThatLogLine(
                        containsString("Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed"), is(1));

        validator
                .getSurefireReportsFile("issue3511.CollectionSuiteTest.txt", UTF_8)
                .assertContainsText("Tests run: 9, Failures: 0, Errors: 0, Skipped: 0");
        validator
                .getSurefireReportsFile("TEST-issue3511.CollectionSuiteTest.xml", UTF_8)
                .assertContainsText("tests=\"9\"");
    }
}
