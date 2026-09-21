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
package org.apache.maven.surefire.its.fixture;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.executor.ExecutorException;
import org.apache.maven.executor.ExecutorHelper;
import org.apache.maven.executor.ExecutorRequest;
import org.apache.maven.executor.ExecutorResult;
import org.apache.maven.executor.embedded.EmbeddedMavenExecutor;
import org.apache.maven.executor.forked.ForkedMavenExecutor;

/**
 * Minimal, purpose-built replacement for the deprecated {@code org.apache.maven.shared.verifier.Verifier}
 * (maven-verifier 2.0.0-M1, see apache/maven-verifier#186), implementing only the subset of that API used
 * by the surefire-its fixture, on top of {@code org.apache.maven.executor:maven-executor}.
 *
 * @see <a href="https://github.com/apache/maven-executor">maven-executor</a>
 */
public class Verifier {
    private static final String[] DEFAULT_CLI_ARGUMENTS = {"-e", "--batch-mode"};

    private static final String CLEAN_CLI_ARGUMENT = "org.apache.maven.plugins:maven-clean-plugin:clean";

    private static final Path MAVEN_HOME = Paths.get(System.getProperty("maven.home"));

    private static final EmbeddedMavenExecutor EMBEDDED_MAVEN_EXECUTOR = new EmbeddedMavenExecutor(MAVEN_HOME);

    private static final ForkedMavenExecutor FORKED_MAVEN_EXECUTOR = new ForkedMavenExecutor(MAVEN_HOME);

    /**
     * One embedded executor per JVM on purpose: a new EmbeddedMavenExecutor per execution builds a new Maven
     * ClassWorld that close() does not release (OOM after ~100 builds); the cost is that system properties set
     * between executions are reset, see apache/maven-executor#46.
     */
    private static final ExecutorHelper EXECUTOR_HELPER =
            ExecutorHelper.forExecutors(ExecutorHelper.Mode.AUTO, EMBEDDED_MAVEN_EXECUTOR, FORKED_MAVEN_EXECUTOR);

    private final String basedir;

    private final String[] defaultCliArguments;

    private final String localRepo;

    private final List<String> cliArguments = new ArrayList<>();

    private Properties systemProperties = new Properties();

    private Map<String, String> environmentVariables = new HashMap<>();

    private boolean autoclean = true;

    private Boolean forkJvm;

    private String logFileName = "log.txt";

    public Verifier(String basedir) throws VerificationException {
        this(basedir, DEFAULT_CLI_ARGUMENTS);
    }

    public Verifier(String basedir, String[] defaultCliArguments) throws VerificationException {
        this.basedir = basedir;
        this.defaultCliArguments = defaultCliArguments == null ? new String[0] : defaultCliArguments.clone();
        this.localRepo = findLocalRepo();
    }

    /**
     * Resolves the local repository the way the (deprecated) Verifier did: an explicit
     * {@code -Dmaven.repo.local} Java System property first, falling back to {@code ~/.m2/repository}. Unlike
     * the old Verifier, this does not additionally parse a {@code settings.xml} for a
     * {@code <localRepository>} element as a fallback: that parsing was never applied to the actual build
     * (the resolved value only fed {@link #getLocalRepository()}/{@link #getArtifactPath}), so dropping it
     * changes no build behavior (see apache/maven-verifier#142).
     */
    private static String findLocalRepo() {
        String repo = System.getProperty("maven.repo.local");
        if (repo == null) {
            repo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }
        File repoDir = new File(repo);
        if (!repoDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            repoDir.mkdirs();
        }
        return repoDir.getAbsolutePath();
    }

    public String getBasedir() {
        return basedir;
    }

    public String getLocalRepository() {
        return localRepo;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        if (logFileName == null || logFileName.isEmpty()) {
            throw new IllegalArgumentException("log file name unspecified");
        }
        this.logFileName = logFileName;
    }

    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    public void setForkJvm(boolean forkJvm) {
        this.forkJvm = forkJvm;
    }

    public void addCliArgument(String cliArgument) {
        cliArguments.add(cliArgument);
    }

    public void addCliArguments(String... cliArguments) {
        Collections.addAll(this.cliArguments, cliArguments);
    }

    public void setSystemProperties(Properties systemProperties) {
        this.systemProperties = systemProperties == null ? new Properties() : systemProperties;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables == null ? new HashMap<>() : environmentVariables;
    }

    public String getExecutable() {
        return MAVEN_HOME.resolve("bin").resolve("mvn").toString();
    }

