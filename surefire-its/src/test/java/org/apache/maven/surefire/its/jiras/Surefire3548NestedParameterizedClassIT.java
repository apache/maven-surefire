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

/**
 * A test in a nested {@code @ParameterizedClass} only exists once the outer class runs. Selecting it
 * with {@code -Dtest} must still run it, two outer values times two inner values.
 *
 * @see <a href="https://github.com/apache/maven-surefire/issues/3548">GitHub issue #3548</a>
 */
public class Surefire3548NestedParameterizedClassIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    void selectedTestInNestedParameterizedClassRuns() {
        unpack("surefire-3548-nested-parameterized-class")
                .setTestToRun("CalculatorParameterizedClassTests$Inner#regularTest")
                .executeTest()
                .verifyErrorFree(4);
    }
}
