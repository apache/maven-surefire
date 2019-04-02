package org.apache.maven.plugin.surefire;
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

import junit.framework.TestCase;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.suite.RunResult;

import static org.fest.assertions.Assertions.assertThat;

public class SurefirePluginTest extends TestCase
{
    public void testDefaultIncludes()
    {
        assertThat( new SurefirePlugin().getDefaultIncludes() )
                .containsOnly( "**/Test*.java", "**/*Test.java", "**/*Tests.java", "**/*TestCase.java" );
    }

    public void testReportSchemaLocation()
    {
        assertThat( new SurefirePlugin().getReportSchemaLocation() )
            .isEqualTo( "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd" );
    }

    public void testFailIfNoTests() throws Exception
    {
        RunResult runResult = new RunResult( 0, 0, 0, 0 );
        try
        {
            SurefirePlugin plugin = new SurefirePlugin();
            plugin.setFailIfNoTests( true );
            plugin.handleSummary( runResult, null );
        }
        catch ( MojoFailureException e )
        {
            assertThat( e.getLocalizedMessage() )
                    .isEqualTo( "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
            return;
        }
        fail( "Expected MojoFailureException with message "
                + "'No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)'" );
    }

    public void testTestFailure() throws Exception
    {
        RunResult runResult = new RunResult( 1, 0, 1, 0 );
        try
        {
            SurefirePlugin plugin = new SurefirePlugin();
            plugin.handleSummary( runResult, null );
        }
        catch ( MojoFailureException e )
        {
            assertThat( e.getLocalizedMessage() )
                    .isEqualTo( "There are test failures.\n\nPlease refer to null "
                            + "for the individual test results.\nPlease refer to dump files (if any exist) "
                            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream." );
            return;
        }
        fail( "Expected MojoFailureException with message "
                + "'There are test failures.\n\nPlease refer to null "
                + "for the individual test results.\nPlease refer to dump files (if any exist) "
                + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream.'");
    }

    public void testPluginName()
    {
        assertThat( new SurefirePlugin().getPluginName() )
                .isEqualTo( "surefire" );
    }
}
