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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            "TopTest.class",
            "notes.txt",
            "folder1/sub/File5Test.class",
            "folder2/File6Test.class",
            "folder2/Helper.class",
            "folder2/Helper$1.class",
            "folder2/data.properties"
        }) {
            Path p = tmp.resolve(f);
            Files.createDirectories(p.getParent());
            Files.write(p, new byte[0]);
        }
        return tmp.toFile();
    }

    private String[] collect(List<String> includes, List<String> excludes) throws IOException {
        return new DefaultDirectoryScanner(tree(), includes, excludes, null).collectTests();
    }

    private static String p(String slashPath) {
        return slashPath.replace('/', File.separatorChar);
    }

    @Test
    void collectTestsWithJavaPatterns() throws IOException {
        assertThat(collect(Arrays.asList("**/*Test.java"), null))
                .containsExactlyInAnyOrder("TopTest", "folder1.sub.File5Test", "folder2.File6Test");
    }

    @Test
    void collectTestsWithClassPatterns() throws IOException {
        assertThat(collect(Arrays.asList("**/*Test.class"), null))
                .containsExactlyInAnyOrder("TopTest", "folder1.sub.File5Test", "folder2.File6Test");
    }

    @Test
    void collectTestsWithExcludes() throws IOException {
        assertThat(collect(Arrays.asList("**/*.java"), Arrays.asList("**/*$*", "**/Helper.java")))
                .containsExactlyInAnyOrder("TopTest", "folder1.sub.File5Test", "folder2.File6Test");
    }

    @Test
    void collectTestsWithCommaSeparatedIncludes() throws IOException {
        assertThat(collect(Arrays.asList("**/File5*.java, **/Helper.java"), null))
                .containsExactlyInAnyOrder("folder1.sub.File5Test", "folder2.Helper");
    }

    @Test
    void collectTestsWithDirectoryPatterns() throws IOException {
        assertThat(collect(Arrays.asList("folder2/*Test.java"), null)).containsExactly("folder2.File6Test");
        assertThat(collect(Arrays.asList("folder1/**/*.java"), null)).containsExactly("folder1.sub.File5Test");
    }

    @Test
    void collectTestsWithRegex() throws IOException {
        assertThat(collect(Arrays.asList("%regex[.*File[0-9]Test.*]"), null))
                .containsExactlyInAnyOrder("folder1.sub.File5Test", "folder2.File6Test");
    }

    @Test
    void collectTestsIsCaseSensitive() throws IOException {
        assertThat(collect(Arrays.asList("**/*test.java"), null)).isEmpty();
    }

    @Test
    void collectTestsOfMissingDirectoryIsEmpty() {
        assertThat(new DefaultDirectoryScanner(new File(tmp.toFile(), "missing"), null, null, null).collectTests())
                .isEmpty();
    }
}
