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
package org.apache.maven.surefire.api.report;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TODO see the Disabled annotation
 * @author Ashley Scopes
 */
@Disabled("This test class was not running for long, hard to know if it's still valid or not")
public class CategorizedReportEntryTest {
    @Test
    public void testGetReportNameWithGroupWhenSourceTextIsNull() {
        String className = "ClassName";
        String classText = null;
        String groupName = "The Group Name";
        ReportEntry reportEntry =
                new CategorizedReportEntry(NORMAL_RUN, 1L, className, classText, groupName, null, null);
        assertEquals("ClassName (of The Group Name)", reportEntry.getReportNameWithGroup());
    }

    @Test
    public void testGetReportNameWithGroupWhenSourceTextIsEmpty() {
        String className = "ClassName";
        String classText = "";
        String groupName = "The Group Name";
        ReportEntry reportEntry =
                new CategorizedReportEntry(NORMAL_RUN, 1L, className, classText, groupName, null, null);
        assertEquals("ClassName (of The Group Name)", reportEntry.getReportNameWithGroup());
    }

    @Test
    public void testGetReportNameWithGroupWhenSourceTextIsBlank() {
        String className = "ClassName";
        String classText = "  ";
        String groupName = "The Group Name";
        ReportEntry reportEntry =
                new CategorizedReportEntry(NORMAL_RUN, 1L, className, classText, groupName, null, null);
        assertEquals("ClassName (of The Group Name)", reportEntry.getReportNameWithGroup());
    }

    @Test
    public void testGetReportNameWithGroupWhenSourceTextIsProvided() {
        String className = "ClassName";
        String classText = "The Class Name";
        String groupName = "The Group Name";
        ReportEntry reportEntry =
                new CategorizedReportEntry(NORMAL_RUN, 1L, className, classText, groupName, null, null);
        assertEquals("The Class Name (of The Group Name)", reportEntry.getReportNameWithGroup());
    }
}
