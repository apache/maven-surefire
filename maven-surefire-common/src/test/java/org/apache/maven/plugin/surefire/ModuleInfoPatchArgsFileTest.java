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
package org.apache.maven.plugin.surefire;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ModuleInfoPatchArgsFile}.
 */
class ModuleInfoPatchArgsFileTest {
    @TempDir
    Path tempDir;

    private File writeArgs(String content) throws Exception {
        Path metaInf = tempDir.resolve("META-INF/maven");
        Files.createDirectories(metaInf);
        Files.write(metaInf.resolve("module-info-patch.args"), content.getBytes(StandardCharsets.UTF_8));
        return tempDir.toFile();
    }

    @Test
    void shouldReturnNullWithoutFile() throws Exception {
        assertThat(ModuleInfoPatchArgsFile.load(tempDir.toFile())).isNull();
        assertThat(ModuleInfoPatchArgsFile.load(null)).isNull();
    }

    @Test
    void shouldParseSameLineForm() throws Exception {
        File dir = writeArgs("--add-modules org.junit.jupiter.api,org.junit.jupiter.engine\n"
                + "--add-reads com.example=org.junit.jupiter.api\n"
                + "--add-exports com.example/com.example.internal=ALL-UNNAMED\n");

        ModuleInfoPatchArgsFile args = ModuleInfoPatchArgsFile.load(dir);
        assertThat(args).isNotNull();
        assertThat(args.getAddedModules()).containsExactly("org.junit.jupiter.api", "org.junit.jupiter.engine");
        assertThat(args.getLines()).hasSize(3);
    }

    @Test
    void shouldParseTwoLineForm() throws Exception {
        File dir = writeArgs("--add-modules\norg.junit.jupiter.api\n--add-reads\ncom.example=ALL-UNNAMED\n");

        ModuleInfoPatchArgsFile args = ModuleInfoPatchArgsFile.load(dir);
        assertThat(args).isNotNull();
        assertThat(args.getAddedModules()).containsExactly("org.junit.jupiter.api");
    }

    @Test
    void shouldIgnoreCommentsAndUnionMultipleAddModules() throws Exception {
        File dir = writeArgs("# generated\n--add-modules a.b\n\n--add-modules c.d, e.f\n");

        ModuleInfoPatchArgsFile args = ModuleInfoPatchArgsFile.load(dir);
        assertThat(args).isNotNull();
        assertThat(args.getAddedModules()).containsExactly("a.b", "c.d", "e.f");
    }
}
