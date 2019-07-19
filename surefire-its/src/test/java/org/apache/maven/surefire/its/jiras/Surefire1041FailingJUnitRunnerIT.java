package org.apache.maven.surefire.its.jiras;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.FileOutputStream;
import java.io.PrintWriter;

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
import org.apache.maven.surefire.its.fixture.SurefireVerifierException;
import org.junit.Test;

/**
 * SUREFIRE-1041: An error in a JUnit runner should not lead to an error in Surefire
 *
 * @author Andreas Gudian
 */
public class Surefire1041FailingJUnitRunnerIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void reportErrorInJUnitRunnerAsTestError()
    {
        try
        {
            unpack( "surefire-1041-exception-in-junit-runner" ).forkOnce().mavenTestFailureIgnore( true ).executeTest();
            fail( "SurefireVerifierException was expected." );
        } catch (SurefireVerifierException e) {
            assertThat( e.getMessage(), containsString( "There are test failures." ) );
        }
    }
}
