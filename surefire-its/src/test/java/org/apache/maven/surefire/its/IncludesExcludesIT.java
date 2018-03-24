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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Test;

/**
 * Test include/exclude patterns.
 *
 * @author Benjamin Bentmann
 */
public class IncludesExcludesIT
    extends SurefireJUnit4IntegrationTestCase
{
    private SurefireLauncher unpack()
    {
        return unpack( "/includes-excludes" );
    }

    /**
     * Test surefire inclusions/exclusions
     */
    @Test
    public void testIncludesExcludes()
    {
        testWithProfile( "simple" );
    }

    @Test
    public void testRegexIncludesExcludes()
    {
        testWithProfile( "regex" );
    }

    @Test
    public void testPathBasedIncludesExcludes()
    {
        testWithProfile( "path" );
    }

    private void testWithProfile( String profile )
    {
        final OutputValidator outputValidator = unpack().
            activateProfile( profile ).executeTest().verifyErrorFree( 2 );
        outputValidator.getTargetFile( "testTouchFile.txt" ).assertFileExists();
        outputValidator.getTargetFile( "defaultTestTouchFile.txt" ).assertFileExists();
    }
}
