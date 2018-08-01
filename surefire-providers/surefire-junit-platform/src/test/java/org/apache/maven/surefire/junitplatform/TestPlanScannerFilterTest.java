package org.apache.maven.surefire.junitplatform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.core.LauncherFactory;

/**
 * Unit tests for {@link TestPlanScannerFilter}.
 *
 * @since 2.22.0
 */
public class TestPlanScannerFilterTest
{

    @Test
    public void emptyClassIsNotAccepted()
    {
        assertFalse( newFilter().accept( EmptyClass.class ), "does not accept empty class" );
    }

    @Test
    public void classWithNoTestMethodsIsNotAccepted()
    {
        assertFalse( newFilter().accept( ClassWithMethods.class ), "does not accept class with no @Test methods" );
    }

    @Test
    public void classWithTestMethodsIsAccepted()
    {
        assertTrue( newFilter().accept( ClassWithTestMethods.class ) );
    }

    @Test
    public void classWithNestedTestClassIsAccepted()
    {
        assertTrue( newFilter().accept( ClassWithNestedTestClass.class ) );
    }

    @Test
    public void classWithDeeplyNestedTestClassIsAccepted()
    {
        assertTrue( newFilter().accept( ClassWithDeeplyNestedTestClass.class ) );
    }

    @Test
    public void classWithTestFactoryIsAccepted()
    {
        assertTrue( newFilter().accept( ClassWithTestFactory.class ) );
    }

    @Test
    public void classWithNestedTestFactoryIsAccepted()
    {
        assertTrue( newFilter().accept( ClassWithNestedTestFactory.class ) );
    }

    private static TestPlanScannerFilter newFilter()
    {
        return new TestPlanScannerFilter( LauncherFactory.create(), new Filter<?>[0] );
    }

    static class EmptyClass
    {
    }

    static class ClassWithMethods
    {

        void method1()
        {
        }

        void method2()
        {
        }
    }

    static class ClassWithTestMethods
    {

        @Test
        void test1()
        {
        }

        @org.junit.jupiter.api.Test
        public void test2()
        {
        }
    }

    static class ClassWithNestedTestClass
    {

        void method()
        {
        }

        @Nested
        class TestClass
        {

            @org.junit.jupiter.api.Test
            void test1()
            {
            }
        }
    }

    static class ClassWithDeeplyNestedTestClass
    {

        @Nested
        class Level1
        {

            @Nested
            class Level2
            {

                @Nested
                class TestClass
                {

                    @org.junit.jupiter.api.Test
                    void test1()
                    {
                    }
                }
            }
        }
    }

    static class ClassWithTestFactory
    {

        @TestFactory
        Stream<DynamicTest> tests()
        {
            return Stream.empty();
        }
    }

    static class ClassWithNestedTestFactory
    {

        @Nested
        class TestClass
        {

            @TestFactory
            List<DynamicTest> tests()
            {
                return emptyList();
            }
        }
    }
}
