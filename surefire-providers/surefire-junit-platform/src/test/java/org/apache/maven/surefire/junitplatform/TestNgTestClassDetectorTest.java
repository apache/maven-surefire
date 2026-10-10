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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TestNgTestClassDetector}.
 * <p>
 * TestNG itself is not a dependency of this module, so the detection logic is exercised with a stand-in
 * annotation. Only the resolution of {@code org.testng.annotations.Test} depends on the real TestNG.
 */
class TestNgTestClassDetectorTest {

    private final TestNgTestClassDetector detector = new TestNgTestClassDetector(TestNgLikeTest.class);

    @Test
    void classWithoutAnnotationIsNotATestNgTestClass() {
        assertFalse(detector.isTestNgTestClass(PlainClass.class));
    }

    @Test
    void classAnnotatedAtTypeLevelIsATestNgTestClass() {
        assertTrue(detector.isTestNgTestClass(AnnotatedClass.class));
    }

    @Test
    void classWithAnnotatedMethodIsATestNgTestClass() {
        assertTrue(detector.isTestNgTestClass(ClassWithAnnotatedMethod.class));
    }

    @Test
    void classInheritingTypeLevelAnnotationIsATestNgTestClass() {
        assertTrue(detector.isTestNgTestClass(SubclassOfAnnotatedClass.class));
    }

    @Test
    void classInheritingAnnotatedMethodIsATestNgTestClass() {
        assertTrue(detector.isTestNgTestClass(SubclassOfClassWithAnnotatedMethod.class));
    }

    @Test
    void classImplementingInterfaceWithAnnotatedMethodIsATestNgTestClass() {
        assertTrue(detector.isTestNgTestClass(ImplementsAnnotatedInterface.class));
    }

    @Test
    void engineIsNotExcludedWhenTestNgIsNotOnTheClassPath() {
        assertFalse(TestNgTestClassDetector.canExcludeTestNgEngine(
                getClass().getClassLoader(), singletonList(PlainClass.class.getName())));
    }

    @Test
    void engineIsNotExcludedWithoutClassLoader() {
        assertFalse(TestNgTestClassDetector.canExcludeTestNgEngine(null, emptyList()));
    }

    @Test
    void detectorIsNotCreatedWhenTestNgIsNotOnTheClassPath() {
        assertFalse(TestNgTestClassDetector.tryCreate(getClass().getClassLoader()) != null);
    }

    @Test
    void engineIsNotExcludedWhenAClassCannotBeLoaded() {
        assertFalse(TestNgTestClassDetector.canExcludeTestNgEngine(
                getClass().getClassLoader(), asList(PlainClass.class.getName(), "does.not.Exist")));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface TestNgLikeTest {}

    static class PlainClass {

        void method() {}
    }

    @TestNgLikeTest
    static class AnnotatedClass {}

    static class SubclassOfAnnotatedClass extends AnnotatedClass {}

    static class ClassWithAnnotatedMethod {

        @TestNgLikeTest
        void test() {}
    }

    static class SubclassOfClassWithAnnotatedMethod extends ClassWithAnnotatedMethod {}

    interface AnnotatedInterface {

        @TestNgLikeTest
        void test();
    }

    abstract static class ImplementsAnnotatedInterface implements AnnotatedInterface {}
}
