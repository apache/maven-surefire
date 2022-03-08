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

import java.util.ArrayList;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireVerifierException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.fail;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1158">SUREFIRE-1158</a>
 * @since 2.19
 */
@RunWith( Parameterized.class )
public class Surefire1158RemoveInfoLinesIT extends SurefireJUnit4IntegrationTestCase
{

    @Parameters( name = "{0}" )
    public static Iterable<Object[]> data()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] {"junit-option-ff", "JUnitTest", "-ff", "surefire-junit47", false, true} );
        args.add( new Object[] {"testng-option-ff", "TestNGSuiteTest", "-ff", "surefire-testng", false, false} );
        args.add( new Object[] {"junit-option-X", "JUnitTest", "-X", "surefire-junit47", true, true} );
        args.add( new Object[] {"testng-option-X", "TestNGSuiteTest", "-X", "surefire-testng", true, false} );
        args.add( new Object[] {"junit-option-e", "JUnitTest", "-e", "surefire-junit47", true, true} );
        args.add( new Object[] {"testng-option-e", "TestNGSuiteTest", "-e", "surefire-testng", true, false} );
        return args;
    }

    @Parameter( 0 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String description;

    @Parameter( 1 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String testToRun;

    @Parameter( 2 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String cliOption;

    @Parameter( 3 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String provider;

    @Parameter( 4 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public boolean printsInfoLines;

    @Parameter( 5 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public boolean isJUnit;

    @Test
    public void shouldRunWithCliOption() throws Exception
    {
        OutputValidator validator = assertTest();
        if ( isJUnit )
        {
            assertJUnitTestLogs( validator );
        }
        else
        {
            assertTestNGTestLogs( validator );
        }
    }

    private OutputValidator assertTest() throws Exception
    {
        final String[] cli = {"--batch-mode"};
        return unpack( "/surefire-1158-remove-info-lines", "_" + description, cli )
            .sysProp( "provider", provider )
            .addGoal( cliOption )
            .setTestToRun( testToRun )
            .executeTest()
            .verifyErrorFreeLog()
            .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    private void assertJUnitTestLogs( OutputValidator validator )
    {
        try
        {
            validator.verifyTextInLog( "Surefire report directory:" );
            validator.verifyTextInLog(
                    "Using configured provider org.apache.maven.surefire.junitcore.JUnitCoreProvider" );
            validator.verifyTextInLog( "parallel='none', perCoreThreadCount=true, threadCount=0, "
                    + "useUnlimitedThreads=false, threadCountSuites=0, threadCountClasses=0, "
                    + "threadCountMethods=0, parallelOptimized=true" );
            if ( !printsInfoLines )
            {
                fail();
            }
        }
        catch ( SurefireVerifierException e )
        {
            if ( printsInfoLines )
            {
                fail();
            }
        }
    }

    private void assertTestNGTestLogs( OutputValidator validator )
    {
        try
        {
            validator.verifyTextInLog( "Surefire report directory:" );
            validator.verifyTextInLog( "Using configured provider org.apache.maven.surefire.testng.TestNGProvider" );
            validator.verifyTextInLog( "Configuring TestNG with: TestNGMapConfigurator" );
            if ( !printsInfoLines )
            {
                fail();
            }
        }
        catch ( SurefireVerifierException e )
        {
            if ( printsInfoLines )
            {
                fail();
            }
        }
    }
}
