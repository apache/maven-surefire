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
package pkg;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NamedIndexTest {
    @Parameters(name = "{index}: square({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{1, 1}, {2, 4}, {3, 9}});
    }

    private final int input;

    private final int expected;

    public NamedIndexTest(int input, int expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void square() {
        System.out.println("NamedIndexTest.square input=" + input);
        assertEquals(expected, input * input);
    }
}
