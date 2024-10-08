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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * @author <a href="mailto:owen.farrell@gmail.com">Owen Farrell (owenfarrell)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1383">SUREFIRE-1383</a>
 * @since 2.22.0
 */
public class Surefire1383ScanSessionDependenciesIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void test() throws VerificationException {
        SurefireLauncher launcher = unpack("surefire-1383");
        launcher.executeTest();
        launcher.getSubProjectValidator("sut").assertTestSuiteResults(1, 0, 0, 0);
    }
}
