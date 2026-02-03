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

import java.util.Arrays;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 */
@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire1914XmlReportingParameterizedTestIT4 extends SurefireJUnit4IntegrationTestCase {
    @Parameter
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String jupiterVersion;

    @Parameters(name = "{0}")
    public static Iterable<?> junitJupiterVersions() {
        return Arrays.asList("5.8.2", "5.9.1", "5.13.4");
    }

    @Test
    public void testXmlReport() {
        OutputValidator validator = unpack("surefire-1914-xml-reporting-parameterizedtest", "-" + jupiterVersion)
                .sysProp("junit5.version", jupiterVersion)
                .executeTest()
                .verifyErrorFree(16);

        validator
                .getSurefireReportsFile("TEST-jira1914.ParameterizedDisplayNameTest.xml", UTF_8)
                .assertContainsText("testcase name=\"theDisplayNameOfTestMethod1[1] a\" "
                        + "classname=\"theDisplayNameOfTheClass\"")
                .assertContainsText("testcase name=\"theDisplayNameOfTestMethod1[2] b\" "
                        + "classname=\"theDisplayNameOfTheClass\"")
                .assertContainsText("testcase name=\"theDisplayNameOfTestMethod1[3] c\" "
                        + "classname=\"theDisplayNameOfTheClass\"")
                .assertContainsText("testcase name=\"theDisplayNameOfTestMethod2 with param a\" "
                        + "classname=\"theDisplayNameOfTheClass\"")
                .assertContainsText("testcase name=\"theDisplayNameOfTestMethod2 with param b\" "
                        + "classname=\"theDisplayNameOfTheClass\"")
                .assertContainsText("testcase name=\"theDisplayNameOfTestMethod2 with param c\" "
                        + "classname=\"theDisplayNameOfTheClass\"");

        validator
                .getSurefireReportsFile("TEST-jira1914.ParameterizedJupiterTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"add(int, int, int) 0 + 1 = 1\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int) 1 + 2 = 3\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int) 49 + 51 = 100\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int) 1 + 100 = 101\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"square(int, int)[1] 1, 1\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"square(int, int)[2] 2, 4\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"square(int, int)[3] 3, 9\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"cube(int, int)[1] 1, 1\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"cube(int, int)[2] 2, 8\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"")
                .assertContainsText("<testcase name=\"cube(int, int)[3] 3, 27\" "
                        + "classname=\"jira1914.ParameterizedJupiterTest\"");
    }
}
