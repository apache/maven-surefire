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

        classFileName = fromFullyQualifiedClass("dev.aspectj.*");
        assertEquals("dev/aspectj/*", classFileName);

        classFileName = fromFullyQualifiedClass("dev.*");
        assertEquals("dev/*", classFileName);
    }

    @Test
    public void testPackageWildcardMatchesAllClassesInPackage() {
        ResolvedTest test = new ResolvedTest(CLASS, "dev.aspectj.*", false);
        assertTrue(test.matchAsInclusive("dev/aspectj/MyTest.class", null));
        assertTrue(test.matchAsInclusive("dev/aspectj/OtherTest.class", null));
        assertFalse(test.matchAsInclusive("dev/aspectj/sub/NestedTest.class", null));
        assertFalse(test.matchAsInclusive("dev/aspectjx/MyTest.class", null));
        assertFalse(test.matchAsInclusive("other/MyTest.class", null));
    }

    @Test
    public void testSingleSegmentPackageWildcardMatchesClassesInThatPackage() {
        ResolvedTest test = new ResolvedTest(CLASS, "dev.*", false);
        assertTrue(test.matchAsInclusive("dev/MyTest.class", null));
        assertFalse(test.matchAsInclusive("dev/sub/NestedTest.class", null));
        assertFalse(test.matchAsInclusive("devx/MyTest.class", null));
        assertFalse(test.matchAsInclusive("other/MyTest.class", null));
    }

    @Test
    public void testClassWildcardWithLowercaseNameStillMatchesTheClassItself() {
        // "org.example.lowerTest.*" looks like a package wildcard by the naming convention used to
        // resolve the dot-notation ambiguity, but a pre-existing class named "lowerTest" must still
        // match itself: the package interpretation is additional, not a replacement.
        ResolvedTest test = new ResolvedTest(CLASS, "org.example.lowerTest.*", false);
        assertTrue(test.matchAsInclusive("org/example/lowerTest.class", null));
        assertTrue(test.matchAsInclusive("org/example/lowerTest/NestedInPackage.class", null));
        assertFalse(test.matchAsInclusive("org/example/other.class", null));
    }
}
