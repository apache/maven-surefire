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
package org.apache.maven.surefire.junitplatform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JUnitPlatformProvider#deriveJupiterRandomSeed(long)}.
 *
 * @since 3.6.2
 */
public class JupiterRandomSeedTest {

    @Test
    public void derivedSeedDiffersFromSurefireSeedButStaysReproducible() {
        long derived = JUnitPlatformProvider.deriveJupiterRandomSeed(1234L);

        assertThat(derived).isNotEqualTo(1234L);
        assertThat(JUnitPlatformProvider.deriveJupiterRandomSeed(1234L)).isEqualTo(derived);
    }

    /**
     * Reproduces the reported symptom directly: with the raw Surefire seed reused for Jupiter's class
     * shuffle, two classes shuffled twice with the same seed always come back in the same relative order
     * (shuffle composed with itself is always an involution on a 2-element list). Deriving a different
     * seed for the second shuffle removes that restriction.
     */
    @Test
    public void doubleShuffleWithDerivedSeedIsNotRestrictedToTheOriginalOrder() {
        boolean swappedAtLeastOnce = false;
        for (long seed = 1L; seed <= 500L; seed++) {
            List<String> classes = new ArrayList<>(Arrays.asList("demo.ATest", "demo.BTest"));
            Collections.shuffle(classes, new Random(seed));

            long jupiterSeed = JUnitPlatformProvider.deriveJupiterRandomSeed(seed);
            Collections.shuffle(classes, new Random(jupiterSeed));

            if (!classes.get(0).equals("demo.ATest")) {
                swappedAtLeastOnce = true;
                break;
            }
        }

        assertThat(swappedAtLeastOnce).isTrue();
    }
}
