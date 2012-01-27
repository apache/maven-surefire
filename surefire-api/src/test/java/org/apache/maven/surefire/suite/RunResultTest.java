package org.apache.maven.surefire.suite;

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

/**
 * @author Kristian Rosenvold
 */
public class RunResultTest
    extends TestCase
{
    public void testGetAsString()
        throws Exception
    {

    }

    public void testFromString()
        throws Exception
    {
        RunResult original = new RunResult( 4, 3, 2, 1, true, false );
        final String asString = original.getAsString();
        final RunResult runResult = RunResult.fromString( asString );
        verifySame( original, runResult );
    }

    public void testFromString2()
        throws Exception
    {
        RunResult original = new RunResult( 5, 6, 7, 8, false, true );
        final String asString = original.getAsString();
        final RunResult runResult = RunResult.fromString( asString );
        verifySame( original, runResult );
    }

    private void verifySame( RunResult original, RunResult runResult )
    {
        assertEquals( original.getCompletedCount(), runResult.getCompletedCount() );
        assertEquals( original.getErrors(), runResult.getErrors() );
        assertEquals( original.getFailures(), runResult.getFailures() );
        assertEquals( original.getSkipped(), runResult.getSkipped() );
        assertEquals( original.isFailure(), runResult.isFailure() );
        assertEquals( original.isTimeout(), runResult.isTimeout() );
    }
}
