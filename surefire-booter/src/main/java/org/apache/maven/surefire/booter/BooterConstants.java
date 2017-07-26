package org.apache.maven.surefire.booter;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


/**
 * Constants used by the serializer/deserializer
 *
 * @author Kristian Rosenvold
 */
public final class BooterConstants
{
    private BooterConstants()
    {
    }

    public static final String SPECIFIC_TEST_PROPERTY_PREFIX = "specificTest";
    public static final String INCLUDES_PROPERTY_PREFIX = "includes";
    public static final String EXCLUDES_PROPERTY_PREFIX = "excludes";
    public static final String USESYSTEMCLASSLOADER = "useSystemClassLoader";
    public static final String USEMANIFESTONLYJAR = "useManifestOnlyJar";
    public static final String FAILIFNOTESTS = "failIfNoTests";
    public static final String ISTRIMSTACKTRACE = "isTrimStackTrace";
    public static final String REPORTSDIRECTORY = "reportsDirectory";
    public static final String TESTARTIFACT_VERSION = "testFwJarVersion";
    public static final String TESTARTIFACT_CLASSIFIER = "testFwJarClassifier";
    public static final String REQUESTEDTEST = "requestedTest";
    public static final String REQUESTEDTESTMETHOD = "requestedTestMethod";
    public static final String SOURCE_DIRECTORY = "testSuiteDefinitionTestSourceDirectory";
    public static final String TEST_CLASSES_DIRECTORY = "testClassesDirectory";
    public static final String RUN_ORDER = "runOrder";
    public static final String RUN_STATISTICS_FILE = "runStatisticsFile";
    public static final String TEST_SUITE_XML_FILES = "testSuiteXmlFiles";
    public static final String PROVIDER_CONFIGURATION = "providerConfiguration";
    public static final String FORKTESTSET = "forkTestSet";
    public static final String FORKTESTSET_PREFER_TESTS_FROM_IN_STREAM = "preferTestsFromInStream";
    public static final String RERUN_FAILING_TESTS_COUNT = "rerunFailingTestsCount";
    public static final String MAIN_CLI_OPTIONS = "mainCliOptions";
    public static final String FAIL_FAST_COUNT = "failFastCount";
    public static final String SHUTDOWN = "shutdown";
    public static final String SYSTEM_EXIT_TIMEOUT = "systemExitTimeout";
    public static final String PLUGIN_PID = "pluginPid";
}
