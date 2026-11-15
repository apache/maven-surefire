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

/**
 * What {@code runLastFailed} should run.
 */
public final class LastFailedTests {
    /**
     * The possible outcomes.
     */
    public enum Selection {
        /** We don't know what failed (e.g. no reports yet), run the usual tests. */
        RUN_ALL,
        /** Everything passed last time, nothing to run. */
        NOTHING,
        /** Run only the tests in {@link #getTests()}. */
        SOME
    }

    private static final LastFailedTests RUN_ALL = new LastFailedTests(Selection.RUN_ALL, null, 0, 0, 0);

    private static final LastFailedTests NOTHING = new LastFailedTests(Selection.NOTHING, null, 0, 0, 0);

    private final Selection selection;

    private final String tests;

    private final int failedMethods;

    private final int failedClasses;

    private final int unfinishedClasses;

    private LastFailedTests(
            Selection selection, String tests, int failedMethods, int failedClasses, int unfinishedClasses) {
        this.selection = selection;
        this.tests = tests;
        this.failedMethods = failedMethods;
        this.failedClasses = failedClasses;
        this.unfinishedClasses = unfinishedClasses;
    }

    public static LastFailedTests runAll() {
        return RUN_ALL;
    }

    static LastFailedTests nothing() {
        return NOTHING;
    }

    static LastFailedTests some(String tests, int failedMethods, int failedClasses, int unfinishedClasses) {
        return new LastFailedTests(Selection.SOME, tests, failedMethods, failedClasses, unfinishedClasses);
    }

    public Selection getSelection() {
        return selection;
    }

    /**
     * @return the tests to run, written like the {@code test} parameter, only set for {@link Selection#SOME}
     */
    public String getTests() {
        return tests;
    }

    /**
     * @return the number of failed test methods to run
     */
    public int getFailedMethods() {
        return failedMethods;
    }

    /**
     * @return the number of classes that failed outside a test, e.g. in {@code @BeforeAll}
     */
    public int getFailedClasses() {
        return failedClasses;
    }

    /**
     * @return the number of classes that didn't finish because the last run stopped early
     */
    public int getUnfinishedClasses() {
        return unfinishedClasses;
    }
}
