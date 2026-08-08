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
package org.apache.druid.sql.calcite;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class CalciteJoinQueryTest {
    private static final AtomicInteger FAILURES_REMAINING = new AtomicInteger(2);

    @Test
    @Parameters(source = QueryContextForJoinProvider1.class)
    public void ensureDecoupledTestConfigAnnotationWorks(Map<String, Object> queryContext) {
        assertTrue("fails only on the first invocation", FAILURES_REMAINING.getAndDecrement() <= 0);
    }

    @Test
    @Parameters(source = QueryContextForJoinProvider1.class)
    public void testJoinWithNonEquiCondition(Map<String, Object> queryContext) {
        Assume.assumeFalse(true);
    }

    @Test
    @Parameters(source = QueryContextForJoinProvider1.class)
    public void testTopNOnStringWithNonSortedOrUniqueDictionary(Map<String, Object> queryContext) {}

    public static class QueryContextForJoinProvider1 {
        public static Object[] provideQueryContexts() {
            return new Object[] {new HashMap<>(), new HashMap<>()};
        }
    }
}
