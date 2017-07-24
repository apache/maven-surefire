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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Asserts proper behaviour of console output when forking
 * SUREFIRE-639
 * SUREFIRE-651
 *
 * @author Kristian Rosenvold
 */
public class ForkConsoleOutputIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void printSummaryTrueWithRedirect()
    {
        unpack().setForkJvm()
                .redirectToFile( true )
                .printSummary( true )
                .executeTest()
                .getSurefireReportsFile( "forkConsoleOutput.Test1-output.txt" )
                .assertFileExists();
    }

    @Test
    public void printSummaryTrueWithoutRedirect()
    {
        unpack().setForkJvm()
                .redirectToFile( false )
                .printSummary( true )
                .executeTest()
                .getSurefireReportsFile( "forkConsoleOutput.Test1-output.txt" )
                .assertFileNotExists();
    }

    @Test
    public void printSummaryFalseWithRedirect()
    {
        unpack().setForkJvm()
                .redirectToFile( true )
                .printSummary( false )
                .debugLogging()
                .showErrorStackTraces()
                .executeTest()
                .getSurefireReportsFile( "forkConsoleOutput.Test1-output.txt" )
                .assertFileExists();
    }

    @Test
    public void printSummaryFalseWithoutRedirect()
    {
        unpack().setForkJvm()
                .redirectToFile( false )
                .printSummary( false )
                .executeTest()
                .getSurefireReportsFile( "forkConsoleOutput.Test1-output.txt" )
                .assertFileNotExists();
    }


    private SurefireLauncher unpack()
    {
        return unpack( "/fork-consoleOutput" );
    }
}
