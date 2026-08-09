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

import java.io.File;
import java.io.IOException;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.jupiter.api.Test;

import static java.nio.file.Files.createDirectories;

public class Surefire2113TempDirIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void tempDirUsesJavaIoTmpDir() throws IOException {
        SurefireLauncher launcher = unpack("surefire-2113-temp-dir").setJUnitVersion("5.9.0");
        File customTempDir = new File(launcher.getUnpackedAt(), "custom-temp");
        createDirectories(customTempDir.toPath());

        launcher.addGoal("-Djava.io.tmpdir=" + customTempDir.getAbsolutePath())
                .executeTest()
                .assertTestSuiteResults(1, 0, 0, 0);
    }

    @Test
    public void argLineOverridesEffectiveJavaIoTmpDir() throws IOException {
        SurefireLauncher launcher = unpack("surefire-2113-temp-dir").setJUnitVersion("5.9.0");
        File effectiveTempDir = new File(launcher.getUnpackedAt(), "effective-temp");
        File argLineTempDir = new File(launcher.getUnpackedAt(), "arg-line-temp");
        createDirectories(effectiveTempDir.toPath());
        createDirectories(argLineTempDir.toPath());

        launcher.addGoal("-Djava.io.tmpdir=" + effectiveTempDir.getAbsolutePath())
                .addGoal("-DargLine=-Djava.io.tmpdir=" + argLineTempDir.getAbsolutePath())
                .addGoal("-Dexpected.java.io.tmpdir=" + argLineTempDir.getAbsolutePath())
                .executeTest()
                .assertTestSuiteResults(1, 0, 0, 0);
    }
}
