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
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Basic suite test using all known versions of JUnit 4.x
 *
 * @author Kristian Rosenvold
 */
public class ConsoleOutputIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void properNewlinesAndEncodingWithDefaultEncodings()
    {
        final OutputValidator outputValidator =
            unpack( "/consoleOutput" ).forkOnce().executeTest();

        validate( outputValidator, true );
    }

    @Test
    public void properNewlinesAndEncodingWithDifferentEncoding()
    {
        final OutputValidator outputValidator =
            unpack( "/consoleOutput" ).forkOnce().argLine( "-Dfile.encoding=UTF-16" ).executeTest();

        validate( outputValidator, true );
    }

    @Test
    public void properNewlinesAndEncodingWithoutFork()
    {
        final OutputValidator outputValidator =
            unpack( "/consoleOutput" ).forkNever().executeTest();

        validate( outputValidator, false );
    }

    private void validate( final OutputValidator outputValidator, boolean includeShutdownHook )
    {
        TestFile xmlReportFile = outputValidator.getSurefireReportsXmlFile( "TEST-consoleOutput.Test1.xml" );
        xmlReportFile.assertContainsText( "SoutLine" );
        xmlReportFile.assertContainsText(  "äöüß" );
        xmlReportFile.assertContainsText(  "failing with ü" );

        TestFile outputFile = outputValidator.getSurefireReportsFile( "consoleOutput.Test1-output.txt", UTF_8 );
        outputFile.assertContainsText( "SoutAgain" );
        outputFile.assertContainsText( "SoutLine" );
        outputFile.assertContainsText( "äöüß" );

        if ( includeShutdownHook )
        {
            outputFile.assertContainsText( "Printline in shutdown hook" );
        }
    }

    @Test
    public void largerSoutThanMemory()
    {
        unpack( "consoleoutput-noisy" )
                .setMavenOpts( "-Xmx64m" )
                .sysProp( "thousand", "32000" )
                .executeTest();
    }
}
