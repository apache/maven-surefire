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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

/**
 * The runtime handoff file {@code META-INF/maven/module-info-patch.args} written by
 * maven-compiler-plugin 4.x from {@code module-info-patch.maven}. It contains the Java
 * Modules options the compiler used for the test compilation (one option per line, value
 * either on the same line separated by whitespace or on the following line).
 *
 * @since 3.6.0
 */
public final class ModuleInfoPatchArgsFile {
    public static final String RELATIVE_PATH = "META-INF/maven/module-info-patch.args";

    private final File file;
    private final List<String> lines;
    private final Set<String> addedModules;

    private ModuleInfoPatchArgsFile(File file, List<String> lines, Set<String> addedModules) {
        this.file = file;
        this.lines = lines;
        this.addedModules = addedModules;
    }

    /**
     * Loads the handoff file from the test output directory.
     *
     * @param testClassesDirectory the test output directory (e.g. target/test-classes)
     * @return the parsed file, or null if it does not exist
     * @throws IOException if the file exists but cannot be read
     */
    public static ModuleInfoPatchArgsFile load(File testClassesDirectory) throws IOException {
        if (testClassesDirectory == null || !testClassesDirectory.isDirectory()) {
            return null;
        }
        return parse(new File(testClassesDirectory, RELATIVE_PATH));
    }

    /**
     * Parses the given handoff file.
     *
     * @param file the module-info-patch.args file
     * @return the parsed file, or null if it does not exist
     * @throws IOException if the file exists but cannot be read
     */
    public static ModuleInfoPatchArgsFile parse(File file) throws IOException {
        if (file == null || !file.isFile()) {
            return null;
        }

        List<String> lines = new ArrayList<>();
        Set<String> addedModules = new LinkedHashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                lines.add(line);
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String value = optionValue(line, "--add-modules", lines, i);
            if (value != null) {
                for (String module : value.split(",")) {
                    String name = module.trim();
                    if (!name.isEmpty()) {
                        addedModules.add(name);
                    }
                }
            }
        }
        return new ModuleInfoPatchArgsFile(file, unmodifiableList(lines), unmodifiableSet(addedModules));
    }

    /**
     * The value of the given option at index {@code i}, supporting both the same-line form
     * ({@code --add-modules a,b}) and the two-line argfile form (value on the next line).
     *
     * @return the option value, or null if the line is not the given option
     */
    private static String optionValue(String line, String option, List<String> lines, int i) {
        if (line.equals(option)) {
            return i + 1 < lines.size() ? lines.get(i + 1) : null;
        }
        if (line.startsWith(option) && Character.isWhitespace(line.charAt(option.length()))) {
            return line.substring(option.length()).trim();
        }
        return null;
    }

    public File getFile() {
        return file;
    }

    /**
     * @return all non-empty, non-comment lines of the file, in order
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Module names listed by the file's {@code --add-modules} directives — with
     * {@code module-info-patch.maven}'s {@code add-modules TEST-MODULE-PATH} these are the
     * test-scope dependencies that belong on the module path.
     *
     * @return the module names, in file order
     */
    public Set<String> getAddedModules() {
        return addedModules;
    }
}
