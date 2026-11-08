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

import static org.apache.maven.surefire.api.util.internal.SelectorUtils.ANT_HANDLER_PREFIX;
import static org.apache.maven.surefire.api.util.internal.SelectorUtils.PATTERN_HANDLER_SUFFIX;
import static org.apache.maven.surefire.api.util.internal.SelectorUtils.REGEX_HANDLER_PREFIX;
import static org.apache.maven.surefire.api.util.internal.SelectorUtils.matchPath;
import static org.apache.maven.surefire.api.util.internal.SelectorUtils.matchPatternStart;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorUtilsTest {

    @Test
    void matchPatternStartRejectsNull() {
        assertThrows(NullPointerException.class, () -> matchPatternStart(null, null));
    }

    @Test
    void emptyStrings() {
        assertTrue(matchPatternStart("", ""));
    }

    @Test
    void regexPrefixedPatternStart() {
        assertTrue(matchPatternStart(REGEX_HANDLER_PREFIX + File.separator + "aaa" + PATTERN_HANDLER_SUFFIX, ""));
    }

    @Test
    void antPatternStart() {
        assertFalse(matchPatternStart(ant("/aaa"), ""));
        assertFalse(matchPatternStart(ant("\\aaa"), ""));
        assertTrue(matchPatternStart(ant("aaa"), ""));
        assertTrue(matchPatternStart(ant("/aaa/bbb"), "/aaa/bbb"));
        assertTrue(matchPatternStart(ant("/aaa/**"), "/aaa/bbb"));
        assertFalse(matchPatternStart(ant("/aaa/**"), "/ccc/bbb"));
        assertTrue(matchPatternStart(ant("/aaa/**"), "\\aaa\\bbb"));
        assertFalse(matchPatternStart(ant("/aaa/**"), "\\ccc\\bbb"));
        assertFalse(matchPatternStart(ant("/aaa/"), "\\aaa\\bbb"));
    }

    @Test
    void antPaths() {
        String sep = File.separator;
        assertTrue(matchPath("**" + sep + "*Test.class", "a" + sep + "b" + sep + "FooTest.class"));
        assertTrue(matchPath("**" + sep + "*Test.class", "FooTest.class"));
        assertFalse(matchPath("**" + sep + "*Test.class", "a" + sep + "Foo.class"));
        assertTrue(matchPath("a" + sep + "?.txt", "a" + sep + "b.txt"));
        assertFalse(matchPath("a" + sep + "?.txt", "a" + sep + "bb.txt"));
        assertTrue(matchPath("a" + sep + "**", "a" + sep + "b" + sep + "c"));
        assertFalse(matchPath("a" + sep + "**", "b" + sep + "c"));
    }

    @Test
    void caseSensitivity() {
        assertFalse(matchPath("*.TXT", "a.txt", true));
        assertTrue(matchPath("*.TXT", "a.txt", false));
    }

    @Test
    void regexPaths() {
        assertTrue(matchPath(REGEX_HANDLER_PREFIX + ".*Test\\.class" + PATTERN_HANDLER_SUFFIX, "FooTest.class"));
        assertFalse(matchPath(REGEX_HANDLER_PREFIX + ".*Test\\.class" + PATTERN_HANDLER_SUFFIX, "Foo.class"));
    }

    private static String ant(String pattern) {
        return ANT_HANDLER_PREFIX + pattern + PATTERN_HANDLER_SUFFIX;
    }
}
