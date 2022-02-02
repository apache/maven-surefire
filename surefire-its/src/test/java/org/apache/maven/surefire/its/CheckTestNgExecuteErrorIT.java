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
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test for checking that the output from a forked suite is properly captured even if the suite encounters
 * a severe error.
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestNgExecuteErrorIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void executionError()
    {
        OutputValidator outputValidator = unpack( "/testng-execute-error" )
                .maven()
                .sysProp( "testNgVersion", "5.7" )
                .sysProp( "testNgClassifier", "jdk15" )
                .showErrorStackTraces()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "Cyclic graph of methods" )
                .verifyTextInLog( "at org.apache.maven.surefire.testng.TestNGExecutor.run" );

        File reportDir = outputValidator.getSurefireReportsDirectory();
        String[] dumpFiles = reportDir.list( new FilenameFilter()
                                             {
                                                 @Override
                                                 public boolean accept( File dir, String name )
                                                 {
                                                     return name.endsWith( "-jvmRun1.dump" );
                                                 }
                                             } );

        assertThat( dumpFiles )
                .isNotNull()
                .isNotEmpty();

        for ( String dump : requireNonNull( dumpFiles ) )
        {
            outputValidator.getSurefireReportsFile( dump )
                    .assertContainsText( "Cyclic graph of methods" );
        }
    }
}
