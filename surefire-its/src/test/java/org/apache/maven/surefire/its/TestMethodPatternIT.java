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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test project using -Dtest=mtClass#myMethod
 *
 * @author Olivier Lamy
 */
public class TestMethodPatternIT extends SurefireJUnit4IntegrationTestCase {
    private static final String RUNNING_WITH_PROVIDER47 = "parallel='none', perCoreThreadCount=true, threadCount=0";

    private static final String LEGACY_FORK_NODE = "org.apache.maven.plugin.surefire.extensions.LegacyForkNodeFactory";

    private static final String SUREFIRE_FORK_NODE =
            "org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory";

    static Iterable<Object[]> data() {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add(new Object[] {"tcp"});
        args.add(new Object[] {null});
        return args;
    }

    private OutputValidator runMethodPattern(
            String profileId, String projectName, Map<String, String> props, String... goals) throws Exception {
        SurefireLauncher launcher = unpack(projectName, profileId == null ? "" : "-" + profileId);

        if (profileId != null) {
            launcher.activateProfile(profileId);
        }

        for (Entry<String, String> entry : props.entrySet()) {
            launcher.sysProp(entry.getKey(), entry.getValue());
        }
        for (String goal : goals) {
            launcher.addGoal(goal);
        }
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        return launcher.showErrorStackTraces()
                .debugLogging()
                .executeTest()
                .assertTestSuiteResults(2, 0, 0, 0)
                .assertThatLogLine(containsString("Found implementation of fork node factory: " + cls), equalTo(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testJUnit4(String profileId) throws Exception {
        runMethodPattern(profileId, "junit4-method-pattern", Collections.emptyMap());
    }

    @ParameterizedTest
    @MethodSource("data")
    void testJUnit4WithCategoryFilter(String profileId) throws Exception {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        SurefireLauncher launcher = unpack("junit4-method-pattern", profileId == null ? "" : "-" + profileId);

        if (profileId != null) {
            launcher.activateProfile(profileId);
        }

        launcher.debugLogging()
                .addGoal("-Dgroups=junit4.SampleCategory")
                .executeTest()
                .assertTestSuiteResults(1, 0, 0, 0)
                .assertThatLogLine(containsString("Found implementation of fork node factory: " + cls), equalTo(1));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testTestNgMethodBefore(String profileId) throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("testNgVersion", "6.14.3");
        runMethodPattern(profileId, "testng-method-pattern-before", props);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testTestNGMethodPattern(String profileId) throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("testNgVersion", "6.14.3");
        runMethodPattern(profileId, "/testng-method-pattern", props);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testMethodPatternAfter(String profileId) throws Exception {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        SurefireLauncher launcher = unpack("testng-method-pattern-after", profileId == null ? "" : "-" + profileId);

        if (profileId != null) {
            launcher.activateProfile(profileId);
        }

        launcher.debugLogging()
                .sysProp("testNgVersion", "6.14.3")
                .executeTest()
                .verifyErrorFree(2)
                .verifyTextInLog("Called tearDown")
                .assertThatLogLine(containsString("Found implementation of fork node factory: " + cls), equalTo(1));
    }
}
