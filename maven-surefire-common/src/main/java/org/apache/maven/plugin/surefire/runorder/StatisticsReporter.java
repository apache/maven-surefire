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
package org.apache.maven.plugin.surefire.runorder;

import java.io.File;
import java.io.IOException;

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.runorder.RunEntryStatisticsMap;

import static org.apache.maven.surefire.api.runorder.RunEntryStatisticsMap.fromFile;

/**
 * @author Kristian Rosenvold
 */
public class StatisticsReporter {
    private final RunEntryStatisticsMap existing;

    private final RunEntryStatisticsMap newResults;

    private final File dataFile;

    public StatisticsReporter(File dataFile) {
        this(dataFile, fromFile(dataFile), new RunEntryStatisticsMap());
    }

    protected StatisticsReporter(File dataFile, RunEntryStatisticsMap existing, RunEntryStatisticsMap newResults) {
        this.dataFile = dataFile;
        this.existing = existing;
        this.newResults = newResults;
    }

    public synchronized void testSetCompleted() {
        try {
            newResults.serialize(dataFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testSucceeded(ReportEntry report) {
        newResults.add(existing.createNextGeneration(report));
    }

    public void testSkipped(ReportEntry report) {
        newResults.add(existing.createNextGeneration(report));
    }

    public void testError(ReportEntry report) {
        newResults.add(existing.createNextGenerationFailure(report));
    }

    public void testFailed(ReportEntry report) {
        newResults.add(existing.createNextGenerationFailure(report));
    }
}
