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

import java.util.HashMap;
import org.apache.maven.surefire.junit4.MockReporter;

import junit.framework.TestCase;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

/**
 * @author Kristian Rosenvold
 */
public class JUnitCoreRunListenerTest
    extends TestCase
{
    public void testTestRunStarted()
        throws Exception
    {
        RunListener jUnit4TestSetReporter =
            new JUnitCoreRunListener( new MockReporter(), new HashMap<String, TestSet>() );
        JUnitCore core = new JUnitCore();
        core.addListener( jUnit4TestSetReporter );
        Result result = core.run( new Computer(), STest1.class, STest2.class );
        core.removeListener( jUnit4TestSetReporter );
        assertEquals( 2, result.getRunCount() );
    }

    public void testFailedAssumption()
        throws Exception
    {
        RunListener jUnit4TestSetReporter =
            new JUnitCoreRunListener( new MockReporter(), new HashMap<String, TestSet>() );
        JUnitCore core = new JUnitCore();
        core.addListener( jUnit4TestSetReporter );
        Result result = core.run( new Computer(), TestWithAssumptionFailure.class );
        core.removeListener( jUnit4TestSetReporter );
        assertEquals( 1, result.getRunCount() );
    }

    public static class STest1
    {
        @Test
        public void testSomething()
        {
        }
    }

    public static class STest2
    {
        @Test
        public void testSomething2()
        {
        }
    }

    public static class TestWithAssumptionFailure
    {
        @Test
        public void testSomething2()
        {
            Assume.assumeTrue( false );
        }
    }

}
