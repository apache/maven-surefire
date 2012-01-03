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

import java.io.File;
import org.apache.maven.it.VerificationException;

/**
 * Test running a single test with -Dtest=BasicTest
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class CheckSingleTestIT
    extends SurefireVerifierTestClass
{

    public CheckSingleTestIT()
    {
        super( "/default-configuration" );
    }

    public void testSingleTest()
        throws Exception
    {
        addD( "test", "BasicTest"  );
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 1, 0, 0, 0 );
    }

    public void testSingleTestDotJava()
        throws Exception
    {
        addD( "test", "BasicTest.java"  );
        executeTest();
        verifyErrorFreeLog();
        assertTestSuiteResults( 1, 0, 0, 0 );
    }

    public void testSingleTestNonExistent()
        throws Exception
    {
        addD( "test", "DoesNotExist"  );
        try
        {
            executeTest();
            verifyErrorFreeLog();
            fail( "Build should have failed" );
        }
        catch ( VerificationException e )
        {
            // as expected
        }

        File reportsDir = getTargetFile( "surefire-reports" );
        assertFalse( "Unexpected reports directory", reportsDir.exists() );
    }

    public void testSingleTestNonExistentOverride()
        throws Exception
    {
        addD( "test", "DoesNotExist.java"  );
        failIfNoTests(  false );
        executeTest();
        verifyErrorFreeLog();

        File reportsDir = getTargetFile( "surefire-reports" );
        assertFalse( "Unexpected reports directory", reportsDir.exists() );
    }
}
