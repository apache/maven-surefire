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

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class JUnit47RedirectOutputIT
    extends SurefireVerifierTestClass
{
    public JUnit47RedirectOutputIT()
    {
        super("/junit47-redirect-output");
    }

    public void testPrintSummaryTrueWithRedirect()
        throws Exception
    {
        redirectToFile( true );

        addGoal( "clean" );
        executeTest();
        checkReports();
    }


    public void testClassesParallel()
        throws Exception
    {
        redirectToFile( true );

        addGoal( "clean" );
        addGoal( "-Dparallel=classes" );
        executeTest(  );
        checkReports();
    }

    private void checkReports()
        throws IOException
    {
        String report = StringUtils.trimToNull(
            FileUtils.readFileToString( getSurefireReportsFile( "junit47ConsoleOutput.Test1-output.txt" ) ) );
        assertNotNull( report );
        String report2 = StringUtils.trimToNull( FileUtils.readFileToString(
            getSurefireReportsFile( "junit47ConsoleOutput.Test2-output.txt" ) ) );
        assertNotNull(report2);
        assertFalse( getSurefireReportsFile("junit47ConsoleOutput.Test3-output.txt").exists());
    }

}
