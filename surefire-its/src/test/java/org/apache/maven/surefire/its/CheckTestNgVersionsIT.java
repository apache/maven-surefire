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

import java.util.List;

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.surefire.its.fixture.HelperAssertions;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Basic suite test using all known versions of TestNG. Used for regression testing Surefire against old versions. To
 * check new versions of TestNG work with current versions of Surefire, instead run the full test suite with
 * -Dtestng.version=5.14.2 (for example)
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestNgVersionsIT extends SurefireJUnit4IntegrationTestCase {

    // TestNG 7.6 and above needs JDK11
    @Test
    public void test751() {
        runTestNgTestWithRunOrder("7.5.1");
    }

    @Test
    public void test6143() {
        runTestNgTestWithRunOrder("6.14.3");
    }

    @Test
    public void test69136() {
        runTestNgTestWithRunOrder("6.9.13.6");
    }

    @Test
    public void test6821() {
        runTestNgTestWithRunOrder("6.8.21");
    }

    private void runTestNgTestWithRunOrder(String version) {
        runTestNgTest(version, null, true);
    }

    private void runTestNgTest(String version) {
        runTestNgTest(version, null, false);
    }

    private void runTestNgTest(String version, String classifier) {
        runTestNgTest(version, classifier, false);
    }

    private void runTestNgTest(String version, String classifier, boolean validateRunOrder) {
        final SurefireLauncher launcher = unpack("testng-simple").sysProp("testNgVersion", version);

        if (classifier != null) {
            launcher.sysProp("testNgClassifier", classifier);
        }

        final OutputValidator outputValidator = launcher.executeTest();

        outputValidator.assertTestSuiteResults(3, 0, 0, 0);

        if (validateRunOrder) {
            // assert correct run order of tests
            List<ReportTestSuite> report = HelperAssertions.extractReports(outputValidator.getBaseDir());

            assertEquals(3, report.size());

            assertTrue(
                    "TestNGSuiteTestC was executed first",
                    getTestClass(report, 0).endsWith("TestNGSuiteTestC"));

            assertTrue(
                    "TestNGSuiteTestB was executed second",
                    getTestClass(report, 1).endsWith("TestNGSuiteTestB"));

            assertTrue(
                    "TestNGSuiteTestA was executed last",
                    getTestClass(report, 2).endsWith("TestNGSuiteTestA"));
        }
    }

    private String getTestClass(List<ReportTestSuite> report, int i) {
        return report.get(i).getFullClassName();
    }
}
