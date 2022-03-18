package org.apache.maven.surefire.junit4;

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

import org.apache.maven.surefire.api.booter.BaseProviderFactory;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.junit.Ignore;
import org.junit.Test;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Aslak Hellesøy
 */
public class JUnit4ProviderOrderingTest
{
    /**
     */
    public static class TestX
    {
        @Test
        public void testA()
        {
        }

        @Test
        public void testB()
        {
        }

        @Test
        public void testC()
        {
        }
    }

    /**
     */
    public static class TestY
    {
        @Test
        public void testD()
        {
        }

        @Test
        public void testE()
        {
        }

        @Test
        public void testF()
        {
        }

    }

    @Test
    public void testShouldOrderWithinSingleTestClass() throws TestSetFailedException
    {
        assertTestOrder(
            TestX.class.getName() + "#testC",
            TestX.class.getName() + "#testA",
            TestX.class.getName() + "#testB"
        );
    }

    @Test
    @Ignore( "this is currently not possible" )
    public void testShouldOrderWithinInterleavedTestClasses() throws TestSetFailedException
    {
        assertTestOrder(
            TestX.class.getName() + "#testC",
            TestY.class.getName() + "#testE",
            TestX.class.getName() + "#testA",
            TestY.class.getName() + "#testD",
            TestX.class.getName() + "#testB",
            TestY.class.getName() + "#testF"
        );
    }

    private void assertTestOrder( String... tests ) throws TestSetFailedException
    {
        List<String> testOrder = asList( tests );
        BaseProviderFactory providerParameters = new BaseProviderFactory( true );
        providerParameters.setProviderProperties( new HashMap<>() );
        providerParameters.setClassLoaders( getClass().getClassLoader() );
        TestListResolver requestedTests = new TestListResolver( String.join( ",", testOrder ) );
        TestRequest testRequest = new TestRequest( null, null, requestedTests );
        providerParameters.setTestRequest( testRequest );
        providerParameters.setRunOrderParameters( RunOrderParameters.alphabetical() );

        MockReporter testReportListener = new MockReporter();
        providerParameters.setReporterFactory( new StubReporterFactory( testReportListener ) );
        JUnit4Provider provider = new JUnit4Provider( providerParameters );
        TestsToRun testsToRun = new TestsToRun( new HashSet<>( asList( TestX.class, TestY.class ) ) );
        provider.invoke( testsToRun );

        List<String> actualOrder = testReportListener.getReports().stream()
            .map( r -> String.format( "%s#%s", r.getSourceName(), r.getName() ) )
            .collect( Collectors.toList() );

        assertThat( actualOrder, is( testOrder ) );
    }

    private static class StubReporterFactory implements ReporterFactory
    {
        private final TestReportListener<TestOutputReportEntry> testReportListener;

        private StubReporterFactory( TestReportListener<TestOutputReportEntry> testReportListener )
        {
            this.testReportListener = testReportListener;
        }

        @Override
        public TestReportListener<TestOutputReportEntry> createTestReportListener()
        {
            return testReportListener;
        }

        @Override
        public RunResult close()
        {
            return null;
        }
    }
}
