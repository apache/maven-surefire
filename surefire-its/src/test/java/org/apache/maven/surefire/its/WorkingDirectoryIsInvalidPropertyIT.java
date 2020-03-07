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
import org.junit.Test;

/**
 * Test when the configured working directory is an invalid property, SUREFIRE-715
 *
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class WorkingDirectoryIsInvalidPropertyIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testWorkingDirectory()
        throws Exception
    {
        unpack( "working-directory-is-invalid-property" ).maven().withFailure().executeTest().verifyTextInLog(
            "workingDirectory cannot be null" );
    }
}