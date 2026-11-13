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
package issue3548;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ParameterizedClass
@ValueSource(ints = {1, 2})
class CalculatorParameterizedClassTests {

    @Parameter
    int i;

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void parameterizedTest(int j) {}

    @Nested
    @ParameterizedClass
    @ValueSource(ints = {1, 2})
    class Inner {

        final int j;

        Inner(int j) {
            this.j = j;
        }

        @Test
        void regularTest() {}
    }
}
