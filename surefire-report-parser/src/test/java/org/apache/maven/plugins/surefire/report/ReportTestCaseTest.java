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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jontri
 */
@SuppressWarnings("checkstyle:magicnumber")
class ReportTestCaseTest {
    private ReportTestCase tCase;

    @BeforeEach
    void setUp() {
        tCase = new ReportTestCase();
    }

    @Test
    void testSetName() {
        tCase.setName("Test Case Name");

        assertEquals("Test Case Name", tCase.getName());
    }

    @Test
    void testSetTime() {
        tCase.setTime(.06f);

        assertEquals(.06f, tCase.getTime(), 0.0);
    }

    @Test
    void testSetFailure() {
        tCase.setFailure("messageVal", "typeVal");

        assertEquals("messageVal", tCase.getFailureMessage());
        assertEquals("typeVal", tCase.getFailureType());
    }

    @Test
    void testSetFullName() {
        tCase.setFullName("Test Case Full Name");

        assertEquals("Test Case Full Name", tCase.getFullName());
    }
}
