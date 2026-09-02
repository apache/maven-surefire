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
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;

/**
 * Integration test for https://github.com/apache/maven-surefire/issues/3356:
 * TXT report must count nested tests on the outer class.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire3356NestedTxtReportIT extends SurefireJUnit4IntegrationTestCase {

    @BeforeClass
    public static void setUp() {
        assumeJavaVersion(17);
    }

    @Test
    public void txtReportCountsNestedTestsOnOuterClass() {
        OutputValidator validator =
                unpack("surefire-3356-nested-txt-report").maven().executeTest().verifyErrorFree(3);

        TestFile txt = validator.getSurefireReportsFile("jira3356.NestedOnlyTest.txt");
        txt.assertFileExists();
        txt.assertContainsText("Tests run: 3");

        TestFile xml = validator.getSurefireReportsFile("TEST-jira3356.NestedOnlyTest.xml", UTF_8);
        xml.assertFileExists();
        xml.assertContainsText("tests=\"3\"");
    }
}
