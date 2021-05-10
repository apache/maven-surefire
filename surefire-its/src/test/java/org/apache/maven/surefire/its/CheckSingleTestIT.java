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
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Test running a single test with -Dtest=BasicTest
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckSingleTestIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void singleTest()
    {
        unpack().setTestToRun( "BasicTest" ).executeTest().verifyErrorFree( 1 );
    }

    @Test
    public void singleTestDotJava()
    {
        unpack().setTestToRun( "BasicTest.java" ).executeTest().verifyErrorFree( 1 );
    }

    @Test
    public void singleTestNonExistent()
    {
        final OutputValidator output = unpack().setTestToRun( "DoesNotExist" ).maven().withFailure().executeTest();
        TestFile reportsDir = output.getTargetFile( "surefire-reports" );
        assertFalse( "Unexpected reports directory", reportsDir.exists() );
    }

    @Test
    public void singleTestNonExistentOverride()
    {
        final OutputValidator output =
            unpack().setTestToRun( "DoesNotExist" ).failIfNoSpecifiedTests( false ).executeTest().verifyErrorFreeLog();
        output.getTargetFile( "surefire-reports" );
        //   assertFalse( "Unexpected reports directory", reportsDir.exists() ); Hmpf. Not really a good test
    }

    private SurefireLauncher unpack()
    {
        return unpack( "/default-configuration" );
    }

}
