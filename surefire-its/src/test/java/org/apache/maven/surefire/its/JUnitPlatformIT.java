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

import java.util.ArrayList;
import java.util.List;

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
public class JUnitPlatformIT extends SurefireJUnit4IntegrationTestCase {
    @Parameter
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String junit5Version;

    @Parameter(1)
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String jqwikVersion;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> artifactVersions() {
        List<Object[]> args = new ArrayList<>();
        args.add(new Object[] {"5.8.2", "1.6.5"});
        args.add(new Object[] {"5.9.1", "1.7.1"});
        args.add(new Object[] {"5.10.2", "1.8.5"});
        args.add(new Object[] {"5.11.4", "1.8.5"});
        args.add(new Object[] {"5.13.4", "1.8.5"});
        return args;
    }

    @Test
    public void testVintageEngine() {
        unpack("junit-platform-engine-vintage", "-" + junit5Version + "-" + jqwikVersion)
                .sysProp("junit5.version", junit5Version)
                .sysProp("jqwik.version", jqwikVersion)
                .executeTest()
                .assertTestSuiteResults(1, 0, 0, 0);
    }

    @Test
    public void testJQwikEngine() {
        unpack("junit-platform-engine-jqwik", "-" + junit5Version + "-" + jqwikVersion)
                .sysProp("junit5.version", junit5Version)
                .sysProp("jqwik.version", jqwikVersion)
                .executeTest()
                .assertTestSuiteResults(1, 0, 0, 0);
    }

    @Test
    public void testMultipleEngines() {
        OutputValidator validator = unpack("junit-platform-multiple-engines", "-" + junit5Version + "-" + jqwikVersion)
                .sysProp("junit5.version", junit5Version)
                .sysProp("jqwik.version", jqwikVersion)
                .executeTest()
                .assertTestSuiteResults(7, 0, 0, 0);

        validator
                .getSurefireReportsFile("TEST-junitplatformenginejupiter.BasicJupiterTest.xml", UTF_8)
                .assertContainsText("<testcase name=\"test(TestInfo)\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int)[1]\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int)[2]\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int)[3]\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"")
                .assertContainsText("<testcase name=\"add(int, int, int)[4]\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"");
    }
}
