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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.shared.verifier.VerificationException;

/**
 * Encapsulate all needed features to start a surefire run
 * <br>
 * Also includes thread-safe access to the extracted resource
 * files
 *
 * @author Kristian Rosenvold                                 -
 */
public final class SurefireLauncher {
    private final MavenLauncher mavenLauncher;

    private final String surefireVersion = System.getProperty("surefire.version");

    public SurefireLauncher(MavenLauncher mavenLauncher) {
        this.mavenLauncher = mavenLauncher;
        reset();
    }

    public MavenLauncher maven() {
        return mavenLauncher;
    }

    String getTestMethodName() {
        return mavenLauncher.getTestMethodName();
    }

    public void reset() {
        mavenLauncher.reset();
        for (String s : getInitialGoals()) {
            mavenLauncher.addGoal(s);
        }
    }

    public SurefireLauncher getSubProjectLauncher(String subProject) {
        return new SurefireLauncher(mavenLauncher.getSubProjectLauncher(subProject));
    }

    public OutputValidator getSubProjectValidator(String subProject) throws VerificationException {
        return mavenLauncher.getSubProjectValidator(subProject);
    }

    private SurefireLauncher addEnvVar(String key, String value) {
        mavenLauncher.addEnvVar(key, value);
        return this;
    }

    public SurefireLauncher setMavenOpts(String opts) {
        return addEnvVar("MAVEN_OPTS", opts);
    }

    private List<String> getInitialGoals() {
        List<String> goals = new ArrayList<>();

        goals.add("-Dsurefire.version=" + surefireVersion);

        String jacocoAgent = System.getProperty("jacoco.agent", "");
        // Remove unnecessary backslash escaping for Windows, see https://github.com/jacoco/jacoco/issues/1559
        jacocoAgent = jacocoAgent.replace("\\\\", "\\");
        // Remove quotes which will cause a syntax error raised by cmd.exe because of quote escaping
        jacocoAgent = jacocoAgent.replace("\"", "");
        goals.add("-Djacoco.agent=" + jacocoAgent);
        goals.add("-nsu");

        return goals;
    }

    public SurefireLauncher showErrorStackTraces() {
        mavenLauncher.showErrorStackTraces();
        return this;
    }

    public SurefireLauncher debugLogging() {
        mavenLauncher.debugLogging();
        return this;
    }

    @SuppressWarnings("UnusedDeclaration")
    public SurefireLauncher debugSurefireFork() {
        mavenLauncher.sysProp("maven.surefire.debug", "true");
        return this;
    }

    public SurefireLauncher failNever() {
        mavenLauncher.failNever();
        return this;
    }

    public SurefireLauncher groups(String groups) {
        mavenLauncher.sysProp("groups", groups);
        return this;
    }

    public SurefireLauncher addGoal(String goal) {
        mavenLauncher.addGoal(goal);
        return this;
    }

    public OutputValidator executeTest() {
        return mavenLauncher.execute("test");
    }

    public OutputValidator executeInstall() {
        return mavenLauncher.execute("install");
    }

    public FailsafeOutputValidator executeVerify() {
        OutputValidator verify = execute("verify");
        return new FailsafeOutputValidator(verify);
    }

    public OutputValidator execute(String goal) {
        return mavenLauncher.execute(goal);
    }

    public OutputValidator executeSurefireReport() {
        return mavenLauncher.execute("surefire-report:report");
    }

    public OutputValidator executeCurrentGoals() {
        return mavenLauncher.executeCurrentGoals();
    }

    public SurefireLauncher printSummary(boolean printsummary) {
        mavenLauncher.sysProp("printSummary", printsummary);
        return this;
    }

    public SurefireLauncher redirectToFile(boolean redirect) {
        mavenLauncher.sysProp("maven.test.redirectTestOutputToFile", redirect);
        return this;
    }

    public SurefireLauncher forkOnce() {
        return forkCount(1).reuseForks(true);
    }

    public SurefireLauncher forkNever() {
        return forkCount(0);
    }

    public SurefireLauncher forkAlways() {
        return forkCount(1).reuseForks(false);
    }

    public SurefireLauncher forkPerThread(int threadCount) {
        return forkCount(threadCount).reuseForks(true);
    }

    public SurefireLauncher threadCount(int threadCount) {
        mavenLauncher.sysProp("threadCount", threadCount);
        return this;
    }

    public SurefireLauncher forkCount(int forkCount) {
        mavenLauncher.sysProp("forkCount", forkCount);
        return this;
    }

    public SurefireLauncher reuseForks(boolean reuseForks) {
        mavenLauncher.sysProp("reuseForks", reuseForks);
        return this;
    }

    public SurefireLauncher runOrder(String runOrder) {
        mavenLauncher.sysProp("surefire.runOrder", runOrder);
        return this;
    }

    public SurefireLauncher runOrderRandomSeed(String runOrderRandomSeed) {
        mavenLauncher.sysProp("surefire.runOrder.random.seed", runOrderRandomSeed);
        return this;
    }

