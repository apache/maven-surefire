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

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.maven.surefire.junitcore.pc.OptimizedParallelComputerTest;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilderTest;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerUtilTest;
import org.apache.maven.surefire.junitcore.pc.SchedulingStrategiesTest;

/**
 * Adapt the JUnit47 tests which use only annotations to the JUnit3 test suite.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class JUnit47SuiteTest extends TestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite( ConcurrentRunListenerTest.class );
        suite.addTestSuite( ConfigurableParallelComputerTest.class );
        suite.addTestSuite( JUnitCoreRunListenerTest.class );
        suite.addTestSuite( MavenSurefireJUnit47RunnerTest.class );
        suite.addTestSuite( MavenSurefireJUnit48RunnerTest.class );
        suite.addTestSuite( TestMethodTest.class );
        suite.addTest( new JUnit4TestAdapter( Surefire746Test.class ) );
        suite.addTest( new JUnit4TestAdapter( Surefire813IncorrectResultTest.class ) );
        suite.addTest( new JUnit4TestAdapter( ParallelComputerUtilTest.class ) );
        suite.addTest( new JUnit4TestAdapter( ParallelComputerBuilderTest.class ) );
        suite.addTest( new JUnit4TestAdapter( SchedulingStrategiesTest.class ) );
        suite.addTest( new JUnit4TestAdapter( OptimizedParallelComputerTest.class ) );
        suite.addTest( new JUnit4TestAdapter( JUnit4Reflector481Test.class ) );
        suite.addTest( new JUnit4TestAdapter( JUnitCoreParametersTest.class ) );
        return suite;
    }
}
