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

/**
 * Asserts proper behaviour of console output when forking
 * SUREFIRE-639
 * SUREFIRE-651
 *
 * @author Kristian Rosenvold
 */
public class ForkConsoleOutputWithErrorsIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void xmlFileContainsConsoleOutput()
    {
        final OutputValidator outputValidator = unpack( "/fork-consoleOutputWithErrors" ).
            failNever().redirectToFile( true ).executeTest();
        final TestFile surefireReportsFile =
            outputValidator.getSurefireReportsXmlFile( "TEST-forkConsoleOutput.Test2.xml" );
        surefireReportsFile.assertContainsText( "sout: Will Fail soon" );
        surefireReportsFile.assertContainsText( "serr: Will Fail now" );
    }
}
