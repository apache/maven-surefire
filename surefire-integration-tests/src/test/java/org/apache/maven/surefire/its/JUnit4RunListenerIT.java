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

import org.apache.maven.reporting.MavenReportException;

import java.io.File;

/**
 * JUnit4 RunListener Integration Test.
 * 
 * @author <a href="mailto:matthew.gilliard@gmail.com">Matthew Gilliard</a>
 */
public class JUnit4RunListenerIT
    extends SurefireVerifierTestClass
{
    public JUnit4RunListenerIT()
    {
        super( "/junit4-runlistener" );
    }

    public void testJUnit4RunListener()
        throws Exception
    {
        addGoal( "-Dprovider=surefire-junit4" );
        addGoal( "-DjunitVersion=4.4" );
        executeTest();
        verifyErrorFreeLog();
        assertResults();
    }

    private void assertResults()
        throws MavenReportException
    {
        assertTestSuiteResults( 1, 0, 0, 0 );
        final File targetDir = getSubFile( "target" );

        assertFileExists( new File( targetDir, "runlistener-output-1.txt" ) );
        assertFileExists( new File( targetDir, "runlistener-output-2.txt" ) );
    }

    public void testRunlistenerJunitCoreProvider()
        throws Exception
    {
        addGoal( "-Dprovider=surefire-junit47" );
        addGoal( "-DjunitVersion=4.8.1" );
        executeTest();
        verifyErrorFreeLog();
        assertResults();
    }

    private void assertFileExists( final File file )
    {
        assertTrue( "File doesn't exist: " + file.getAbsolutePath(), file.exists() );
    }

}
