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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * A TestNG {@code ISuiteListener} registered through {@code META-INF/services/org.testng.ITestNGListener}
 * should not be called more than needed.
 * <p>
 * The TestNG engine finds tests with a TestNG dry run, and a dry run calls suite listeners just like a real
 * run. Surefire used to discover each scanned class on its own, so the listener was called once per test
 * class, plus once for the discovery before the run, plus once for the run itself. The scan no longer uses
 * the TestNG engine, so we are down to {@value #EXPECTED_SUITE_EVENTS}:
 * <ol>
 *     <li>the discovery done right before the run,</li>
 *     <li>the run of {@code TestNGTest}.</li>
 * </ol>
 * Getting down to one would need a change in the TestNG engine.
 *
 * @see <a href="https://github.com/apache/maven-surefire/issues/3462">GitHub issue #3462</a>
 */
public class Surefire3462TestNgSuiteListenerIT extends SurefireJUnit4IntegrationTestCase {

    private static final int EXPECTED_SUITE_EVENTS = 2;

    @Test
    void suiteListenerIsNotInvokedOncePerScannedClass() throws Exception {
        unpack("surefire-3462-testng-suite-listener")
                .executeTest()
                .verifyErrorFree(2)
                .assertThatLogLine(containsString("SUREFIRE-3462-SUITE-START"), is(EXPECTED_SUITE_EVENTS))
                .assertThatLogLine(containsString("SUREFIRE-3462-SUITE-FINISH"), is(EXPECTED_SUITE_EVENTS));
    }

    /**
     * In a TestNG only project the TestNG engine is the only engine, so skipping it during the scan must
     * not make JUnit warn that all engines were excluded.
     */
    @Test
    void testNgOnlyProjectScansWithoutWarning() throws Exception {
        unpack("surefire-3462-testng-only")
                .executeTest()
                .verifyErrorFree(1)
                .verifyTextNotInLog("All TestEngines were excluded")
                .assertThatLogLine(containsString("SUREFIRE-3462-SUITE-START"), is(EXPECTED_SUITE_EVENTS))
                .assertThatLogLine(containsString("SUREFIRE-3462-SUITE-FINISH"), is(EXPECTED_SUITE_EVENTS));
    }
}
