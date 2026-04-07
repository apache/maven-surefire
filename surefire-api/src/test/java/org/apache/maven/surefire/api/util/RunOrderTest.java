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
package org.apache.maven.surefire.api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class RunOrderTest {

    @Test
    public void testMultiValue() {
        final RunOrder[] hourlies = RunOrder.valueOfMulti("failedfirst,balanced");
        assertEquals(RunOrder.FAILEDFIRST, hourlies[0]);
        assertEquals(RunOrder.BALANCED, hourlies[1]);
    }

    @Test
    public void testAsString() {
        RunOrder[] orders = new RunOrder[] {RunOrder.FAILEDFIRST, RunOrder.ALPHABETICAL};
        assertEquals("failedfirst,alphabetical", RunOrder.asString(orders));
    }

    @Test
    public void testShouldReturnNullForNullName() {
        assertTrue(RunOrder.valueOfMulti(null).length == 0);
    }

    @Test
    public void testShouldThrowExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> RunOrder.valueOfMulti("arbitraryName"));
    }
}
