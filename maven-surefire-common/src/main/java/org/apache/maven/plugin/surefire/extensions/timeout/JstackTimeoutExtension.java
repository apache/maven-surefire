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
package org.apache.maven.plugin.surefire.extensions.timeout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutContext;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension;
import org.apache.maven.surefire.shared.lang3.SystemUtils;

/**
 * Built-in {@link ForkedProcessTimeoutExtension} that captures a {@code jstack}
 * thread dump of the forked test JVM just before it is killed because of
 * {@code forkedProcessTimeoutInSeconds}.
 * <p>
 * The output is written to
 * {@code <reportsDirectory>/surefire-timeout-jstack-<forkNumber>-<pid>.txt}.
 * <p>
 * <strong>Activation:</strong> this extension is registered via
 * {@code META-INF/services} but is <em>disabled by default</em>. To enable it
 * set the system property {@code surefire.timeout.jstack.enabled=true} on the
 * Maven process (for example with {@code MAVEN_OPTS} or
 * {@code -Dsurefire.timeout.jstack.enabled=true}). The {@code jstack} binary
 * is resolved from {@code ${java.home}/bin/jstack}, the parent JDK {@code bin}
 * (Java 8 layout), {@code $JAVA_HOME/bin/jstack}, and finally from {@code PATH}.
 *
 * @since 3.6.0
 */
public class JstackTimeoutExtension implements ForkedProcessTimeoutExtension {

    /** System property that enables this extension. */
    public static final String ENABLED_PROPERTY = "surefire.timeout.jstack.enabled";

    /**
     * Extension-context key that enables this extension from the POM via the
     * {@code forkedProcessTimeoutExtensionContext} Mojo parameter. Set to
     * {@code true} to enable. When either this key or {@link #ENABLED_PROPERTY}
     * is set to {@code true}, the extension runs.
     */
    public static final String ENABLED_KEY = "jstack.enabled";

    /**
     * Extension context key that overrides the directory where the
     * {@code surefire-timeout-jstack-*.txt} files are written. When the key is
     * missing or blank, the Surefire reports directory is used.
     */
    public static final String OUTPUT_LOCATION_KEY = "jstack.output.location";

    /** Wall-clock timeout for the jstack subprocess. */
    static final int JSTACK_TIMEOUT_SECONDS = 20;

    @Override
    public void onTimeoutDetected(ForkedProcessTimeoutContext context) {
        ConsoleLogger logger = context.getConsoleLogger();
        if (!isEnabled(context)) {
            logger.debug("JstackTimeoutExtension disabled (set -D" + ENABLED_PROPERTY + "=true or POM key "
                    + ENABLED_KEY + "=true to enable)");
            return;
        }
        long pid = context.getPid();
        if (pid <= 0L) {
            logger.warning("JstackTimeoutExtension: PID of forked JVM unknown (Java 8 or unsupported platform); "
                    + "skipping jstack for fork " + context.getForkNumber());
            return;
        }
        File jstack = resolveJstackBinary();
        if (jstack == null) {
            logger.warning("JstackTimeoutExtension: cannot find jstack in java.home, JAVA_HOME or PATH; "
                    + "skipping jstack for fork " + context.getForkNumber() + " (pid=" + pid + ")");
            return;
        }
        File outputDirectory = resolveOutputDirectory(context, logger);
        if (outputDirectory == null) {
            return;
        }
        File output =
                new File(outputDirectory, "surefire-timeout-jstack-" + context.getForkNumber() + "-" + pid + ".txt");
        runJstack(jstack, pid, output, logger, context.getForkNumber());
    }

    private static boolean isEnabled(ForkedProcessTimeoutContext context) {
        if (Boolean.getBoolean(ENABLED_PROPERTY)) {
            return true;
        }
        String fromCtx = context.getExtensionContext().get(ENABLED_KEY);
        return fromCtx != null && Boolean.parseBoolean(fromCtx.trim());
    }

    private static File resolveOutputDirectory(ForkedProcessTimeoutContext context, ConsoleLogger logger) {
        String configured = context.getExtensionContext().get(OUTPUT_LOCATION_KEY);
        File target;
        if (configured != null && !configured.trim().isEmpty()) {
            target = new File(configured.trim());
        } else {
            target = context.getReportsDirectory();
        }
        if (!target.isDirectory() && !target.mkdirs()) {
            logger.warning("JstackTimeoutExtension: cannot create output directory " + target.getAbsolutePath());
            return null;
        }
        return target;
    }

    @Override
    public void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) {
        // No-op: the diagnostic is captured before the kill.
    }

    private void runJstack(File jstack, long pid, File output, ConsoleLogger logger, int forkNumber) {
        ProcessBuilder builder = new ProcessBuilder(jstack.getAbsolutePath(), Long.toString(pid))
                .redirectErrorStream(true)
                .redirectOutput(output);
        Process process = null;
        try {
            process = builder.start();
            if (!process.waitFor(JSTACK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                logger.warning("JstackTimeoutExtension: jstack for fork " + forkNumber + " (pid=" + pid
                        + ") did not complete within " + JSTACK_TIMEOUT_SECONDS + "s");
                return;
            }
            int exit = process.exitValue();
            if (exit != 0) {
                logger.warning("JstackTimeoutExtension: jstack exited with code " + exit + " for fork " + forkNumber
                        + " (pid=" + pid + "); see " + output.getAbsolutePath());
            } else {
                logger.info("JstackTimeoutExtension: wrote thread dump for fork " + forkNumber + " (pid=" + pid
                        + ") to " + output.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.warning("JstackTimeoutExtension: failed to run jstack for fork " + forkNumber + ": " + e);
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
        }
    }

    static File resolveJstackBinary() {
        String exe = SystemUtils.IS_OS_WINDOWS ? "jstack.exe" : "jstack";
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            // java.home may point to JRE (Java 8) or JDK (Java 9+); jstack lives in <jdk>/bin
            File candidate = new File(javaHome, "bin/" + exe);
            if (isExecutable(candidate)) {
                return candidate;
            }
            // Java 8: <jdk>/jre/bin/java -> jstack at <jdk>/bin/jstack
            File parent = new File(javaHome).getParentFile();
            if (parent != null) {
                File candidate2 = new File(parent, "bin/" + exe);
                if (isExecutable(candidate2)) {
                    return candidate2;
                }
            }
        }
        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null) {
            File candidate = new File(envJavaHome, "bin/" + exe);
            if (isExecutable(candidate)) {
                return candidate;
            }
        }
        // Fall back to PATH lookup
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                File candidate = new File(dir, exe);
                if (isExecutable(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean isExecutable(File f) {
        return f != null && f.isFile() && Files.isExecutable(f.toPath());
    }
}
