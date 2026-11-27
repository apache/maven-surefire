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
package org.apache.maven.surefire.booter;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_UNIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link PpidChecker}.
 */
public class PpidCheckerTest {

    @Test
    public void shouldPassPpidToPsAsSingleArgumentWithoutShell() {
        String ppid = "1; echo";

        String[] command = PpidChecker.unixPsCommand(ppid);

        assertThat(command[0]).isIn("/usr/bin/ps", "/bin/ps");
        assertThat(command).doesNotContain("/bin/sh", "-c");
        if (IS_OS_LINUX) {
            assertThat(command).containsExactly(command[0], "-o", "etime,pid", ppid);
        } else {
            assertThat(command).containsExactly(command[0], "-o", "etime,pid", "-p", ppid);
        }
    }

    @Test
    public void shouldReadCurrentProcessWithPs() {
        assumeTrue(IS_OS_UNIX && PpidChecker.canExecuteUnixPs());
        String expectedPid =
                ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

        ProcessInfo processInfo = new PpidChecker(expectedPid).unix();

        assertThat(processInfo.isInvalid()).isFalse();
        assertThat(processInfo.getPID()).isEqualTo(expectedPid);
    }
}
