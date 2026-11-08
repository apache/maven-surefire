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
package org.apache.maven.surefire.api.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.surefire.api.util.DefaultDirectoryScanner.scan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test of the directory scanner.
 */
public class DefaultDirectoryScannerTest {
    @Test
    public void testLocateTestClasses() throws IOException, TestSetFailedException {
        // use target as people can configure ide to compile in an other place than maven
        File baseDir = new File(new File("target").getCanonicalPath());
        List<String> include = new ArrayList<>();
        include.add("**/*ZT*A.java");
        List<String> exclude = new ArrayList<>();

        DefaultDirectoryScanner surefireDirectoryScanner =
                new DefaultDirectoryScanner(baseDir, include, exclude, new ArrayList<String>());

        String[] classNames = surefireDirectoryScanner.collectTests();
        assertNotNull(classNames);
        System.out.println("classNames " + Arrays.asList(classNames));
        assertEquals(4, classNames.length);
    }

    @TempDir
    Path tmp;

    private File tree() throws IOException {
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
            Path p = tmp.resolve(f);
            Files.createDirectories(p.getParent());
            Files.write(p, new byte[0]);
        }
        return tmp.toFile();
    }

    private static String p(String slashPath) {
        return slashPath.replace('/', File.separatorChar);
    }

    @Test
    void scanIncludesEverythingByDefault() throws IOException {
        File base = tree();
        assertThat(scan(base, null, null))
                .hasSize(8)
                .contains(p("folder1/sub/File5Test.class"), p(".hidden/file7.txt"));
        assertThat(scan(base, new String[0], null)).hasSize(8);
    }

    @Test
    void scanAntPatternsAcrossDirectories() throws IOException {
        File base = tree();
        assertThat(scan(base, new String[] {"**/*Test.class"}, null))
                .containsExactlyInAnyOrder(p("folder1/sub/File5Test.class"), p("folder2/File6Test.class"));
        assertThat(scan(base, new String[] {"*.txt"}, null)).containsExactlyInAnyOrder("file1.txt", "file2.txt");
        assertThat(scan(base, new String[] {"folder1/*"}, null))
                .containsExactlyInAnyOrder(p("folder1/file3.txt"), p("folder1/file4.dat"));
        assertThat(scan(base, new String[] {"folder?/*.class"}, null))
                .containsExactlyInAnyOrder(p("folder2/File6Test.class"), p("folder2/Helper.class"));
    }

    @Test
    void scanExcludesWin() throws IOException {
        File base = tree();
        assertThat(scan(base, new String[] {"**/*.class"}, new String[] {"**/Helper.class"}))
                .containsExactlyInAnyOrder(p("folder1/sub/File5Test.class"), p("folder2/File6Test.class"));
        assertThat(scan(base, null, new String[] {"folder1/**", ".hidden/**"}))
                .containsExactlyInAnyOrder(
                        "file1.txt", "file2.txt", p("folder2/File6Test.class"), p("folder2/Helper.class"));
    }

    @Test
    void scanTrailingSeparatorMeansEverythingBelow() throws IOException {
        File base = tree();
        assertThat(scan(base, new String[] {"folder1/"}, null))
                .containsExactlyInAnyOrder(
                        p("folder1/file3.txt"), p("folder1/file4.dat"), p("folder1/sub/File5Test.class"));
        assertThat(scan(base, new String[] {"folder1\\sub\\"}, null)).containsExactly(p("folder1/sub/File5Test.class"));
    }

    @Test
    void scanRegexPatterns() throws IOException {
        // both slashes become the separator, inside a regex too, so the pattern avoids backslashes
        assertThat(scan(tree(), new String[] {"%regex[.*File[0-9]Test[.]class]"}, null))
                .containsExactlyInAnyOrder(p("folder1/sub/File5Test.class"), p("folder2/File6Test.class"));
    }

    @Test
    void scanIsCaseSensitive() throws IOException {
        File base = tree();
        assertThat(scan(base, new String[] {"**/*test.class"}, null)).isEmpty();
        assertThat(scan(base, new String[] {"FILE1.txt"}, null)).isEmpty();
    }

    @Test
    void scanRejectsNullPatternsAndBadBasedir() throws IOException {
        File base = tree();
        assertThrows(NullPointerException.class, () -> scan(base, new String[] {"a", null}, null));
        assertThrows(NullPointerException.class, () -> scan(base, null, new String[] {null}));
        assertThrows(IllegalStateException.class, () -> scan(new File(base, "missing"), null, null));
        assertThrows(IllegalStateException.class, () -> scan(new File(base, "file1.txt"), null, null));
    }

    @Test
    void collectTestsTurnsClassFilesIntoClassNames() throws IOException {
        File base = tree();
        DefaultDirectoryScanner scanner = new DefaultDirectoryScanner(
                base, Arrays.asList("**/*Test.java,**/Helper.class"), Arrays.asList("**/File6*"), null);
        assertThat(scanner.collectTests()).containsExactlyInAnyOrder("folder1.sub.File5Test", "folder2.Helper");
    }
}
