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

import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;

/**
 * Test project using -Dtest=mtClass#myMethod
 *
 * @author <a href="mailto:olamy@apache.org">Olivier Lamy</a>
 */
public class TestMethodPatternIT
    extends SurefireIntegrationTestCase
{
    public void runMethodPattern( String projectName )
    {
        unpack( projectName ).executeTest().assertTestSuiteResults( 2, 0, 0, 0 );
    }

    public void testJUnit44()
    {
        runMethodPattern( "junit44-method-pattern" );
    }

    public void testJUnit48()
    {
        runMethodPattern( "junit48-method-pattern" );
    }

    public void testTestNgMethodBefore()
    {
        runMethodPattern( "testng-method-pattern-before" );
    }

    public void testTestNGMethodPattern()
    {
        runMethodPattern( "/testng-method-pattern" );
    }

    public void testMethodPatternAfter()
    {
        unpack( "testng-method-pattern-after" ).executeTest().verifyErrorFree( 2 ).verifyTextInLog( "Called tearDown" );
    }

}
