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
package org.apache.maven.surefire.api.util.internal;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchPatternsTest {

    @Test
    void matches() {
        MatchPatterns from = MatchPatterns.from("ABC**", "CDE**");
        assertTrue(from.matches("ABCDE", true));
        assertTrue(from.matches("CDEF", true));
        assertFalse(from.matches("XYZ", true));
    }

    @Test
    void matchesPatternStart() {
        MatchPatterns from = MatchPatterns.from("ABC" + File.separator + "**");
        assertTrue(from.matchesPatternStart("ABC", true));
        assertFalse(from.matchesPatternStart("XYZ", true));
    }

    @Test
    void caseInsensitiveWhenAsked() {
        MatchPatterns from = MatchPatterns.from("abc*");
        assertFalse(from.matches("ABCD", true));
        assertTrue(from.matches("ABCD", false));
    }
}
