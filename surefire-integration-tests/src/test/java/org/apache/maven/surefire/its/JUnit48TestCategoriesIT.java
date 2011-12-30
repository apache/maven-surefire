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

import org.apache.maven.it.VerificationException;

/**
 * Test project using "groups" support
 *
 * @author <a href="mailto:todd@apache.org">Todd Lipcon</a>
 */
public class JUnit48TestCategoriesIT
    extends SurefireVerifierTestClass
{
    public JUnit48TestCategoriesIT()
    {
        super( "/junit48-categories" );
    }

    public void testCategoriesAB()
        throws Exception
    {
        runAB();
    }

    public void testCategoriesABForkAlways()
        throws Exception
    {
        forkAlways();
        runAB();
    }

    private void runAB()
        throws VerificationException
    {
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 3, 0, 0, 0 );
        verifyTextInLog( "catA: 1" );
        verifyTextInLog( "catB: 1" );
        verifyTextInLog( "catC: 0" );
        verifyTextInLog( "catNone: 0" );
    }

    public void testCategoriesAC()
        throws Exception
    {
        runAC();
    }

    public void testCategoriesACForkAlways()
        throws Exception
    {
        forkAlways();
        runAC();
    }


    private void runAC()
        throws Exception
    {
        addGoal( "-Dgroups=junit4.CategoryA,junit4.CategoryC" );
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 6, 0, 0, 0 );
        verifyTextInLog( "catA: 1" );
        verifyTextInLog( "catB: 0" );
        verifyTextInLog( "catC: 1" );
        verifyTextInLog( "catNone: 0" );
        verifyTextInLog( "mA: 1" );
        verifyTextInLog( "mB: 1" ); // This seems questionable !? The class is annotated with category C and method with B
        verifyTextInLog( "mC: 1" );
        verifyTextInLog( "CatNone: 1" );
    }

}
