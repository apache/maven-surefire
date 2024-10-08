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
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for SUREFIRE-2252
 */
public class Surefire2252JUnit5IT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testJUnit5() throws Exception {
        unpack("surefire-2252-junit5-parallel")
                .executeTest()
                .verifyTextInLog(
                        "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider")
                .assertThatLogLine(containsString("Tests run: 1, Failures: 0, Errors: 0, Skipped: 0"), is(2));
    }

    @Test
    public void testJUnit5Xml() {
        OutputValidator validator =
                unpack("surefire-2252-junit5-parallel-xml").executeTest().verifyErrorFree(2);

        validator
                .getSurefireReportsFile("TEST-pkg.domain.AxTest.xml", UTF_8)
                .assertContainsText("tests=\"1\"")
                .assertContainsText("errors=\"0\"")
                .assertContainsText("skipped=\"0\"")
                .assertContainsText("failures=\"0\"");

        validator
                .getSurefireReportsFile("TEST-pkg.domain.BxTest.xml", UTF_8)
                .assertContainsText("tests=\"1\"")
                .assertContainsText("errors=\"0\"")
                .assertContainsText("skipped=\"0\"")
                .assertContainsText("failures=\"0\"");
    }
}
