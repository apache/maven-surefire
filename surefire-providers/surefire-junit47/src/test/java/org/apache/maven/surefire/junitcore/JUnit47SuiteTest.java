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
import org.apache.maven.surefire.junitcore.pc.OptimizedParallelComputerTest;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilderTest;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerUtilTest;
import org.apache.maven.surefire.junitcore.pc.SchedulingStrategiesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Adapt the JUnit47 tests which use only annotations to the JUnit3 test suite.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
@Suite.SuiteClasses( {
    Surefire746Test.class,
    Surefire813IncorrectResultTest.class,
    ParallelComputerUtilTest.class,
    ParallelComputerBuilderTest.class,
    SchedulingStrategiesTest.class,
    JUnitCoreParametersTest.class,
    OptimizedParallelComputerTest.class,
    ConcurrentRunListenerTest.class,
    ConfigurableParallelComputerTest.class,
    JUnit4Reflector481Test.class,
    JUnitCoreParametersTest.class,
    JUnitCoreRunListenerTest.class,
    MavenSurefireJUnit47RunnerTest.class,
    MavenSurefireJUnit48RunnerTest.class,
    TestMethodTest.class
} )
@RunWith( Suite.class )
public class JUnit47SuiteTest
{
    public static Test suite()
    {
        return new JUnit4TestAdapter( JUnit47SuiteTest.class );
    }
}
