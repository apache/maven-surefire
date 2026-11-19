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
package org.apache.maven.plugin.surefire.report;

import org.junit.jupiter.api.Test;

import static org.apache.maven.plugin.surefire.report.FileReporterUtils.makeFileName;
import static org.apache.maven.plugin.surefire.report.FileReporterUtils.truncateFileName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SUREFIRE-2233: a report file name derived from a long, deeply nested test class name must not
 * exceed common filesystem limits (e.g. 255 bytes on ext4, less on eCryptfs).
 */
public class FileReporterUtilsTest {

    @Test
    public void shortFileNameIsLeftUnchanged() {
        assertEquals("org.example.MyTest.txt", truncateFileName("org.example.MyTest.txt"));
    }

    @Test
    public void makeFileNameStripsIllegalCharsAndTruncates() {
        // ':' is an illegal filename character on both Windows and non-Windows
        String longName = "p1:illegal." + repeat("subpackage", 20) + "TestWithAVeryLongName.txt";

        String fileName = makeFileName(longName);

        assertTrue(fileName.length() < longName.length(), "name should have been shortened");
        assertTrue(fileName.endsWith(".txt"), "extension should be preserved");
        assertTrue(fileName.indexOf(':') == -1, "illegal chars should be stripped");
    }

    @Test
    public void longFileNameIsTruncatedAndKeepsItsExtension() {
        String longName = "p1." + repeat("subpackage", 20) + "TestWithAVeryLongName.txt";

        String truncated = truncateFileName(longName);

        assertTrue(truncated.length() < longName.length(), "name should have been shortened");
        assertTrue(truncated.endsWith(".txt"), "extension should be preserved");
    }

    @Test
    public void differentLongFileNamesTruncateToDifferentNames() {
        String prefix = "p1." + repeat("subpackage", 20);
        String longName1 = prefix + "TestOne.txt";
        String longName2 = prefix + "TestTwo.txt";

        assertEquals(
                longName1.substring(0, 210),
                longName2.substring(0, 210),
                "test fixture should share a common prefix longer than the truncation limit");

        String truncated1 = truncateFileName(longName1);
        String truncated2 = truncateFileName(longName2);

        assertTrue(!truncated1.equals(truncated2), "distinct long names must not collide after truncation");
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s).append('.');
        }
        return sb.toString();
    }
}
