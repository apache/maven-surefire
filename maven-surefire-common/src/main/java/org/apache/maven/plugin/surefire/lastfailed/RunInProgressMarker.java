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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A small file that exists while tests run, and is deleted when the run ends.
 * If it is still there next time, the last run didn't finish: a fork crashed, timed out, or Maven was killed.
 * <br>
 * It lists the test classes of that run, and its date is when the run started.
 * It sits next to the reports directory, e.g. {@code target/.surefire-reports-run-in-progress},
 * so we never create or change the reports directory itself.
 */
public final class RunInProgressMarker {
    private final File file;

    private RunInProgressMarker(File file) {
        this.file = file;
    }

    /**
     * @param reportsDirectory where the XML reports are written
     * @return the marker for this reports directory
     */
    public static RunInProgressMarker of(File reportsDirectory) {
        File directory = reportsDirectory.getAbsoluteFile();
        return new RunInProgressMarker(
                new File(directory.getParentFile(), "." + directory.getName() + "-run-in-progress"));
    }

    public File getFile() {
        return file;
    }

    public boolean exists() {
        return file.isFile();
    }

    /**
     * Writes the test classes about to run, replacing any old marker.
     */
    public void write(Collection<String> testClasses) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        Files.write(file.toPath(), testClasses, UTF_8);
    }

    public void delete() throws IOException {
        Files.deleteIfExists(file.toPath());
    }

    /**
     * @return when the run started
     */
    long getStartTime() {
        return file.lastModified();
    }

    /**
     * @return the test classes of the run
     */
    Set<String> readTestClasses() throws IOException {
        Set<String> classes = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file.toPath(), UTF_8)) {
            String className = line.trim();
            if (!className.isEmpty()) {
                classes.add(className);
            }
        }
        return classes;
    }
}
