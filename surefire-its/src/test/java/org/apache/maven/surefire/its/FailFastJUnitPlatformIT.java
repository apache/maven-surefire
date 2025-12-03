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

import java.util.Arrays;

import org.junit.runners.Parameterized.Parameters;

public class FailFastJUnitPlatformIT extends AbstractFailFastIT {

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        //                    description    profile     forkCount,
        //                    |              |           |  fail-fast-count,
        //                    |              |           |  |  reuseForks
        //                    |              |           |  |  |      total
        //                    |              |           |  |  |      |  failures
        //                    |              |           |  |  |      |  |  errors
        //                    |              |           |  |  |      |  |  |  skipped
        //                    |              |           |  |  |      |  |  |  |  pipes
        return Arrays.asList( //             |           |  |  |      |  |  |  |  |
                new Object[] {"oneFork-ff1", null, props(1, 1, true), 5, 0, 1, 4, true},
                new Object[] {"oneFork-ff2", null, props(1, 2, true), 5, 0, 2, 3, true},
                new Object[] {"twoForks-ff1", null, props(2, 1, true), 5, 0, 2, 3, true},
                new Object[] {"twoForks-ff2", null, props(2, 2, true), 5, 0, 2, 2, true},
                new Object[] {"oneFork-ff3", null, props(1, 3, true), 5, 0, 2, 0, true},
                new Object[] {"twoForks-ff3", null, props(2, 3, true), 5, 0, 2, 0, true});
    }

    @Override
    protected String withProvider() {
        return "junit-platform";
    }
}
