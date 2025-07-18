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
import java.util.Map;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static java.lang.Integer.parseInt;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class FailFastJUnitPlatformIT extends AbstractFailFastIT {

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        //                    description    profile     forkCount,
        //                    |              |           |  fail-fast-count,
        //                    |              |           |  |  reuseForks
        //                    |              |           |  |  |      total
        //                    |              |           |  |  |      |  failures
        //                    |              |           |  |  |      |  |  errors
        //                    |              |           |  |  |      |  |  |  skipped
        //                    |              |           |  |  |      |  |  |  |  pipes
        return Arrays.asList( //             |           |  |  |      |  |  |  |  |
                new Object[] {"oneFork-ff1", null, props(1, 1, true), 5, 0, 1, 4, true},
                new Object[] {"oneFork-ff2", null, props(1, 2, true), 5, 0, 2, 3, true},
                new Object[] {"twoForks-ff1", null, props(2, 1, true), 5, 0, 2, 3, true},
                new Object[] {"twoForks-ff2", null, props(2, 2, true), 5, 0, 2, 2, true},
                new Object[] {"oneFork-ff3", null, props(1, 3, true), 5, 0, 2, 0, true},
                new Object[] {"twoForks-ff3", null, props(2, 3, true), 5, 0, 2, 0, true},
                new Object[] {
                    "unsupported-version", null, withUnsupportedJUnitVersion(props(2, 1, true)), 5, 0, 2, 0, true
                });
    }

    private static Map<String, String> withUnsupportedJUnitVersion(Map<String, String> props) {
        props.put("junit.version", "5.13.3");
        return props;
    }

    @Override
    protected String withProvider() {
        return "junit-platform";
    }

    @Override
    @Test
    public void test() throws Exception {
        if (!"unsupported-version".equals(description)) {
            // JUnit 6.0.0 requires Java 17+
            assumeJavaVersion(17);
        }
        super.test();
    }

    @Override
    protected void performExtraChecks(OutputValidator validator) throws Exception {
        if ("unsupported-version".equals(description)) {
            int forkCount = parseInt(properties.get("forkCount"));
            validator.assertThatLogLine(
                    containsString("An attempt was made to cancel the current test run"), equalTo(forkCount));
        }
    }
}
