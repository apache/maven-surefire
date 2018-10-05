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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class JUnit47RedirectOutputIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testPrintSummaryTrueWithRedirect()
        throws Exception
    {
        final OutputValidator clean = unpack().redirectToFile( true ).addGoal( "clean" ).executeTest();
        checkReports( clean );
    }

    @Test
    public void testClassesParallel()
        throws Exception
    {
        final OutputValidator clean =
            unpack().redirectToFile( true ).parallelClasses().addGoal( "clean" ).executeTest();
        checkReports( clean );
    }

    private void checkReports( OutputValidator validator )
        throws IOException
    {
        String report = StringUtils.trimToNull(
            validator.getSurefireReportsFile( "junit47ConsoleOutput.Test1-output.txt" ).readFileToString() );
        assertNotNull( report );
        String report2 = StringUtils.trimToNull(
            validator.getSurefireReportsFile( "junit47ConsoleOutput.Test2-output.txt" ).readFileToString() );
        assertNotNull( report2 );
        assertFalse( validator.getSurefireReportsFile( "junit47ConsoleOutput.Test3-output.txt" ).exists() );
    }


    private SurefireLauncher unpack()
    {
        return unpack( "/junit47-redirect-output" );
    }

}
