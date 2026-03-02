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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReportTestTimestampIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testReportTestTimestampDisabled() {
        OutputValidator outputValidator = unpack("/disable-timestamp-element").executeTest();
        TestFile reportFile = outputValidator.getSurefireReportsXmlFile("TEST-TestA.xml");

        String xml = reportFile.slurpFile();

        // testsuite has no timestamp
        assertFalse(
                "Unexpected timestamp on testsuite", xml.matches(".*<testsuite[^>]*\\btimestamp=\"[^\"]+\"[^>]*>.*"));

        // no testcase has timestamp
        int testcaseWithTsCount = countMatches(xml, "<testcase[^>]*\\btimestamp=\"[^\"]+\"");
        assertEquals("Unexpected timestamp on testcase", 0, testcaseWithTsCount);
    }

    @Test
    public void testReportTestTimestampEnabled() {
        OutputValidator outputValidator = unpack("/enable-timestamp-element").executeTest();
        TestFile reportFile = outputValidator.getSurefireReportsXmlFile("TEST-TestA.xml");

        String xml = reportFile.slurpFile();

        // testsuite has timestamp
        assertTrue("Missing timestamp on testsuite", xml.matches(".*<testsuite[^>]*\\btimestamp=\"[^\"]+\"[^>]*>.*"));

        // each testcase has timestamp
        int testcaseCount = countMatches(xml, "<testcase");
        int testcaseWithTsCount = countMatches(xml, "<testcase[^>]*\\btimestamp=\"[^\"]+\"");
        assertEquals("Not all testcases have timestamp", testcaseCount, testcaseWithTsCount);

        assertAllTimestampsIso(xml);
    }

    private static int countMatches(String input, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(input);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static void assertAllTimestampsIso(String xml) {
        Matcher matcher = Pattern.compile("timestamp=\"([^\"]+)\"").matcher(xml);
        while (matcher.find()) {
            String timestamp = matcher.group(1);
            assertTrue("Invalid ISO timestamp: " + timestamp, isIsoTimestamp(timestamp));
        }
    }

    private static boolean isIsoTimestamp(String timestamp) {
        try {
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestamp, OffsetDateTime::from);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
