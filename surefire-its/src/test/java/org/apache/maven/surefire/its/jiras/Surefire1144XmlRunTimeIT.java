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

import java.util.List;

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.extractReports;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Test that runtime reported on console matches runtime in XML
 *
 * @author <a href="mailto:eloussi2@illinois.edu">Lamyaa Eloussi</a>
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire1144XmlRunTimeIT extends SurefireJUnitIntegrationTestCase {
    @Test
    public void testXmlRunTime() {
        OutputValidator outputValidator =
                unpack("/surefire-1144-xml-runtime").forkOnce().executeTest();

        List<ReportTestSuite> reports = extractReports(outputValidator.getBaseDir());
        assertThat(reports, hasSize(1));

        ReportTestSuite report = reports.get(0);
        float xmlTime = report.getTimeElapsed();

        assertThat(xmlTime, is(greaterThanOrEqualTo(1.6f))); // include beforeClass and afterClass
        outputValidator.verifyTextInLog(Float.toString(xmlTime)); // same time in console
    }
}
