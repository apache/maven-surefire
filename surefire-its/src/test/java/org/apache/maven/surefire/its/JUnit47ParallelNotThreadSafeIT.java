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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Testing {@code @net.jcip.annotations.NotThreadSafe} with ParallelComputerBuilder.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public class JUnit47ParallelNotThreadSafeIT extends SurefireJUnit4IntegrationTestCase {

    private SurefireLauncher unpack() {
        return unpack("junit47-parallel-nts");
    }

    @Test
    public void test() {
        unpack().parallelAll()
                .useUnlimitedThreads()
                .executeTest()
                .verifyErrorFree(2)
                .verifyTextInLog("xxx-maven-surefire-plugin@NotThreadSafe")
                .verifyTextInLog("expected-thread");
    }
}
