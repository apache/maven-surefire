package org.apache.maven.surefire.common.junit4;

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

import junit.framework.TestCase;
import org.apache.maven.surefire.util.internal.ClassMethod;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.*;

/**
 * @author Qingzhou Luo
 */
public class JUnit4ProviderUtilTest
    extends TestCase
{
    public void testGenerateFailingTestDescriptions()
    {
        List<Failure> failures = new ArrayList<>();

        Description test1Description = Description.createTestDescription( T1.class, "testOne" );
        Description test2Description = Description.createTestDescription( T1.class, "testTwo" );
        Description test3Description = Description.createTestDescription( T2.class, "testThree" );
        Description test4Description = Description.createTestDescription( T2.class, "testFour" );
        Description test5Description = Description.createSuiteDescription( "Test mechanism" );

        failures.add( new Failure( test1Description, new AssertionError() ) );
        failures.add( new Failure( test2Description, new AssertionError() ) );
        failures.add( new Failure( test3Description, new RuntimeException() ) );
        failures.add( new Failure( test4Description, new AssertionError() ) );
        failures.add( new Failure( test5Description, new RuntimeException() ) );

        Set<Description> result = generateFailingTestDescriptions( failures );

        assertEquals( 4, result.size() );

        assertTrue( result.contains( test1Description) );
        assertTrue( result.contains( test2Description) );
        assertTrue( result.contains( test3Description) );
        assertTrue( result.contains( test4Description) );
    }

    public void testIllegalTestDescription$NegativeTest()
    {
        Description test = Description.createSuiteDescription( "someTestMethod" );
        ClassMethod classMethod = JUnit4ProviderUtil.toClassMethod( test );
        assertFalse( classMethod.isValidTest() );
    }

    public void testOldJUnitParameterizedDescriptionParser()
    {
        Description test = Description.createTestDescription( T1.class, " \n testMethod[5] " );
        assertEquals( " \n testMethod[5] (" + T1.class.getName() + ")", test.getDisplayName() );
        ClassMethod classMethod = JUnit4ProviderUtil.toClassMethod( test );
        assertTrue( classMethod.isValidTest() );
        assertEquals( " \n testMethod[5] ", classMethod.getMethod() );
        assertEquals( T1.class.getName(), classMethod.getClazz() );
    }

    public void testNewJUnitParameterizedDescriptionParser()
    {
        Description test = Description.createTestDescription( T1.class, "flakyTest[3: (Test11); Test12; Test13;]" );
        ClassMethod classMethod = JUnit4ProviderUtil.toClassMethod( test );
        assertTrue( classMethod.isValidTest() );
        assertEquals( "flakyTest[3: (Test11); Test12; Test13;]", classMethod.getMethod() );
        assertEquals( T1.class.getName(), classMethod.getClazz() );
    }

    private static class T1
    {

    }

    private static class T2
    {

    }
}
