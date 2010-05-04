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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test for checking that the output from a forked suite is properly captured even if the suite encounters a severe error.
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class CheckTestNgExecuteErrorIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testExecuteError()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-execute-error" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        try
        {
            this.executeGoal( verifier, "test" );
        }
        catch ( VerificationException e )
        {
        } // expected 

        verifier.resetStreams();
        assertTrue( new File( testDir, "target/surefire-reports/TestSuite-output.txt" ).length() > 0 );
    }
}
