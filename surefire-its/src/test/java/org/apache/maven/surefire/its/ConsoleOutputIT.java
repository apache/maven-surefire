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

import java.util.ArrayList;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Basic suite test using all known versions of JUnit 4.x
 *
 * @author Kristian Rosenvold
 */
@RunWith( Parameterized.class )
public class ConsoleOutputIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String LEGACY_FORK_NODE =
        "org.apache.maven.plugin.surefire.extensions.LegacyForkNodeFactory";

    private static final String SUREFIRE_FORK_NODE =
        "org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory";

    @Parameters
    public static Iterable<Object[]> data()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] { "tcp" } );
        args.add( new Object[] { null } );
        return args;
    }

    @Parameter
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String profileId;

    @Test
    public void properNewlinesAndEncodingWithDefaultEncodings() throws Exception
    {
        OutputValidator outputValidator = unpack().forkOnce().executeTest();
        validate( outputValidator, true );
    }

    @Test
    public void properNewlinesAndEncodingWithDifferentEncoding() throws Exception
    {
        OutputValidator outputValidator = unpack()
                .forkOnce()
                .argLine( "-Dfile.encoding=UTF-16" )
                .executeTest();
        validate( outputValidator, true );
    }

    @Test
    public void properNewlinesAndEncodingWithoutFork() throws Exception
    {
        OutputValidator outputValidator = unpack()
                .forkNever()
                .executeTest();
        validate( outputValidator, false );
    }

    private SurefireLauncher unpack()
    {
        SurefireLauncher launcher =
            unpack( "/consoleOutput", profileId == null ? "" : "-" + profileId )
                .debugLogging();

        if ( profileId != null )
        {
            launcher.activateProfile( profileId );
        }

        return launcher;
    }

    private void validate( final OutputValidator outputValidator, boolean canFork )
        throws Exception
    {
        TestFile xmlReportFile = outputValidator.getSurefireReportsXmlFile( "TEST-consoleOutput.Test1.xml" );
        xmlReportFile.assertContainsText( "SoutLine" );
        xmlReportFile.assertContainsText(  "äöüß" );
        xmlReportFile.assertContainsText(  "failing with ü" );

        TestFile outputFile = outputValidator.getSurefireReportsFile( "consoleOutput.Test1-output.txt", UTF_8 );
        outputFile.assertContainsText( "SoutAgain" );
        outputFile.assertContainsText( "SoutLine" );
        outputFile.assertContainsText( "äöüß" );

        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;

        if ( canFork )
        {
            outputValidator
                .assertThatLogLine(
                    containsString( "Found implementation of fork node factory: " + cls ),
                    equalTo( 1 ) );
        }
    }

    @Test
    public void largerSoutThanMemory() throws Exception
    {
        SurefireLauncher launcher =
            unpackNoisy()
                .setMavenOpts( "-Xmx64m" )
                .sysProp( "thousand", "32000" );

        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;

        launcher.executeTest()
            .verifyErrorFreeLog()
            .assertThatLogLine( containsString( "Found implementation of fork node factory: " + cls ), equalTo( 1 ) );
    }

    private SurefireLauncher unpackNoisy()
    {
        SurefireLauncher launcher =
            unpack( "consoleoutput-noisy", profileId == null ? "" : "-" + profileId )
                .debugLogging();

        if ( profileId != null )
        {
            launcher.activateProfile( profileId );
        }

        return launcher;
    }
}
