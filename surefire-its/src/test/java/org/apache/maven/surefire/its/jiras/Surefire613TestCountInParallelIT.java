package org.apache.maven.surefire.its.jiras;

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
import org.junit.Test;

/**
 * SUREFIRE-613 Asserts proper test counts when running in parallel
 *
 * @author Kristian Rosenvold
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire613TestCountInParallelIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testParallelBuildResultCount()
    {
        OutputValidator validator = unpack( "/surefire-613-testCount-in-parallel" ).failNever().executeTest();

        validator.verifyTextInLog( "testAllok to stdout" );
        validator.verifyTextInLog( "testAllok to stderr" );
        validator.verifyTextInLog( "testWithException1 to stdout" );
        validator.verifyTextInLog( "testWithException1 to stderr" );
        validator.assertTestSuiteResults( 30, 8, 4, 17 );
    }
}
