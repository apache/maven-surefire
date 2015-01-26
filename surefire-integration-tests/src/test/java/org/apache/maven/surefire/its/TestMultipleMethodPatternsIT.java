package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;


/**
 * Test project using multiple method patterns, including wildcards in class and method names.
 */
public class TestMultipleMethodPatternsIT
    extends SurefireJUnit4IntegrationTestCase
{

    private static final String RUNNING_WITH_JUNIT48 = "parallel='none', perCoreThreadCount=true, threadCount=0";

    /*public OutputValidator multipleMethod( String projectName )
        throws Exception
    {
        return unpack( projectName ).executeTest().verifyErrorFreeLog().assertTestSuiteResults( 7, 0, 0, 0 );
    }*/

    public SurefireLauncher multipleMethod( String projectName )
        throws Exception
    {
        return unpack( projectName );
    }


    /*@Test
    public void testJunit48()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" ).verifyTextInLog( RUNNING_WITH_JUNIT48 );
    }*/

    @Test
    public void shouldMatchExactClassAndMethod()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "BasicTest#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void shouldMatchWildcardPackageAndExactClassAndMethod()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "jiras.**.BasicTest#testSuccessTwo" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void shouldMatchExactClassAndMethodWildcard()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .maven()
            .addGoal( "-e" ).addGoal( "-X" )
            .debugLogging().sysProp( "test", "BasicTest#test*One" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessOne" );
    }

    @Test
    public void shouldMatchExactClassAndMethodsWildcard()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "BasicTest#testSuccess*" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessTwo" );
    }

    @Test
    public void shouldMatchExactClassAndMethodCharacters()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "BasicTest#test???????One" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.BasicTest#testSuccessOne" );
    }

    @Test
    public void shouldMatchExactClassAndMethodsPostfix()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "TestFive#testSuccess???" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessTwo" );
    }

    @Test
    public void shouldMatchExactClassAndMethodPostfix()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "TestFive#testSuccess?????" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessThree" );
    }

    @Test
    public void shouldMatchExactClassAndMultipleMethods()
        throws Exception
    {
        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( "TestFive#testSuccessOne+testSuccessThree" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyErrorFreeLog()
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessOne" )
            .verifyTextInLog( "jiras.surefire745.TestFive#testSuccessThree" );
    }

    @Test
    public void shouldMatchMultiplePatterns()
        throws Exception
    {
        String test = "jiras.surefire745.BasicTest#testSuccessOne+testSuccessTwo"//2
                + ',' + "jiras.**.TestTwo"//2
                + ',' + "jiras.surefire745.TestThree#testSuccess*"//2
                + ',' + "jiras.surefire745.TestFour#testSuccess???"//2
                + ',' + "jiras.surefire745.*Five#test*One";//1

        multipleMethod( "junit48-multiple-method-patterns" )
            .setTestToRun( test )
            .executeTest()
            .verifyErrorFree( 9 )
            .verifyErrorFreeLog();
    }

}
