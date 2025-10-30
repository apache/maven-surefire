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
package org.apache.maven.surefire.api.testset;

import java.io.File;

/**
 * Information about the requested test.
 *
 * @author Kristian Rosenvold
 */
public class TestRequest {

    private final File testSourceDirectory;

    private final TestListResolver requestedTests;

    private final int rerunFailingTestsCount;

    public TestRequest(File testSourceDirectory, TestListResolver requestedTests, int rerunFailingTestsCount) {
        this.testSourceDirectory = testSourceDirectory;
        this.requestedTests = requestedTests;
        this.rerunFailingTestsCount = rerunFailingTestsCount;
    }

    /**
     * Test source directory, normally ${project.build.testSourceDirectory}
     *
     * @return A file pointing to test sources
     */
    public File getTestSourceDirectory() {
        return testSourceDirectory;
    }

    /**
     * A specific test request issued with -Dtest= from the command line.
     *
     * @return filter
     */
    public TestListResolver getTestListResolver() {
        return requestedTests;
    }

    /**
     * How many times to rerun failing tests, issued with -Dsurefire.rerunFailingTestsCount from the command line.
     *
     * @return The int parameter to indicate how many times to rerun failing tests
     */
    public int getRerunFailingTestsCount() {
        return rerunFailingTestsCount;
    }
}
