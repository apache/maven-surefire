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
package org.apache.maven.surefire.api.testset;

import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.api.testset.ResolvedTest.Type.CLASS;
import static org.apache.maven.surefire.api.testset.ResolvedTest.Type.METHOD;
import static org.apache.maven.surefire.api.testset.ResolvedTest.fromFullyQualifiedClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class ResolvedTestTest {
    @Test
    public void testEmptyClassRegex() {
        ResolvedTest test = new ResolvedTest(CLASS, "  ", true);
        assertNull(test.getTestClassPattern());
        assertNull(test.getTestMethodPattern());
        assertFalse(test.hasTestClassPattern());
        assertFalse(test.hasTestMethodPattern());
        assertTrue(test.isRegexTestClassPattern());
        assertFalse(test.isRegexTestMethodPattern());
        assertTrue(test.isEmpty());
    }

    @Test
    public void testEmptyMethodRegex() {
        ResolvedTest test = new ResolvedTest(METHOD, "  ", true);
        assertNull(test.getTestClassPattern());
        assertNull(test.getTestMethodPattern());
        assertFalse(test.hasTestClassPattern());
        assertFalse(test.hasTestMethodPattern());
        assertFalse(test.isRegexTestClassPattern());
        assertTrue(test.isRegexTestMethodPattern());
        assertTrue(test.isEmpty());
    }

    @Test
    public void testFromFullyQualifiedClass() {
        String classFileName = fromFullyQualifiedClass("my.package.MyTest");
        assertEquals("my/package/MyTest", classFileName);

        classFileName = fromFullyQualifiedClass("my.package.MyTest.class");
        assertEquals("my/package/MyTest.class", classFileName);

        classFileName = fromFullyQualifiedClass("my/package/MyTest.class");
        assertEquals("my/package/MyTest.class", classFileName);

        classFileName = fromFullyQualifiedClass("my/package/MyTest.*");
        assertEquals("my/package/MyTest.*", classFileName);

        classFileName = fromFullyQualifiedClass("my.package.MyTest.*");
        assertEquals("my/package/MyTest.*", classFileName);
    }

    @Test
    public void testRegexPatternUsesForwardSlashesLikeTestClassFile() {
        // Per inclusion-exclusion.md.vm: "Regex matches are done over paths using slashes ("/") ...
        // Slashes here are forward, even on Windows." testClassFile (built by FileScanner) is always
        // forward-slash too, so neither side of this match should ever be converted to the platform's
        // separator - %regex[it/.*] must keep matching "it/ExampleIntegrationTest.class" everywhere.
        ResolvedTest test = new ResolvedTest(CLASS, "it/.*", true);
        assertTrue(test.matchAsInclusive("it/ExampleIntegrationTest.class", null));
        assertFalse(test.matchAsInclusive("other/ExampleIntegrationTest.class", null));
    }
}
