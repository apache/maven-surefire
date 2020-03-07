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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Test failIfNoTests
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class CheckTestFailIfNoTestsIT
    extends SurefireJUnit4IntegrationTestCase
{
    private SurefireLauncher unpack()
    {
        return unpack( "/default-configuration-noTests" );
    }

    @Test
    public void failIfNoTests()
    {
        unpack().failIfNoTests( true ).maven().withFailure().executeTest();
    }

    @Test
    public void dontFailIfNoTests()
    {
        final OutputValidator outputValidator = unpack().failIfNoTests( false ).executeTest();
        outputValidator.verifyErrorFreeLog();
        TestFile reportsDir = outputValidator.getSurefireReportsFile( "" );
        assertFalse( "Unexpected reports directory", reportsDir.exists() );
    }

    @Test
    public void jUnit48CategoriesFailWhenNoTests()
    {
        unpack().failIfNoTests( false ).activateProfile( "junit47" ).setJUnitVersion(
            "4.8.1" ).executeTest().verifyErrorFreeLog();
    }

}
