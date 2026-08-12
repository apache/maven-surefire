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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.api.util.RunOrder.ALPHABETICAL;
import static org.apache.maven.surefire.api.util.RunOrder.RANDOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderCalculatorTest {

    @Test
    public void testOrderTestClasses() {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun(getClassesToRun());
        RunOrderParameters alphabetical = new RunOrderParameters(new RunOrder[] {ALPHABETICAL}, null);
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator(alphabetical, 1);
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses(testsToRun);
        assertEquals(A.class, testsToRun1.iterator().next());
    }

    @Test
    public void testRandomOrderIsIndependentOfDiscoveryOrder() {
        long seed = 42L;
        RunOrderParameters random = new RunOrderParameters(new RunOrder[] {RANDOM}, null, seed);

        TestsToRun firstDiscoveryOrder = new TestsToRun(getClassesToRun(A.class, B.class, C.class, D.class));
        TestsToRun secondDiscoveryOrder = new TestsToRun(getClassesToRun(D.class, B.class, A.class, C.class));

        TestsToRun firstRandomOrder = new DefaultRunOrderCalculator(random, 1).orderTestClasses(firstDiscoveryOrder);
        TestsToRun secondRandomOrder = new DefaultRunOrderCalculator(random, 1).orderTestClasses(secondDiscoveryOrder);

        assertIterableEquals(firstRandomOrder, secondRandomOrder);
    }

    private Set<Class<?>> getClassesToRun() {
        return getClassesToRun(B.class, A.class);
    }

    private Set<Class<?>> getClassesToRun(Class<?>... classes) {
        return new LinkedHashSet<>(Arrays.asList(classes));
    }

    static class A {}

    static class B {}

    static class C {}

    static class D {}
}
