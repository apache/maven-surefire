package org.apache.maven.surefire.common.junit48;

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

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.runner.Version;
import org.apache.maven.surefire.common.junit48.tests.group.ABCParameterizedTest;
import org.apache.maven.surefire.common.junit48.tests.group.ABCTest;
import org.apache.maven.surefire.common.junit48.tests.group.ATest;
import org.apache.maven.surefire.common.junit48.tests.group.BBCTest;
import org.apache.maven.surefire.common.junit48.tests.group.BCTest;
import org.apache.maven.surefire.common.junit48.tests.group.BTest;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.SingleGroupMatcher;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.runner.Description.createSuiteDescription;
import static org.junit.runner.Description.createTestDescription;

/**
 * Tests covering inheritance in @Categories for Test classes.
 */
public class GroupMatcherCategoryFilterTest
{
    private GroupMatcherCategoryFilter cut;

    @BeforeClass
    public static void printVersion()
    {
        System.out.println( Version.id() );
    }

    @Test
    public void shouldMatchIncludedCategoryInSelf()
    {
        GroupMatcher included =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryB" );
        GroupMatcher excluded = null;
        cut = new GroupMatcherCategoryFilter( included, excluded );
        assertTrue( cut.shouldRun( createSuiteDescription( BTest.class ) ) );
    }

    @Test
    public void shouldMatchIncludedCategoryInParent()
    {
        GroupMatcher included =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryB" );
        GroupMatcher excluded = null;
        cut = new GroupMatcherCategoryFilter( included, excluded );
        assertTrue( cut.shouldRun( createSuiteDescription( BCTest.class ) ) );
        assertFalse( cut.shouldRun( createSuiteDescription( ATest.class ) ) );
    }

    @Test
    public void shouldMatchIncludedCategoryInHierarchy()
    {
        GroupMatcher included =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryC" );
        GroupMatcher excluded = null;
        cut = new GroupMatcherCategoryFilter( included, excluded );
        assertTrue( cut.shouldRun( createSuiteDescription( ABCTest.class ) ) );
        assertTrue( cut.shouldRun( createSuiteDescription( BCTest.class ) ) );
        assertFalse( cut.shouldRun( createSuiteDescription( ATest.class ) ) );
    }

    @Test
    public void shouldMatchIncludedCategoryInParentWhenSelfHasAnother()
    {
        GroupMatcher included =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryB" );
        GroupMatcher excluded = null;
        cut = new GroupMatcherCategoryFilter( included, excluded );
        assertTrue( cut.shouldRun( createSuiteDescription( ABCTest.class ) ) );
        assertTrue( cut.shouldRun( createTestDescription( ABCTest.class, "abc" ) ) );
        assertTrue( cut.shouldRun( createSuiteDescription( ABCParameterizedTest.class ) ) );
        assertTrue( cut.shouldRun( createTestDescription( ABCParameterizedTest.class, "abc" ) ) );
    }

    @Test
    public void shouldNotMatchIncludedCategoryInParentWhenSelfHasExcludedCategory()
    {
        GroupMatcher included =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryB" );
        GroupMatcher excluded =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryA" );
        cut = new GroupMatcherCategoryFilter( included, excluded );
        assertFalse( cut.shouldRun( createSuiteDescription( ABCTest.class ) ) );
        assertTrue( cut.shouldRun( createSuiteDescription( BBCTest.class ) ) );
        assertTrue( cut.shouldRun( createSuiteDescription( BTest.class ) ) );
    }

    @Test
    public void shouldMatchExcludedCategoryInSelf()
    {
        GroupMatcher included = null;
        GroupMatcher excluded =
            new SingleGroupMatcher( "org.apache.maven.surefire.common.junit48.tests.group.marker.CategoryA" );
        cut = new GroupMatcherCategoryFilter( included, excluded );
        assertFalse( cut.shouldRun( createSuiteDescription( ATest.class ) ) );
        assertTrue( cut.shouldRun( createSuiteDescription( BTest.class ) ) );
        assertTrue( cut.shouldRun( createSuiteDescription( BBCTest.class ) ) );
    }

    /**
     *
     */
    public static class JUnit4SuiteTest extends TestCase
    {
        public static junit.framework.Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest( new JUnit4TestAdapter( GroupMatcherCategoryFilterTest.class ) );
            return suite;
        }
    }
}
