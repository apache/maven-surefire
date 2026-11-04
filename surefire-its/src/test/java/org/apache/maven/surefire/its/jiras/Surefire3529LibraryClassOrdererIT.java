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
import org.apache.maven.surefire.its.fixture.VerificationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * runOrder must not replace a class orderer set in junit-platform.properties, like the one Quarkus ships (#3529).
 */
public class Surefire3529LibraryClassOrdererIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    void runOrderShouldKeepClassOrdererFromJunitPlatformProperties() throws VerificationException {
        OutputValidator validator = unpack("surefire-3529-library-class-orderer")
                .runOrder("alphabetical")
                .executeTest()
                .verifyErrorFree(3);

        // runOrder alone gives A, B, C. Our orderer reverses them.
        assertTrue(validator.stringsAppearInSpecificOrderInLog(new String[] {"ran CTest", "ran BTest", "ran ATest"}));
    }
}
