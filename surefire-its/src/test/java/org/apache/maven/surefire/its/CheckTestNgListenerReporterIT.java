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

import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

/**
 * Test simple TestNG listener and reporter
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class CheckTestNgListenerReporterIT extends SurefireJUnitIntegrationTestCase {
    @Parameters(name = "{index}: TestNG {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"6.14.3", null},
            {"7.0.0", null} // Currently latest TestNG version
        });
    }

    @Parameter
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String version;

    @Parameter(1)
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String classifier;

    @Test
    public void testNgListenerReporter() {

        final SurefireLauncher launcher =
                unpack("testng-listener-reporter", "_" + version).sysProp("testNgVersion", version);

        launcher.executeTest()
                .assertTestSuiteResults(1, 0, 0, 0)
                .getTargetFile("resultlistener-output.txt")
                .assertFileExists()
                .getTargetFile("suitelistener-output.txt")
                .assertFileExists()
                .getTargetFile("reporter-output.txt")
                .assertFileExists();
    }
}
