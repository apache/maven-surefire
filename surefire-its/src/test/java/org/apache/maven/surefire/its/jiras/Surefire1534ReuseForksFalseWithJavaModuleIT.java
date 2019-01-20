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

import org.apache.maven.surefire.its.AbstractJigsawIT;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

public class Surefire1534ReuseForksFalseWithJavaModuleIT
        extends AbstractJigsawIT
{

    @Test
    public void testExecuteWithReuseForksFalseWithJavaModule()
            throws IOException
    {
        OutputValidator validator = assumeJigsaw()
                .reuseForks( false )
                .forkCount( 1 )
                .executeTest();

        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyErrorFreeLog();

        TestFile report = validator.getSurefireReportsFile( "TEST-MainTest.xml", UTF_8 );
        assertTrue( report.exists() );
        report.assertContainsText( "<property name=\"reuseForks\" value=\"false\"/>" )
                .assertContainsText( "<property name=\"forkCount\" value=\"1\"/>" )
                .assertContainsText( "<testcase name=\"test1\"" )
                .assertContainsText( "<testcase name=\"test2\"" );
    }

    @Test
    public void testExecuteWithReuseForksFalseWithJavaModuleWithFilter()
            throws IOException
    {
        OutputValidator validator = assumeJigsaw()
                .reuseForks( false )
                .forkCount( 1 )
                .setTestToRun( "MainTest" )
                .executeTest();

        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyErrorFreeLog();

        TestFile report = validator.getSurefireReportsFile( "TEST-MainTest.xml", UTF_8 );
        assertTrue( report.exists() );
        report.assertContainsText( "<property name=\"reuseForks\" value=\"false\"/>" )
                .assertContainsText( "<property name=\"forkCount\" value=\"1\"/>" )
                .assertContainsText( "<testcase name=\"test1\"" )
                .assertContainsText( "<testcase name=\"test2\"" );
    }

    @Test
    public void testExecuteWithZeroForkCountWithJavaModule()
            throws IOException
    {
        OutputValidator validator = assumeJigsaw()
                .forkCount( 0 )
                .executeTest();

        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyErrorFreeLog();

        TestFile report = validator.getSurefireReportsFile( "TEST-MainTest.xml", UTF_8 );
        assertTrue( report.exists() );
        report.assertContainsText( "<property name=\"forkCount\" value=\"0\"/>" )
                .assertContainsText( "<testcase name=\"test1\"" )
                .assertContainsText( "<testcase name=\"test2\"" );
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "surefire-1534-reuse-forks-false-java-module";
    }
}
