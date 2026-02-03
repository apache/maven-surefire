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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Test project using -Dtest=mtClass#myMethod+myMethod2,secondClass#testMethod
 *
 * @author <a href="mailto:ytsolar@gmail.com">rainLee</a>
 */
public class TestMultipleMethodsIT extends SurefireJUnit4IntegrationTestCase {

    public OutputValidator multipleMethod(String projectName, String... goals) {
        SurefireLauncher launcher = unpack(projectName);
        for (String goal : goals) {
            launcher.addGoal(goal);
        }
        return launcher.showErrorStackTraces()
                .debugLogging()
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults(3, 0, 0, 0);
    }

    @Test
    public void testJunit44() {
        multipleMethod("junit44-multiple-methods");
    }

    @Test
    public void testJunit5() {
        multipleMethod("junit44-multiple-methods");
    }
}
