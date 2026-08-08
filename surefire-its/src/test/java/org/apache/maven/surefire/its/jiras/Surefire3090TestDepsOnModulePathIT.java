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
package org.apache.maven.surefire.its.jiras;

import java.io.File;

import org.apache.maven.surefire.its.fixture.AbstractJava9PlusIT;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for running test-scope dependencies as named modules on the module
 * path when a module-info-patch.args handoff file is present.
 * <p>
 * This test requires Maven 4 and is skipped when running with Maven 3.
 * It verifies that surefire correctly:
 * <ul>
 *   <li>Moves the modules referenced by the file's {@code add-modules} (the JUnit
 *       test-scope dependencies) from the classpath to the module path</li>
 *   <li>Passes the handoff file's directives through to the forked JVM verbatim</li>
 *   <li>Keeps reflective test access working with the moved named modules</li>
 * </ul>
 * The fixture asserts the placement behaviorally: {@code Test.class.getModule()} must be
 * the named module {@code org.junit.jupiter.api}, which fails when the engine is left on
 * the classpath in the unnamed module.
 */
class Surefire3090TestDepsOnModulePathIT extends AbstractJava9PlusIT {

    @Test
    void testTestDepsRunAsNamedModules() {
        assumeTrue(isMaven4Plus(), "This test requires Maven 4.");
        // 3 tests: ModulePlacementTest.junitApiIsNamedModuleOnModulePath
        //          + testRunsInsidePatchedModule + SecretWhiteboxTest.testReveal
        assumeJava9()
                .debugLogging()
                .executeTest()
                .verifyErrorFreeLog()
                .verifyTextInLog("Moved test-scope modules to the module path")
                .assertTestSuiteResults(3);
    }

    @Override
    protected String getProjectDirectoryName() {
        return "surefire-3090-test-deps-on-module-path";
    }

    private static boolean isMaven4Plus() {
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null) {
            return false;
        }
        File mavenLib = new File(mavenHome, "lib");
        if (!mavenLib.isDirectory()) {
            return false;
        }
        // Maven 4 ships maven-api-core; Maven 3 does not
        File[] files = mavenLib.listFiles((dir, name) -> name.startsWith("maven-api-core-") && name.endsWith(".jar"));
        return files != null && files.length > 0;
    }
}
