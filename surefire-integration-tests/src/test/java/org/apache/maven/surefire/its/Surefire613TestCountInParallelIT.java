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


/**
 * SUREFIRE-613 Asserts proper test counts when running in parallel
 *
 * @author Kristian Rosenvold
 */
public class Surefire613TestCountInParallelIT
    extends SurefireVerifierTestClass
{

    public Surefire613TestCountInParallelIT()
    {
        super( "/surefire-613-testCount-in-parallel" );
    }

    public void testPaallelBuildResultCount()
        throws Exception
    {
        failNever();

        execute( "test" );

        verifyTextInLog( "testAllok to stdout" );
        verifyTextInLog( "testAllok to stderr" );
        verifyTextInLog( "testWithException1 to stdout" );
        verifyTextInLog( "testWithException1 to stderr" );

        assertTestSuiteResults( 15, 8, 4, 2 );
    }
}