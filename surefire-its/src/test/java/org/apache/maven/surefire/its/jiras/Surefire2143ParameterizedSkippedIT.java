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
import java.util.List;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

/**
 * Integration Test for SUREFIRE-2143
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire2143ParameterizedSkippedIT extends SurefireJUnitIntegrationTestCase {
    @Test
    public void junit5ParameterizedSkipped() {
        OutputValidator validator = unpack("surefire-2143-junit5-parameterized-test-skipped")
                .executeTest()
                .assertTestSuiteResults(5, 0, 0, 2);

        String xmlReport = validator
                .getSurefireReportsFile("TEST-jira2143.DisabledParameterizedTest.xml", UTF_8)
                .readFileToString();

        List<String> testCaseResults = Arrays.asList(xmlReport
                .substring(xmlReport.indexOf("<testcase "), xmlReport.indexOf("</testsuite>"))
                .split(".(?=<testcase )"));

        assertThat(testCaseResults)
                .hasSize(5)
                .filteredOn(testCaseResult -> testCaseResult.contains("<skipped"))
                .map(testCaseResult -> testCaseResult.substring(0, testCaseResult.indexOf("classname")))
                .containsExactlyInAnyOrder(
                        "<testcase name=\"disabledParameterized(String)\" ",
                        "<testcase name=\"disabledNonParameterized\" ");

        validator
                .getSurefireReportsFile("TEST-jira2143.DisabledParameterizedTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"disabledParameterized(String)\" "
                        + "classname=\"jira2143.DisabledParameterizedTest\"")
                .assertContainsText("<testcase name=\"disabledNonParameterized\" "
                        + "classname=\"jira2143.DisabledParameterizedTest\"")
                .assertContainsText(matchesRegex(".*<skipped [^>]*disabledParameterized.*"))
                .assertContainsText(matchesRegex(".*<skipped [^>]*disabledNonParameterized.*"))
                .assertContainsText("<testcase name=\"enabledParameterized(String)[1]\"")
                .assertContainsText("<testcase name=\"enabledParameterized(String)[2]\"")
                .assertContainsText("<testcase name=\"enabledNonParameterized\"")
                .assertNotContainsText(matchesRegex(".*<skipped [^>]*enabledParameterized.*"))
                .assertNotContainsText(matchesRegex(".*<skipped [^>]*enabledNonParameterized.*"));
    }

    @Test
    public void junit4ParameterizedSkipped() {
        OutputValidator validator = unpack("surefire-2143-junit4-parameterized-test-skipped")
                .executeTest()
                .assertTestSuiteResults(4, 0, 0, 2);

        String xmlReport = validator
                .getSurefireReportsFile("TEST-jira2143.IgnoredParameterizedTest.xml", UTF_8)
                .readFileToString();

        List<String> testCaseResults = Arrays.asList(xmlReport
                .substring(xmlReport.indexOf("<testcase "), xmlReport.indexOf("</testsuite>"))
                .split(".(?=<testcase )"));

        assertThat(testCaseResults)
                .hasSize(4)
                .filteredOn(testCaseResult -> testCaseResult.contains("<skipped"))
                .map(testCaseResult -> testCaseResult.substring(0, testCaseResult.indexOf("classname")))
                .containsExactlyInAnyOrder(
                        "<testcase name=\"ignoredParameterized[0]\" ", "<testcase name=\"ignoredParameterized[1]\" ");
    }
}
