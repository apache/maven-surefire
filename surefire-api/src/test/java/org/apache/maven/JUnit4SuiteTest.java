package org.apache.maven;

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
import org.apache.maven.plugin.surefire.runorder.ThreadedExecutionSchedulerTest;
import org.apache.maven.surefire.SpecificTestClassFilterTest;
import org.apache.maven.surefire.booter.ForkingRunListenerTest;
import org.apache.maven.surefire.booter.MasterProcessCommandTest;
import org.apache.maven.surefire.booter.SurefireReflectorTest;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriterTest;
import org.apache.maven.surefire.suite.RunResultTest;
import org.apache.maven.surefire.testset.FundamentalFilterTest;
import org.apache.maven.surefire.testset.ResolvedTestTest;
import org.apache.maven.surefire.testset.TestListResolverTest;
import org.apache.maven.surefire.util.DefaultDirectoryScannerTest;
import org.apache.maven.surefire.util.ReflectionUtilsTest;
import org.apache.maven.surefire.util.RunOrderCalculatorTest;
import org.apache.maven.surefire.util.RunOrderTest;
import org.apache.maven.surefire.util.ScanResultTest;
import org.apache.maven.surefire.util.TestsToRunTest;
import org.apache.maven.surefire.util.UrlUtilsTest;
import org.apache.maven.surefire.util.internal.ConcurrencyUtilsTest;
import org.apache.maven.surefire.util.internal.ImmutableMapTest;
import org.apache.maven.surefire.util.internal.StringUtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Adapt the JUnit4 tests which use only annotations to the JUnit3 test suite.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.19
 */
@Suite.SuiteClasses( {
    ThreadedExecutionSchedulerTest.class,
    ForkingRunListenerTest.class,
    MasterProcessCommandTest.class,
    SurefireReflectorTest.class,
    LegacyPojoStackTraceWriterTest.class,
    RunResultTest.class,
    ResolvedTestTest.class,
    TestListResolverTest.class,
    ConcurrencyUtilsTest.class,
    StringUtilsTest.class,
    DefaultDirectoryScannerTest.class,
    RunOrderCalculatorTest.class,
    RunOrderTest.class,
    ScanResultTest.class,
    TestsToRunTest.class,
    UrlUtilsTest.class,
    SpecificTestClassFilterTest.class,
    FundamentalFilterTest.class,
    ImmutableMapTest.class,
    ReflectionUtilsTest.class
} )
@RunWith( Suite.class )
public class JUnit4SuiteTest
{
    public static Test suite()
    {
        return new JUnit4TestAdapter( JUnit4SuiteTest.class );
    }
}
