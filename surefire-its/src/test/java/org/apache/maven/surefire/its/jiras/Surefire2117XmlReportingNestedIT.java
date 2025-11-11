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
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Integration Test for SUREFIRE-2117
 */
@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire2117XmlReportingNestedIT extends SurefireJUnitIntegrationTestCase {
    @Parameter
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String jupiterVersion;

    @Parameters(name = "{0}")
    public static Iterable<?> junitJupiterVersions() {
        return Arrays.asList("5.8.2", "5.9.1", "5.13.4");
    }

    @Test
    public void testXmlReport() {
        OutputValidator validator = unpack("surefire-2117-xml-reporting-nested", "-" + jupiterVersion)
                .sysProp("junit5.version", jupiterVersion)
                .executeTest()
                .verifyErrorFree(9);

        validator
                .getSurefireReportsFile("TEST-jira2117.NestedJupiterTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level1_test\" " + "classname=\"jira2117.NestedJupiterTest$A\"");

        validator
                .getSurefireReportsFile("TEST-jira2117.NestedJupiterTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level2_test_nonparameterized\" "
                        + "classname=\"jira2117.NestedJupiterTest$B$C\"")
                .assertContainsText("<testcase name=\"level2_test_parameterized(String)[1] paramValue1\" "
                        + "classname=\"jira2117.NestedJupiterTest$B$C\"")
                .assertContainsText("<testcase name=\"level2_test_parameterized(String)[2] paramValue2\" "
                        + "classname=\"jira2117.NestedJupiterTest$B$C\"");

        String expectedDisplayNameForNestedClassA = "Display name of level 1 nested class A";

        validator
                .getSurefireReportsFile("TEST-jira2117.NestedDisplayNameTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"level1_test_without_display_name\" " + "classname=\""
                        + expectedDisplayNameForNestedClassA + "\"")
                .assertContainsText("<testcase name=\"Display name of level 1 test method\" " + "classname=\""
                        + expectedDisplayNameForNestedClassA + "\"");

        String expectedDisplayNameForNestedClassC = "Display name of level 2 nested class C";

        validator
                .getSurefireReportsFile("TEST-jira2117.NestedDisplayNameTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"Display name of non-parameterized level 2 test method\" "
                        + "classname=\"" + expectedDisplayNameForNestedClassC + "\"")
                .assertContainsText(
                        "<testcase name=\"Display name of parameterized level 2 test method[1] paramValue1\" "
                                + "classname=\"" + expectedDisplayNameForNestedClassC + "\"")
                .assertContainsText(
                        "<testcase name=\"Display name of parameterized level 2 test method[2] paramValue2\" "
                                + "classname=\"" + expectedDisplayNameForNestedClassC + "\"");
    }
}
