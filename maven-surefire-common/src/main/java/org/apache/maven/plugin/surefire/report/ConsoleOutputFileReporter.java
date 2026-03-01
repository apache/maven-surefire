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
package org.apache.maven.plugin.surefire.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

import static org.apache.maven.plugin.surefire.report.FileReporter.getReportFile;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;

/**
 * Surefire output consumer proxy that writes test output to a {@link java.io.File} for each test suite.
 *
 * @author Kristian Rosenvold
 * @author Carlos Sanchez
 */
public class ConsoleOutputFileReporter implements TestcycleConsoleOutputReceiver {
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;

    private final File reportsDirectory;
    private final String reportNameSuffix;
    private final boolean usePhrasedFileName;
    private final Integer forkNumber;
    private final String encoding;

    private final Map<String, FilterOutputStream> outputStreams = new ConcurrentHashMap<>();

    private volatile String reportEntryName;

    public ConsoleOutputFileReporter(
            File reportsDirectory,
            String reportNameSuffix,
            boolean usePhrasedFileName,
            Integer forkNumber,
            String encoding) {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
        this.usePhrasedFileName = usePhrasedFileName;
        this.forkNumber = forkNumber;
        this.encoding = encoding;
    }

    @Override
    public void testSetStarting(TestSetReportEntry reportEntry) {
        String className = usePhrasedFileName ? reportEntry.getSourceText() : reportEntry.getSourceName();
        try {
            File file = getReportFile(reportsDirectory, className, reportNameSuffix, "-output.txt");
            if (!reportsDirectory.exists()) {
                Files.createDirectories(reportsDirectory.toPath());
            }
            if (!Files.exists(file.toPath())) {
                Files.createFile(file.toPath());
            }
            outputStreams.put(
                    className, new BufferedOutputStream(Files.newOutputStream(file.toPath()), STREAM_BUFFER_SIZE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void testSetCompleted(TestSetReportEntry report) {}

    @Override
    public void close() {
        // Close all output streams in the map
        for (FilterOutputStream stream : outputStreams.values()) {
            try {
                stream.close();
            } catch (IOException e) {
                dumpException(e);
            }
        }
    }

    @Override
    public synchronized void writeTestOutput(TestOutputReportEntry reportEntry) {
        try {
            // Determine the target class name based on stack trace or reportEntryName
            String targetClassName = extractTestClassFromStack(reportEntry.getStack());
            if (targetClassName == null) {
                targetClassName = reportEntryName;
            }
            // If still null, use "null" as the file name (for output before any test starts)
            if (targetClassName == null) {
                targetClassName = "null";
            }

            // Get or create output stream for this test class
            FilterOutputStream os = outputStreams.computeIfAbsent(targetClassName, className -> {
                try {
                    if (!reportsDirectory.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        reportsDirectory.mkdirs();
                    }
                    File file = getReportFile(reportsDirectory, className, reportNameSuffix, "-output.txt");
                    return new BufferedOutputStream(Files.newOutputStream(file.toPath()), STREAM_BUFFER_SIZE);
                } catch (IOException e) {
                    dumpException(e);
                    throw new UncheckedIOException(e);
                }
            });

            String output = reportEntry.getLog();
            if (output == null) {
                output = "null";
            }
            Charset charset = Charset.forName(encoding);
            os.write(output.getBytes(charset));
            if (reportEntry.isNewLine()) {
                os.write(NL.getBytes(charset));
            }
        } catch (IOException e) {
            dumpException(e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Extracts the test class name from the stack trace.
     * Stack format: className#method;className#method;...
     * Returns the first class name that looks like a test class.
     */
    private String extractTestClassFromStack(String stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        // The stack contains entries like "className#method;className#method;..."
        // We look for the test class which typically is the first entry or an entry with "Test" in the name
        String[] entries = stack.split(";");
        for (String entry : entries) {
            int hashIndex = entry.indexOf('#');
            if (hashIndex > 0) {
                String className = entry.substring(0, hashIndex);
                if (outputStreams.containsKey(className)) {
                    return className;
                }
            }
        }
        return null;
    }

    private void dumpException(IOException e) {
        if (forkNumber == null) {
            InPluginProcessDumpSingleton.getSingleton().dumpException(e, e.getLocalizedMessage(), reportsDirectory);
        } else {
            InPluginProcessDumpSingleton.getSingleton()
                    .dumpException(e, e.getLocalizedMessage(), reportsDirectory, forkNumber);
        }
    }
}
