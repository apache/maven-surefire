package org.apache.maven.surefire.junitcore;

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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.maven.surefire.common.junit4.JUnit4Reflector;
import org.apache.maven.surefire.api.util.ReflectionUtils;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Reflector Test with junit 4.8.1
 *
 * @author Kristian Rosenvold
 */
public class JUnit4Reflector481Test
{
    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    @Test
    public void testGetAnnotatedIgnore()
    {
        final Method testSomething2 = ReflectionUtils.getMethod( IgnoreWithDescription.class, "testSomething2",
                EMPTY_CLASS_ARRAY );
        final Annotation[] annotations = testSomething2.getAnnotations();
        Description desc = Description.createTestDescription( IgnoreWithDescription.class, "testSomething2",
                annotations );
        Ignore annotatedIgnore = JUnit4Reflector.getAnnotatedIgnore( desc );
        assertNotNull( annotatedIgnore );
        assertEquals(
                "testSomething2" + "(org.apache.maven.surefire.junitcore.JUnit4Reflector481Test$IgnoreWithDescription)",
                desc.getDisplayName() );
        assertEquals(
                "testSomething2" + "(org.apache.maven.surefire.junitcore.JUnit4Reflector481Test$IgnoreWithDescription)",
                desc.toString() );
        assertEquals( "org.apache.maven.surefire.junitcore.JUnit4Reflector481Test$IgnoreWithDescription",
                desc.getClassName() );
        assertEquals( "testSomething2", desc.getMethodName() );
        assertEquals( 0, desc.getChildren().size() );
        assertEquals( 2, desc.getAnnotations().size() );
        assertSame( annotatedIgnore, desc.getAnnotation( Ignore.class ) );
        assertEquals( REASON, annotatedIgnore.value() );
    }

    @Test
    public void testGetAnnotatedIgnoreWithoutClass()
    {
        final Method testSomething2 = ReflectionUtils.getMethod( IgnoreWithDescription.class, "testSomething2",
                EMPTY_CLASS_ARRAY );
        final Annotation[] annotations = testSomething2.getAnnotations();
        Description desc = Description.createSuiteDescription( "testSomething2", annotations );
        Ignore annotatedIgnore = JUnit4Reflector.getAnnotatedIgnore( desc );
        assertNotNull( annotatedIgnore );
        assertEquals( "testSomething2", desc.getDisplayName() );
        assertEquals( "testSomething2", desc.toString() );
        assertEquals( "testSomething2", desc.getClassName() );
        assertNull( desc.getMethodName() );
        assertEquals( 0, desc.getChildren().size() );
        assertEquals( 2, desc.getAnnotations().size() );
        assertSame( annotatedIgnore, desc.getAnnotation( Ignore.class ) );
        assertEquals( REASON, annotatedIgnore.value() );
    }

    private static final String REASON = "Ignorance is bliss";

    /**
     *
     */
    public static class IgnoreWithDescription
    {

        @Test
        @Ignore( REASON )
        public void testSomething2()
        {
        }
    }

    @Test
    public void testCreatePureDescription()
    {
        Description description = JUnit4Reflector.createDescription( "exception" );
        assertEquals( "exception", description.getDisplayName() );
        assertEquals( "exception", description.toString() );
        assertEquals( 0, description.getChildren().size() );
    }

    @Test
    public void testCreateDescription()
    {
        Ignore ignore = JUnit4Reflector.createIgnored( "error" );
        Description description = JUnit4Reflector.createDescription( "exception", ignore );
        assertEquals( "exception", description.getDisplayName() );
        assertEquals( "exception", description.toString() );
        assertEquals( "exception", description.getClassName() );
        assertEquals( 0, description.getChildren().size() );
        Ignore annotatedIgnore = JUnit4Reflector.getAnnotatedIgnore( description );
        assertNotNull( annotatedIgnore );
        assertEquals( "error", annotatedIgnore.value() );
    }
}
