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
 * must not be notified more often than the number of TestNG runs actually performed.
 * <p>
 * The TestNG engine starts a real TestNG suite whenever it is asked to <em>discover</em> tests, because it
 * relies on TestNG's dry-run mode to tell test classes from non-test classes. Every extra discovery pass
 * therefore costs one additional {@code onStart()}/{@code onFinish()} pair. Surefire used to discover once
 * per scanned class, so the listener was invoked once per test class plus once for the execution request
 * plus once for the actual run.
 * <p>
 * The expected count of {@value #EXPECTED_SUITE_EVENTS} is made up of:
 * <ol>
 *     <li>one class path scan, which now discovers all candidate classes in a single request,</li>
 *     <li>one discovery performed as part of the execution request,</li>
 *     <li>one real execution of {@code TestNGTest}.</li>
 * </ol>
 * Getting rid of the second pair requires the TestNG engine to stop notifying user listeners while it is
 * discovering, which is outside of Surefire's control.
 *
 * @see <a href="https://github.com/apache/maven-surefire/issues/3462">GitHub issue #3462</a>
 */
public class Surefire3462TestNgSuiteListenerIT extends SurefireJUnit4IntegrationTestCase {

    private static final int EXPECTED_SUITE_EVENTS = 3;

    @Test
    void suiteListenerIsNotInvokedOncePerScannedClass() throws Exception {
        unpack("surefire-3462-testng-suite-listener")
                .executeTest()
                .verifyErrorFree(2)
                .assertThatLogLine(containsString("SUREFIRE-3462-SUITE-START"), is(EXPECTED_SUITE_EVENTS))
                .assertThatLogLine(containsString("SUREFIRE-3462-SUITE-FINISH"), is(EXPECTED_SUITE_EVENTS));
    }
}
