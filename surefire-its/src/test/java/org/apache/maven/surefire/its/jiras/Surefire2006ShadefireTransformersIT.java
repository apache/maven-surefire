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
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Test for shadefire usage
 *
 * @author Slawomir Jaranowski
 */
public class Surefire2006ShadefireTransformersIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void shadefireShouldBeUsed() throws VerificationException {
        unpack("surefire-2006-shadefire-transformers")
                .debugLogging()
                .executeTest()
                .assertTestSuiteResults(2, 0, 0, 0)
                .assertThatLogLine(
                        containsString(
                                "[INFO] Using configured provider org.apache.maven.shadefire.surefire.junitplatform.JUnitPlatformProvider"),
                        is(1))
                .assertThatLogLine(containsString("[INFO] Running PojoTest"), is(0))
                .assertThatLogLine(containsString("[INFO] Running JUnit4Test"), is(1))
                .assertThatLogLine(containsString("[INFO] Running JUnit5Test"), is(1));
    }
}
