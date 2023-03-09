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
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test for SUREFIRE-2032
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire2032NestedSkippedIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testXmlReport() {
        OutputValidator validator =
                unpack("surefire-2032-nested-test-class-skipped").executeTest().assertTestSuiteResults(4, 0, 0, 2);

        String redXmlReport = validator
                .getSurefireReportsFile("TEST-jira2032.DisabledNestedTest$RedTaggedEnabledTest.xml", UTF_8)
                .readFileToString();

        // Enabled nested subclass
        List<String> redTestCaseResults = Arrays.asList(redXmlReport
                .substring(redXmlReport.indexOf("<testcase "), redXmlReport.indexOf("</testsuite>"))
                .split(".(?=<testcase )"));

        assertThat(redTestCaseResults)
                .hasSize(2)
                .filteredOn(testCaseResult -> testCaseResult.contains("<skipped"))
                .isEmpty();

        // Disabled nested subclass
        String orangeXmlReport = validator
                .getSurefireReportsFile("TEST-jira2032.DisabledNestedTest$OrangeTaggedDisabledTest.xml", UTF_8)
                .readFileToString();

        List<String> orangeTestCaseResults = Arrays.asList(orangeXmlReport
                .substring(orangeXmlReport.indexOf("<testcase "), orangeXmlReport.indexOf("</testsuite>"))
                .split(".(?=<testcase )"));

        assertThat(orangeTestCaseResults)
                .hasSize(2)
                .filteredOn(testCaseResult -> testCaseResult.contains("<skipped"))
                .map(testCaseResult -> testCaseResult.substring(0, testCaseResult.indexOf("classname")))
                .containsExactlyInAnyOrder("<testcase name=\"test1\" ", "<testcase name=\"test2\" ");
    }
}
