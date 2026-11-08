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
package org.apache.maven.surefire.extensions.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The command line of a process Surefire starts: an executable, its arguments, the environment and the working
 * directory, started with {@link ProcessBuilder}. The process is a direct child; there is no shell in between.
 */
public class Commandline {
    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows");

    private String executable;
    private final List<String> arguments = new ArrayList<>();
    private final Map<String, String> addedEnvironment = new LinkedHashMap<>();
    private File workingDirectory;

    public Commandline() {}

    /**
     * @param commandLine the executable followed by its arguments, split like a shell would, see
     *                    {@link #translateCommandline(String)}
     */
    public Commandline(String commandLine) {
        String[] parts = translateCommandline(commandLine);
        if (parts.length > 0) {
            executable = parts[0];
            arguments.addAll(java.util.Arrays.asList(parts).subList(1, parts.length));
        }
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getExecutable() {
        return executable;
    }

    public Argument createArg() {
        return new Argument();
    }

    public void addArgument(String argument) {
        arguments.add(argument);
    }

    public void addArguments(String... args) {
        Collections.addAll(arguments, args);
    }

    /**
     * @return the arguments, without the executable
     */
    public String[] getArguments() {
        return arguments.toArray(new String[0]);
    }

    /**
     * @return the executable followed by the arguments
     */
    public String[] getCommandline() {
        List<String> all = new ArrayList<>(arguments.size() + 1);
        all.add(executable);
        all.addAll(arguments);
        return all.toArray(new String[0]);
    }

    public void setWorkingDirectory(String path) {
        this.workingDirectory = path == null ? null : new File(path);
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void addEnvironment(String name, String value) {
        addedEnvironment.put(name, value);
    }

    /**
     * The environment the process gets: the environment of this JVM, with {@link #addEnvironment added} variables
     * overriding it. Subclasses may remove variables.
     *
     * @return a mutable copy
     */
    protected Map<String, String> environment() {
        Map<String, String> env = new LinkedHashMap<>(System.getenv());
        env.putAll(addedEnvironment);
        return env;
    }

    /**
     * @return the environment of the process as {@code NAME=value} entries
     */
    public String[] getEnvironmentVariables() {
        Map<String, String> env = environment();
        String[] result = new String[env.size()];
        int i = 0;
        for (Map.Entry<String, String> e : env.entrySet()) {
            result[i++] = e.getKey() + "=" + e.getValue();
        }
        return result;
    }

    public ProcessBuilder toProcessBuilder() {
        ProcessBuilder builder = new ProcessBuilder(getCommandline());
        builder.directory(workingDirectory);
        Map<String, String> env = builder.environment();
        env.clear();
        env.putAll(environment());
        return builder;
    }

    public Process execute() throws IOException {
        return toProcessBuilder().start();
    }

    /**
     * The command line as text, each item quoted: single quotes on Unix, double quotes on Windows.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String item : getCommandline()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (WINDOWS) {
                sb.append('"')
                        .append(item == null ? "" : item.replace("\"", "\\\""))
                        .append('"');
            } else {
                sb.append('\'')
                        .append(item == null ? "" : item.replace("'", "'\"'\"'"))
                        .append('\'');
            }
        }
        return sb.toString();
    }

    /**
     * Splits a command line into arguments like a shell would: whitespace separates, single or double quotes
     * group, a backslash escapes the next character.
     *
     * @throws IllegalArgumentException on unbalanced quotes
     */
    public static String[] translateCommandline(String toProcess) {
        if (toProcess == null || toProcess.isEmpty()) {
            return new String[0];
        }

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        boolean inEscape = false;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"' \\", true);
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
                case inQuote:
                    if ("'".equals(nextTok)) {
                        if (inEscape) {
                            current.append(nextTok);
                            inEscape = false;
                        } else {
                            state = normal;
                        }
                    } else {
                        current.append(nextTok);
                        inEscape = "\\".equals(nextTok);
                    }
                    break;
                case inDoubleQuote:
                    if ("\"".equals(nextTok)) {
                        if (inEscape) {
                            current.append(nextTok);
                            inEscape = false;
                        } else {
                            state = normal;
                        }
                    } else {
                        current.append(nextTok);
                        inEscape = "\\".equals(nextTok);
                    }
                    break;
                default:
                    if ("'".equals(nextTok)) {
                        if (inEscape) {
                            inEscape = false;
                            current.append(nextTok);
                        } else {
                            state = inQuote;
                        }
                    } else if ("\"".equals(nextTok)) {
                        if (inEscape) {
                            inEscape = false;
                            current.append(nextTok);
                        } else {
                            state = inDoubleQuote;
                        }
                    } else if (" ".equals(nextTok)) {
                        if (current.length() != 0) {
                            tokens.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(nextTok);
                        inEscape = "\\".equals(nextTok);
                    }
                    break;
            }
        }

        if (current.length() != 0) {
            tokens.add(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new IllegalArgumentException("unbalanced quotes in " + toProcess);
        }

        return tokens.toArray(new String[0]);
    }

    /**
     * One argument being added, as {@code createArg().setValue(..)} or {@code setLine(..)}.
     */
    public final class Argument {
        private Argument() {}

        public void setValue(String value) {
            arguments.add(value);
        }

        public void setFile(File file) {
            arguments.add(file.getAbsolutePath());
        }

        /**
         * @param line a line split into arguments like a shell would
         * @throws IllegalArgumentException on unbalanced quotes
         */
        public void setLine(String line) {
            if (line != null) {
                Collections.addAll(arguments, translateCommandline(line));
            }
        }
    }
}