    /**
     * Executes Maven with the accumulated CLI arguments, system properties and environment variables. Both
     * stdout and stderr are piped into the same {@code <basedir>/<logFileName>} file, in arrival order, so
     * that {@link #loadFile} and the {@code OutputValidator} log-reading helpers can read it back from disk
     * exactly like the old Verifier did. Runs through the single JVM-lifetime {@link #EXECUTOR_HELPER}; see
     * its Javadoc for why.
     */
    public void execute() throws VerificationException {
        List<String> args = new ArrayList<>();
        Collections.addAll(args, defaultCliArguments);
        args.add("-Dmaven.repo.local=" + localRepo);
        for (String name : systemProperties.stringPropertyNames()) {
            args.add("-D" + name + "=" + systemProperties.getProperty(name));
        }
        if (autoclean) {
            args.add(CLEAN_CLI_ARGUMENT);
        }
        for (String cliArgument : cliArguments) {
            args.add(cliArgument.replace("${basedir}", basedir));
        }

        ExecutorHelper.Mode mode = forkJvm == null
                ? ExecutorHelper.Mode.AUTO
                : (forkJvm ? ExecutorHelper.Mode.FORKED : ExecutorHelper.Mode.EMBEDDED);

        File logFile = new File(basedir, logFileName);
        ExecutorResult result;
        // shared by both stdOut and stdErr below: the pump threads each close whatever stream they are given,
        // and may write concurrently, so this sink must tolerate a double close and serialize writes.
        try (IdempotentOutputStream sink = new IdempotentOutputStream(Files.newOutputStream(logFile.toPath()))) {
            ExecutorRequest.Builder builder = ExecutorRequest.mavenBuilder()
                    .cwd(Paths.get(basedir))
                    .arguments(args)
                    .stdOut(sink)
                    .stdErr(sink);
            if (!environmentVariables.isEmpty()) {
                builder.environmentVariables(environmentVariables);
            }
            ExecutorRequest request = builder.build();

            try {
                result = EXECUTOR_HELPER.execute(mode, request);
            } catch (ExecutorException e) {
                throw new VerificationException("Failed to execute Maven", e);
            }
        } catch (IOException e) {
            throw new VerificationException("Could not write log file: " + logFile, e);
        }

        if (!result.success()) {
            throw new VerificationException("Exit code was non-zero: "
                    + result.exitCode().orElse(-1) + "; command line and log = \n" + getExecutable() + " "
                    + join(args) + "\n" + getLogContents());
        }
    }

    /**
     * Wraps a single {@link OutputStream} so it can be handed to both the stdout and stderr sinks of an
     * {@link ExecutorRequest} and still be closed safely by either (or both) pump threads.
     */
    private static final class IdempotentOutputStream extends OutputStream {
        private final OutputStream delegate;
        private boolean closed;

        IdempotentOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
        public synchronized void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public synchronized void close() throws IOException {
            if (!closed) {
                closed = true;
                delegate.close();
            }
        }
    }

    private static String join(List<String> args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(arg);
        }
        return sb.toString();
    }

    private String getLogContents() {
        try {
            return new String(Files.readAllBytes(new File(basedir, logFileName).toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "(Error reading log contents: " + e.getMessage() + ")";
        }
    }

    public void verifyErrorFreeLog() throws VerificationException {
        for (String line : loadFile(basedir, logFileName, false)) {
            if (stripAnsi(line).contains("[ERROR]") && !isVelocityError(line)) {
                throw new VerificationException("Error in execution: " + line);
            }
        }
    }

    public void verifyTextInLog(String text) throws VerificationException {
        for (String line : loadFile(basedir, logFileName, false)) {
            if (stripAnsi(line).contains(text)) {
                return;
            }
        }
        throw new VerificationException("Text not found in log: " + text);
    }

    public void verifyFileNotPresent(String file) throws VerificationException {
        File expected = new File(file);
        if (!expected.isAbsolute()) {
            expected = new File(basedir, file);
        }
        if (expected.exists()) {
            throw new VerificationException("Unwanted file was found: " + expected);
        }
    }

    public String getArtifactPath(String gid, String aid, String version, String ext) {
        return getArtifactPath(gid, aid, version, ext, null);
    }

    public String getArtifactPath(String gid, String aid, String version, String ext, String classifier) {
        if (classifier != null && classifier.isEmpty()) {
            classifier = null;
        }
        if ("maven-plugin".equals(ext)) {
            ext = "jar";
        } else if ("coreit-artifact".equals(ext)) {
            ext = "jar";
            classifier = "it";
        } else if ("test-jar".equals(ext)) {
            ext = "jar";
            classifier = "tests";
        }

        String repositoryPath = gid.replace('.', '/') + "/" + aid + "/" + version + "/" + aid + "-" + version;
        if (classifier != null) {
            repositoryPath += "-" + classifier;
        }
        repositoryPath += "." + ext;

        return new File(localRepo, repositoryPath.replace('/', File.separatorChar)).getPath();
    }

    /**
     * Loads the (trimmed, non-empty, non-comment) lines of the given file, relative to {@code basedir}.
     * The third parameter is retained only for source compatibility with call sites ported from the old
     * Verifier API (which used it to enable {@code ${artifact:g:a:v:ext}} substitution); it is unused here as
     * none of the surefire-its callers pass {@code true}.
     */
    public List<String> loadFile(String basedir, String filename, boolean hasCommand) throws VerificationException {
        List<String> lines = new ArrayList<>();
        File file = new File(basedir, filename);
        if (file.exists()) {
            try {
                for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                        lines.add(trimmed);
                    }
                }
            } catch (IOException e) {
                throw new VerificationException(e);
            }
        }
        return lines;
    }

    /**
     * Checks whether the specified line is just an error message from Velocity. Especially old versions of
     * Doxia employ a very noisy Velocity instance.
     */
    private static boolean isVelocityError(String line) {
        return line.contains("VM_global_library.vm") || (line.contains("VM #") && line.contains("macro"));
    }

    public static String stripAnsi(String msg) {
        return msg.replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "");
    }
}
