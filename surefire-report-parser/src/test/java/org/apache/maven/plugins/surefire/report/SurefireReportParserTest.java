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
package org.apache.maven.plugins.surefire.report;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;

import static java.util.Collections.singletonList;

/**
 *
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SurefireReportParserTest extends TestCase {
    public void testParseXMLReportFiles() throws Exception {
        SurefireReportParser parser = new SurefireReportParser(singletonList(getTestDir()), new NullConsoleLogger());

        List<ReportTestSuite> suites = parser.parseXMLReportFiles();

        assertEquals(8, suites.size());

        for (ReportTestSuite suite : suites) {
            assertNotNull(suite.getName() + " was not correctly parsed", suite.getTestCases());
            assertNotNull(suite.getName());
            assertNotNull(suite.getPackageName());
        }
    }

    private File getTestDir() throws UnsupportedEncodingException {
        URL resource = getClass().getResource("/test-reports");
        // URLDecoder.decode necessary for JDK 1.5+, where spaces are escaped to %20
        return new File(URLDecoder.decode(resource.getPath(), "UTF-8")).getAbsoluteFile();
    }

    public void testGetSummary() throws Exception {
        ReportTestSuite tSuite1 = new ReportTestSuite()
                .setNumberOfErrors(10)
                .setNumberOfFailures(20)
                .setNumberOfSkipped(2)
                .setTimeElapsed(1.0f)
                .setNumberOfTests(100);

        ReportTestSuite tSuite2 = new ReportTestSuite()
                .setNumberOfErrors(10)
                .setNumberOfFailures(20)
                .setNumberOfSkipped(2)
                .setTimeElapsed(1.0f)
                .setNumberOfTests(100);

        List<ReportTestSuite> suites = new ArrayList<>();

        suites.add(tSuite1);

        suites.add(tSuite2);

        SurefireReportParser parser = new SurefireReportParser(null, new NullConsoleLogger());

        Map<String, Object> testMap = parser.getSummary(suites);

        assertEquals(20, (int) testMap.get("totalErrors"));

        assertEquals(40, (int) testMap.get("totalFailures"));

        assertEquals(200, (int) testMap.get("totalTests"));

        assertEquals(4, (int) testMap.get("totalSkipped"));

        assertEquals(2.0f, (float) testMap.get("totalElapsedTime"));

        assertEquals(0.68f, (float) testMap.get("totalPercentage"));
    }

    public void testGetSuitesGroupByPackage() {
        ReportTestSuite tSuite1 = new ReportTestSuite();

        ReportTestSuite tSuite2 = new ReportTestSuite();

        ReportTestSuite tSuite3 = new ReportTestSuite();

        tSuite1.setPackageName("Package1");

        tSuite2.setPackageName("Package1");

        tSuite3.setPackageName("Package2");

        List<ReportTestSuite> suites = new ArrayList<>();

        suites.add(tSuite1);

        suites.add(tSuite2);

        suites.add(tSuite3);

        SurefireReportParser parser = new SurefireReportParser(null, new NullConsoleLogger());

        Map<String, List<ReportTestSuite>> groupMap = parser.getSuitesGroupByPackage(suites);

        assertEquals(2, groupMap.size());

        assertEquals(tSuite1, groupMap.get("Package1").get(0));

        assertEquals(tSuite2, groupMap.get("Package1").get(1));

        assertEquals(tSuite3, groupMap.get("Package2").get(0));
    }

    public void testComputePercentage() throws Exception {
        SurefireReportParser parser = new SurefireReportParser(null, new NullConsoleLogger());

        assertEquals(0.7f, (float) parser.computePercentage(100, 20, 10, 0));
    }

    public void testGetFailureDetails() {
        ReportTestSuite tSuite1 = new ReportTestSuite();

        ReportTestSuite tSuite2 = new ReportTestSuite();

        ReportTestCase tCase1 = new ReportTestCase();

        ReportTestCase tCase2 = new ReportTestCase();

        ReportTestCase tCase3 = new ReportTestCase();

        tCase1.setFailure(null, IllegalArgumentException.class.getName());

        tCase3.setFailure("index: 0, size: 0", IndexOutOfBoundsException.class.getName());

        List<ReportTestCase> tCases = new ArrayList<>();

        List<ReportTestCase> tCases2 = new ArrayList<>();

        tCases.add(tCase1);

        tCases.add(tCase2);

        tCases2.add(tCase3);

        tSuite1.setTestCases(tCases);

        tSuite2.setTestCases(tCases2);

        List<ReportTestSuite> suites = new ArrayList<>();

        suites.add(tSuite1);

        suites.add(tSuite2);

        SurefireReportParser parser = new SurefireReportParser(null, new NullConsoleLogger());

        List<ReportTestCase> failures = parser.getFailureDetails(suites);

        assertEquals(2, failures.size());

        assertEquals(tCase1, failures.get(0));

        assertEquals(tCase3, failures.get(1));
    }
}
