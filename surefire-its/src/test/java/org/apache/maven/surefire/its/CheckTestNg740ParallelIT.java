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
package org.apache.maven.surefire.its;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CheckTestNg740ParallelIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void withTestNG740AndParallelSet() {
        unpack("testng-740-parallel").executeTest().assertTestSuiteResults(2, 0, 0, 0);
    }

    @Test
    public void withTestNG740AndParallelSetWithoutThreadCount() {
        unpack("testng-740-parallel-without-threadcount").executeTest().assertTestSuiteResults(2, 0, 0, 0);
    }
}
