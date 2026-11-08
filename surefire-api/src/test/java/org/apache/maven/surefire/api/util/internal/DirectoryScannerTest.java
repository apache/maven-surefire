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
package org.apache.maven.surefire.api.util.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DirectoryScannerTest {

    @TempDir
    Path basedir;

    @BeforeEach
    void createTestData() throws IOException {
        for (String f : new String[] {
            "file1.txt",
            "file2.txt",
            "folder1/file3.txt",
            "folder1/file4.dat",
            "folder1/sub/File5Test.class",
            "folder2/File6Test.class",
            "folder2/Helper.class",
            ".hidden/file7.txt"
        }) {
            Path p = basedir.resolve(f);
            Files.createDirectories(p.getParent());
            Files.write(p, new byte[0]);
        }
    }

    private String[] scan(String[] includes, String[] excludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(basedir.toFile());
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private static String p(String slashPath) {
        return slashPath.replace('/', File.separatorChar);
    }

    @Test
    void includesEverythingByDefault() {
        assertThat(scan(null, null))
                .containsExactlyInAnyOrder(
                        p("file1.txt"),
                        p("file2.txt"),
                        p("folder1/file3.txt"),
                        p("folder1/file4.dat"),
                        p("folder1/sub/File5Test.class"),
                        p("folder2/File6Test.class"),
                        p("folder2/Helper.class"),
                        p(".hidden/file7.txt"));
    }

    @Test
    void emptyIncludesMeanEverything() {
        assertThat(scan(new String[0], null)).hasSize(8);
    }

    @Test
    void antPatternsAcrossDirectories() {
        assertThat(scan(new String[] {"**/*Test.class"}, null))
                .containsExactlyInAnyOrder(p("folder1/sub/File5Test.class"), p("folder2/File6Test.class"));
        assertThat(scan(new String[] {"*.txt"}, null)).containsExactlyInAnyOrder("file1.txt", "file2.txt");
        assertThat(scan(new String[] {"folder1/*"}, null))
                .containsExactlyInAnyOrder(p("folder1/file3.txt"), p("folder1/file4.dat"));
        assertThat(scan(new String[] {"folder?/*.class"}, null))
                .containsExactlyInAnyOrder(p("folder2/File6Test.class"), p("folder2/Helper.class"));
    }

    @Test
    void excludesWin() {
        assertThat(scan(new String[] {"**/*.class"}, new String[] {"**/Helper.class"}))
                .containsExactlyInAnyOrder(p("folder1/sub/File5Test.class"), p("folder2/File6Test.class"));
        assertThat(scan(null, new String[] {"folder1/**", ".hidden/**"}))
                .containsExactlyInAnyOrder(
                        "file1.txt", "file2.txt", p("folder2/File6Test.class"), p("folder2/Helper.class"));
    }

    @Test
    void trailingSeparatorMeansEverythingBelow() {
        assertThat(scan(new String[] {"folder1/"}, null))
                .containsExactlyInAnyOrder(
                        p("folder1/file3.txt"), p("folder1/file4.dat"), p("folder1/sub/File5Test.class"));
        assertThat(scan(new String[] {"folder1\\sub\\"}, null)).containsExactly(p("folder1/sub/File5Test.class"));
    }

    @Test
    void regexPatterns() {
        // setIncludes turns \\ into the separator even inside a regex, so the pattern avoids backslashes
        assertThat(scan(new String[] {"%regex[.*File[0-9]Test[.]class]"}, null))
                .containsExactlyInAnyOrder(p("folder1/sub/File5Test.class"), p("folder2/File6Test.class"));
    }

    @Test
    void matchingIsCaseSensitive() {
        assertThat(scan(new String[] {"**/*test.class"}, null)).isEmpty();
        assertThat(scan(new String[] {"FILE1.txt"}, null)).isEmpty();
    }

    @Test
    void nullElementsAreRejected() {
        DirectoryScanner scanner = new DirectoryScanner();
        assertThrows(NullPointerException.class, () -> scanner.setIncludes("a", null));
        assertThrows(NullPointerException.class, () -> scanner.setExcludes((String) null));
    }

    @Test
    void basedirMustBeAnExistingDirectory() {
        DirectoryScanner scanner = new DirectoryScanner();
        assertThrows(IllegalStateException.class, scanner::scan);
        scanner.setBasedir(basedir.resolve("missing").toFile());
        assertThrows(IllegalStateException.class, scanner::scan);
        scanner.setBasedir(basedir.resolve("file1.txt").toFile());
        assertThrows(IllegalStateException.class, scanner::scan);
    }

    @Test
    void noFilesBeforeScan() {
        assertThat(new DirectoryScanner().getIncludedFiles()).isEmpty();
    }
}
