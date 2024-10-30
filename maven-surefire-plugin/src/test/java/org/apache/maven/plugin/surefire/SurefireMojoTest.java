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
package org.apache.maven.plugin.surefire;

import java.io.File;

import junit.framework.TestCase;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.api.suite.RunResult;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class SurefireMojoTest extends TestCase {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    public void testDefaultIncludes() {
        assertThat(new SurefireMojo().getDefaultIncludes())
                .containsOnly("**/Test*.java", "**/*Test.java", "**/*Tests.java", "**/*TestCase.java");
    }

    public void testReportSchemaLocation() {
        assertThat(new SurefireMojo().getReportSchemaLocation())
                .isEqualTo("https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd");
    }

    public void testFailIfNoTests() throws Exception {
        RunResult runResult = new RunResult(0, 0, 0, 0);
        try {
            SurefireMojo mojo = new SurefireMojo();
            mojo.setFailIfNoTests(true);
            mojo.handleSummary(runResult, null);
        } catch (MojoFailureException e) {
            assertThat(e.getLocalizedMessage())
                    .isEqualTo("No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)");
            return;
        }
        fail("Expected MojoFailureException with message "
                + "'No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)'");
    }

    public void testTestFailure() throws Exception {
        RunResult runResult = new RunResult(1, 0, 1, 0);
        try {
            SurefireMojo mojo = new SurefireMojo();
            mojo.handleSummary(runResult, null);
        } catch (MojoFailureException e) {
            assertThat(e.getLocalizedMessage())
                    .isEqualTo("There are test failures.\n\nSee null "
                            + "for the individual test results.\nSee dump files (if any exist) "
                            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream.");
            return;
        }
        fail("Expected MojoFailureException with message "
                + "'There are test failures.\n\nSee null "
                + "for the individual test results.\nSee dump files (if any exist) "
                + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream.'");
    }

    public void testPluginName() {
        assertThat(new SurefireMojo().getPluginName()).isEqualTo("surefire");
    }

    public void testShouldGetNullEnv() {
        SurefireMojo mojo = new SurefireMojo();
        assertThat(mojo.getExcludedEnvironmentVariables()).hasSize(0);
    }

    public void testShouldGetEnv() {
        SurefireMojo mojo = new SurefireMojo();
        mojo.setExcludedEnvironmentVariables(new String[] {"ABC", "KLM"});
        assertThat(mojo.getExcludedEnvironmentVariables()).hasSize(2).contains("ABC", "KLM");
    }

    public void testShouldGetPropertyFile() {
        SurefireMojo mojo = new SurefireMojo();
        mojo.setSystemPropertiesFile(new File("testShouldGetPropertyFile"));
        assertThat(mojo.getSystemPropertiesFile()).isEqualTo(new File("testShouldGetPropertyFile"));
    }

    public void testNegativeFailOnFlakeCount() {
        SurefireMojo mojo = new SurefireMojo();
        mojo.setFailOnFlakeCount(-1);
        e.expect(MojoFailureException.class);
        e.expectMessage("Parameter \"failOnFlakeCount\" should not be negative.");
    }

    public void testFailOnFlakeCountWithoutRerun() {
        SurefireMojo mojo = new SurefireMojo();
        mojo.setFailOnFlakeCount(1);
        e.expect(MojoFailureException.class);
        e.expectMessage("\"failOnFlakeCount\" requires rerunFailingTestsCount to be at least 1.");
    }

    public void testShouldHaveJUnit5EnginesFilter() {
        SurefireMojo mojo = new SurefireMojo();

        mojo.setIncludeJUnit5Engines(new String[] {"e1", "e2"});
        assertThat(mojo.getIncludeJUnit5Engines()).isEqualTo(new String[] {"e1", "e2"});

        mojo.setExcludeJUnit5Engines(new String[] {"e1", "e2"});
        assertThat(mojo.getExcludeJUnit5Engines()).isEqualTo(new String[] {"e1", "e2"});
    }
}
