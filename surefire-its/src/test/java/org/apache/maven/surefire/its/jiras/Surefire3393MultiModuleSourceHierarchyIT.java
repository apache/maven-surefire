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
 * Integration test for whitebox testing of SEVERAL Java modules declared in one POM
 * using Maven 4 Module Source Hierarchy and module-info-patch.maven.
 * <p>
 * This test requires Maven 4 and is skipped when running with Maven 3.
 * It verifies that surefire correctly:
 * <ul>
 *   <li>Detects all modules in nested target/classes/&lt;module&gt;/ directories</li>
 *   <li>Scans test classes from every nested target/test-classes/&lt;module&gt;/ directory</li>
 *   <li>Patches each module with its own test classes in a single execution</li>
 *   <li>Places dependencies required by ANY of the modules on the module path
 *       (here: jakarta.json, required by com.example.core only)</li>
 * </ul>
 */
class Surefire3393MultiModuleSourceHierarchyIT extends AbstractJava9PlusIT {

    @Test
    void testWhiteboxWithMultiModuleSourceHierarchy() {
        assumeTrue(isMaven4Plus(), "This test requires Maven 4.");
        // 5 tests: core CalculatorTest.testAdd + testKindOfJsonValue + MathHelperWhiteboxTest.testAdd
        //          extra DoublerTest.testDoubled + TwiceHelperWhiteboxTest.testTwice
        assumeJava9().debugLogging().executeTest().verifyErrorFreeLog().assertTestSuiteResults(5);
    }

    @Override
    protected String getProjectDirectoryName() {
        return "surefire-3393-multi-module-source-hierarchy";
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
