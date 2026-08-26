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
package issue3446;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NestedTest {
    private boolean outerSetUp;

    @BeforeEach
    void setUpOuter() {
        outerSetUp = true;
    }

    @Test
    void outerTestMustNotRun() {
        fail("The outer test must not run");
    }

    @Nested
    class Intermediate {
        private boolean intermediateSetUp;

        @BeforeEach
        void setUpIntermediate() {
            assertTrue(outerSetUp);
            intermediateSetUp = true;
        }

        @Test
        void intermediateTestMustNotRun() {
            fail("The intermediate test must not run");
        }

        @Nested
        class Selected {
            @Test
            void selectedTest() {
                assertTrue(outerSetUp);
                assertTrue(intermediateSetUp);
            }
        }

        @Nested
        class Sibling {
            @Test
            void siblingTestMustNotRun() {
                fail("A sibling nested test must not run");
            }
        }
    }
}
