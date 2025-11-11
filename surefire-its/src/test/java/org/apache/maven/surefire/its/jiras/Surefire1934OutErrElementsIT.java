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
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

/**
 * Test for SUREFIRE-1934. Enabling and disabling system-out and system-err elements in plugin configuration.
 *
 * @author NissMoony
 */
public class Surefire1934OutErrElementsIT extends SurefireJUnitIntegrationTestCase {

    @Test
    public void testOutErrElementsDisabled() {
        final OutputValidator outputValidator = unpack("/surefire-1934-disable-out-err-elements")
                .maven()
                .withFailure()
                .executeTest();
        final TestFile reportFile =
                outputValidator.getSurefireReportsXmlFile("TEST-disableOutErrElements.TestOutErrElements.xml");

        reportFile.assertNotContainsText("<system-out><![CDATA[System-out output not expected in the report.");
        reportFile.assertNotContainsText(
                "<system-err><![CDATA[[main] INFO disableOutErrElements.TestOutErrElements - Log output not expected in test report.");
        reportFile.assertContainsText("<system-out><![CDATA[System-out output expected in the report.");
        reportFile.assertContainsText(
                "<system-err><![CDATA[[main] INFO disableOutErrElements.TestOutErrElements - Log output expected in test report.");
    }

    @Test
    public void testOutErrElementsEnabled() {
        final OutputValidator outputValidator =
                unpack("/surefire-1934-enable-out-err-elements").executeTest();
        final TestFile reportFile =
                outputValidator.getSurefireReportsXmlFile("TEST-enableOutErrElements.TestOutErrElements.xml");

        reportFile.assertContainsText("<system-out>");
        reportFile.assertContainsText("<system-err>");
    }
}
