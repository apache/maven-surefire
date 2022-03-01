package org.apache.maven.surefire.junitcore;

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

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SimpleReportEntry;

import junit.framework.TestCase;

import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;

/**
 * @author Kristian Rosenvold
 */
public class TestMethodTest
    extends TestCase
{

    public static final int BIG_UPTIME = 100_000;

    public void testTestFailure()
    {
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 0L, "a", null, "b", null );
        TestSet testSet = new TestSet( TestMethodTest.class.getName(), NORMAL_RUN, null );
        TestMethod testMethod = new TestMethod( reportEntry, testSet );
        testMethod.testFailure( reportEntry );
        final int elapsed = testMethod.getElapsed();
        assertTrue( elapsed >= 0 );
        assertTrue( elapsed < BIG_UPTIME );
    }
}
