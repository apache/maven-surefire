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
import java.util.Objects;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * Test failIfNoTests with various forking strategies.
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestFailIfNoTestsForkCountIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void failIfNoTestsForkAlways() {
        unpack().forkAlways().failIfNoTests(true).maven().withFailure().executeTest();
    }

    @Test
    public void failIfNoTestsForkNever() {
        unpack().forkNever().failIfNoTests(true).maven().withFailure().executeTest();
    }

    @Test
    public void failIfNoTestsForkOnce() {
        unpack().forkOnce().failIfNoTests(true).maven().withFailure().executeTest();
    }

    @Test
    public void dontFailIfNoTestsForkAlways() {
        Assertions.assertThat(unpack().forkAlways()
                        .failIfNoTests(false)
                        .executeTest()
                        .verifyErrorFreeLog()
                        .getSurefireReportsDirectory()
                        .listFiles())
                .isNull();
    }

    @Test
    public void dontFailIfNoTestsForkNever() {
        Assertions.assertThat(unpack().forkNever()
                        .failIfNoTests(false)
                        .executeTest()
                        .verifyErrorFreeLog()
                        .getSurefireReportsDirectory()
                        .listFiles())
                .isNull();
    }

    @Test
    public void dontFailIfNoTestsForkOnce() {
        Assertions.assertThat(Arrays.stream(Objects.requireNonNull(unpack().forkOnce()
                                .failIfNoTests(false)
                                .argLine("-Djdk.net.URLClassPath.disableClassPathURLCheck=true")
                                .executeTest()
                                .verifyErrorFreeLog()
                                .getSurefireReportsDirectory()
                                .listFiles()))
                        //  we may have some files containing
                        //            Boot Manifest-JAR contains absolute paths in classpath
                        // 'D:\a\maven-surefire\maven-surefire\surefire-its\target\CheckTestFailIfNoTestsForkCountIT_dontFailIfNoTestsForkOnce\target\test-classes'
                        //        Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
                        //            'other' has different root
                        .filter(file -> !file.getName().endsWith(".dumpstream")))
                .isEmpty();
    }

    private void doTest(SurefireLauncher launcher) {
        launcher.executeTest().verifyErrorFreeLog().assertTestSuiteResults(0, 0, 0, 0);
    }

    private SurefireLauncher unpack() {
        return unpack("default-configuration-classWithNoTests");
    }
}