    public SurefireLauncher failIfNoTests(boolean fail) {
        mavenLauncher.sysProp("failIfNoTests", fail);
        return this;
    }

    public SurefireLauncher mavenTestFailureIgnore(boolean fail) {
        mavenLauncher.sysProp("maven.test.failure.ignore", fail);
        return this;
    }

    public SurefireLauncher failIfNoSpecifiedTests(boolean fail) {
        mavenLauncher.sysProp("surefire.failIfNoSpecifiedTests", fail);
        return this;
    }

    public SurefireLauncher useSystemClassLoader(boolean useSystemClassLoader) {
        mavenLauncher.sysProp("useSystemClassLoader", useSystemClassLoader);
        return this;
    }

    public SurefireLauncher activateProfile(String profile) {
        mavenLauncher.activateProfile(profile);
        return this;
    }

    public SurefireLauncher disablePerCoreThreadCount() {
        mavenLauncher.sysProp("perCoreThreadCount", false);
        return this;
    }

    public SurefireLauncher disableParallelOptimization() {
        mavenLauncher.sysProp("parallelOptimized", "false");
        return this;
    }

    public SurefireLauncher parallel(String parallel) {
        mavenLauncher.sysProp("parallel", parallel);
        return this;
    }

    public SurefireLauncher parallelSuites() {
        return parallel("suites");
    }

    public SurefireLauncher parallelClasses() {
        return parallel("classes");
    }

    public SurefireLauncher parallelMethods() {
        return parallel("methods");
    }

    public SurefireLauncher parallelBoth() {
        return parallel("both");
    }

    public SurefireLauncher parallelSuitesAndClasses() {
        return parallel("suitesAndClasses");
    }

    public SurefireLauncher parallelSuitesAndMethods() {
        return parallel("suitesAndMethods");
    }

    public SurefireLauncher parallelClassesAndMethods() {
        return parallel("classesAndMethods");
    }

    public SurefireLauncher parallelAll() {
        return parallel("all");
    }

    public SurefireLauncher useUnlimitedThreads() {
        mavenLauncher.sysProp("useUnlimitedThreads", true);
        return this;
    }

    public SurefireLauncher threadCountSuites(int count) {
        mavenLauncher.sysProp("threadCountSuites", count);
        return this;
    }

    public SurefireLauncher threadCountClasses(int count) {
        mavenLauncher.sysProp("threadCountClasses", count);
        return this;
    }

    public SurefireLauncher threadCountMethods(int count) {
        mavenLauncher.sysProp("threadCountMethods", count);
        return this;
    }

    public SurefireLauncher parallelTestsTimeoutInSeconds(double timeout) {
        mavenLauncher.sysProp("surefire.parallel.timeout", timeout);
        return this;
    }

    public SurefireLauncher parallelTestsTimeoutForcedInSeconds(double timeout) {
        mavenLauncher.sysProp("surefire.parallel.forcedTimeout", timeout);
        return this;
    }

    public SurefireLauncher argLine(String value) {
        mavenLauncher.sysProp("argLine", value);
        return this;
    }

    public SurefireLauncher sysProp(String variable, String value) {
        mavenLauncher.sysProp(variable, value);
        return this;
    }

    public SurefireLauncher setJUnitVersion(String version) {
        mavenLauncher.sysProp("junit.version", version);
        return this;
    }

    public SurefireLauncher setGroups(String groups) {
        mavenLauncher.sysProp("groups", groups);
        return this;
    }

    public SurefireLauncher setExcludedGroups(String excludedGroups) {
        mavenLauncher.sysProp("excludedGroups", excludedGroups);
        return this;
    }

    public File getUnpackedAt() {
        return mavenLauncher.getUnpackedAt();
    }

    public SurefireLauncher addFailsafeReportOnlyGoal() {
        mavenLauncher.addGoal(getReportPluginGoal("failsafe-report-only"));
        return this;
    }

    public SurefireLauncher addSurefireReportGoal() {
        mavenLauncher.addGoal(getReportPluginGoal("report"));
        return this;
    }

    public SurefireLauncher addSurefireReportOnlyGoal() {
        mavenLauncher.addGoal(getReportPluginGoal("report-only"));
        return this;
    }

    private String getReportPluginGoal(String goal) {
        return "org.apache.maven.plugins:maven-surefire-report-plugin:" + surefireVersion + ":" + goal;
    }

    public SurefireLauncher setTestToRun(String basicTest) {
        mavenLauncher.sysProp("test", basicTest);
        return this;
    }

    public SurefireLauncher setForkJvm() {
        setForkJvm(true);
        return this;
    }

    public SurefireLauncher setForkJvm(boolean forkJvm) {
        mavenLauncher.setForkJvm(forkJvm);
        return this;
    }

    public SurefireLauncher setLogFileName(String logFileName) {
        mavenLauncher.setLogFileName(logFileName);
        return this;
    }
}
