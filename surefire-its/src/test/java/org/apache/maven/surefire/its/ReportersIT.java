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
import org.junit.Test;

/**
 * Asserts proper behaviour of console output when forking
 * SUREFIRE-679
 *
 * @author Kristian Rosenvold
 */
public class ReportersIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testRedirectOutputTestNg() {
        OutputValidator reporters =
                unpack("reporters").redirectToFile(true).printSummary(true).executeTest();

        reporters.getSurefireReportsFile("TestSuite-output.txt").assertFileExists();
        reporters.getSurefireReportsXmlFile("TEST-TestSuite.xml").assertFileExists();
        reporters.getSurefireReportsFile("TestSuite.txt").assertFileExists();
    }
}
