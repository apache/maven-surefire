package org.apache.maven.surefire.its.jiras;

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
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

/**
 * SUREFIRE-500 Asserts correct error handling for the "odd" surefire-500 (and 625) issues.
 *
 * @author Kristian Rosenvold
 */
public class Surefire500PuzzlingErrorIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testBuildFailingWhenErrors()
    {
        OutputValidator outputValidator = unpack( "/surefire-500-puzzling-error" ).failNever().executeTest();
        TestFile surefireReportsFile = outputValidator.getSurefireReportsFile( "surefire500.ExplodingTest.txt" );
        surefireReportsFile.assertContainsText( "java.lang.NoClassDefFoundError: whoops!" );
    }
}