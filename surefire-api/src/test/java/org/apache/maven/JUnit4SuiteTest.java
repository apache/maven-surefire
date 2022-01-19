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
import org.apache.maven.surefire.api.runorder.ThreadedExecutionSchedulerTest;
import org.apache.maven.surefire.SpecificTestClassFilterTest;
import org.apache.maven.surefire.api.booter.ForkingRunListenerTest;
import org.apache.maven.surefire.api.report.LegacyPojoStackTraceWriterTest;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoderTest;
import org.apache.maven.surefire.api.stream.AbstractStreamEncoderTest;
import org.apache.maven.surefire.api.suite.RunResultTest;
import org.apache.maven.surefire.api.testset.FundamentalFilterTest;
import org.apache.maven.surefire.api.testset.ResolvedTestTest;
import org.apache.maven.surefire.api.testset.TestListResolverTest;
import org.apache.maven.surefire.api.util.DefaultDirectoryScannerTest;
import org.apache.maven.surefire.api.util.ReflectionUtilsTest;
import org.apache.maven.surefire.api.util.RunOrderCalculatorTest;
import org.apache.maven.surefire.api.util.RunOrderTest;
import org.apache.maven.surefire.api.util.ScanResultTest;
import org.apache.maven.surefire.api.util.TestsToRunTest;
import org.apache.maven.surefire.api.util.internal.AsyncSocketTest;
import org.apache.maven.surefire.api.util.internal.ChannelsReaderTest;
import org.apache.maven.surefire.api.util.internal.ChannelsWriterTest;
import org.apache.maven.surefire.api.util.internal.ConcurrencyUtilsTest;
import org.apache.maven.surefire.api.util.internal.ImmutableMapTest;
import org.apache.maven.surefire.api.util.internal.ObjectUtilsTest;
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
    LegacyPojoStackTraceWriterTest.class,
    RunResultTest.class,
    ResolvedTestTest.class,
    TestListResolverTest.class,
    ConcurrencyUtilsTest.class,
    DefaultDirectoryScannerTest.class,
    RunOrderCalculatorTest.class,
    RunOrderTest.class,
    ScanResultTest.class,
    TestsToRunTest.class,
    SpecificTestClassFilterTest.class,
    FundamentalFilterTest.class,
    ImmutableMapTest.class,
    ReflectionUtilsTest.class,
    ChannelsReaderTest.class,
    ChannelsWriterTest.class,
    AsyncSocketTest.class,
    AbstractStreamEncoderTest.class,
    AbstractStreamDecoderTest.class,
    ObjectUtilsTest.class
} )
@RunWith( Suite.class )
public class JUnit4SuiteTest
{
    public static Test suite()
    {
        return new JUnit4TestAdapter( JUnit4SuiteTest.class );
    }
}
