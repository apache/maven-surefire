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
package org.apache.maven.plugin.surefire.lastfailed;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class RunInProgressMarkerTest {
    @TempDir
    Path tempDir;

    @Test
    void sitsNextToTheReportsDirectory() {
        Path reportsDirectory = tempDir.resolve("failsafe-reports");

        assertThat(RunInProgressMarker.of(reportsDirectory.toFile()).getFile())
                .isEqualTo(tempDir.resolve(".failsafe-reports-run-in-progress").toFile());
    }

    @Test
    void listsTheTestClassesOfTheRun() throws IOException {
        Path reportsDirectory = tempDir.resolve("surefire-reports");
        RunInProgressMarker marker = RunInProgressMarker.of(reportsDirectory.toFile());

        marker.write(asList("pkg.ATest", "pkg.BTest"));

        assertThat(marker.exists()).isTrue();
        assertThat(marker.readTestClasses()).containsExactly("pkg.ATest", "pkg.BTest");
        assertThat(marker.getStartTime()).isPositive();
        // the reports directory is left alone
        assertThat(reportsDirectory).doesNotExist();

        marker.delete();

        assertThat(marker.exists()).isFalse();
    }
}
