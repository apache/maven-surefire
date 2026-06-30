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

import java.util.Arrays;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Integration Test for #3380
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire3380ForkedSourceQualifiedNameIT extends SurefireJUnit4IntegrationTestCase {
    static Iterable<?> junitJupiterVersions() {
        return Arrays.asList("5.8.2", "5.9.1", "5.13.4");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("junitJupiterVersions")
    void testXmlReport(String jupiterVersion) {
        OutputValidator validator = unpack("surefire-3380-forked-source-qualified-name", "-" + jupiterVersion)
                .setForkJvm()
                .sysProp("junit5.version", jupiterVersion)
                .executeTest()
                .verifyErrorFree(9);

        validator
                .getSurefireReportsFile("TEST-issue3380.NestedJupiterTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level1_test\" " + "classname=\"issue3380.NestedJupiterTest$A\"");

        validator
                .getSurefireReportsFile("TEST-issue3380.NestedJupiterTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level2_test_nonparameterized\" "
                        + "classname=\"issue3380.NestedJupiterTest$B$C\"")
                .assertContainsText("<testcase name=\"level2_test_parameterized(String)[1]\" "
                        + "classname=\"issue3380.NestedJupiterTest$B$C\"")
                .assertContainsText("<testcase name=\"level2_test_parameterized(String)[2]\" "
                        + "classname=\"issue3380.NestedJupiterTest$B$C\"");

        String expectedDisplayNameForNestedClassA = "issue3380.NestedDisplayNameTest$A";

        validator
                .getSurefireReportsFile("TEST-issue3380.NestedDisplayNameTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level1_test_without_display_name\" " + "classname=\""
                        + expectedDisplayNameForNestedClassA + "\"")
                .assertContainsText("<testcase name=\"level1_test_with_display_name\" " + "classname=\""
                        + expectedDisplayNameForNestedClassA + "\"");

        String expectedDisplayNameForNestedClassC = "issue3380.NestedDisplayNameTest$B$C";

        validator
                .getSurefireReportsFile("TEST-issue3380.NestedDisplayNameTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level2_test_nonparameterized\" " + "classname=\""
                        + expectedDisplayNameForNestedClassC + "\"")
                .assertContainsText("<testcase name=\"level2_test_parameterized(String)[1]\" " + "classname=\""
                        + expectedDisplayNameForNestedClassC + "\"")
                .assertContainsText("<testcase name=\"level2_test_parameterized(String)[2]\" " + "classname=\""
                        + expectedDisplayNameForNestedClassC + "\"");
    }
}
