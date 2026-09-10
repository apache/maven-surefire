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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The fork must apply {@code systemPropertyVariables} before it initializes anything which reads them once and
 * caches the result. The JDK networking layer is such a case: loading the native "net" library freezes
 * {@code java.net.preferIPv4Stack}, so a fork which touched {@code java.nio} channels first silently kept the IPv6
 * stack.
 * <p>
 * Not applicable on Windows, where {@code sun.nio.fs.WindowsNativeDispatcher} loads the native "net" library itself
 * ("nio.dll has dependency on net.dll"). Any jar on the class path is enough to trigger it during JVM start-up, so
 * no forked JVM can honour {@code java.net.preferIPv4Stack} set from within {@code main}.
 *
 * @see <a href="https://github.com/apache/maven-surefire/issues/3456">GitHub issue #3456</a>
 */
public class Surefire3456PreferIPv4StackIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    void preferIPv4StackIsAppliedBeforeNetworkingIsInitialized() {
        assumeFalse(IS_OS_WINDOWS);

        executeErrorFreeTest("surefire-3456-prefer-ipv4-stack", 1);
    }
}
