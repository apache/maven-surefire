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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

/**
 * Integration test for SUREFIRE-2300 / GitHub issue #2602.
 */
public class Surefire2300BeforeAllAssumptionIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void reportsTestsAbortedByBeforeAllAssumptionAsSkipped() {
        OutputValidator validator = unpack("surefire-2300-before-all-assumption")
                .executeTest()
                .assertTestSuiteResults(2, 0, 0, 2)
                .verifyTextInLog("Tests run: 2, Failures: 0, Errors: 0, Skipped: 2");

        validator
                .getSurefireReportsXmlFile("TEST-example.TestAssume.xml")
                .assertContainsText("tests=\"2\" errors=\"0\" skipped=\"2\" failures=\"0\"")
                .assertContainsText("name=\"testOne\"")
                .assertContainsText("name=\"testTwo\"")
                .assertContainsText("<skipped type=\"org.opentest4j.TestAbortedException\">")
                .assertContainsText("Assumption failed: assumption is not true");
    }
}
