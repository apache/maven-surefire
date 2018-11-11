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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Test project using -Dtest=mtClass#myMethod
 *
 * @author Olivier Lamy
 */
public class TestMethodPatternIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String RUNNING_WITH_PROVIDER47 = "parallel='none', perCoreThreadCount=true, threadCount=0";

    public OutputValidator runMethodPattern( String projectName, Map<String, String> props, String... goals )
    {
        SurefireLauncher launcher = unpack( projectName );
        for ( Entry<String, String> entry : props.entrySet() )
        {
            launcher.sysProp( entry.getKey(), entry.getValue() );
        }
        for ( String goal : goals )
        {
            launcher.addGoal( goal );
        }
        return launcher.showErrorStackTraces().debugLogging()
            .executeTest()
            .assertTestSuiteResults( 2, 0, 0, 0 );
    }

    @Test
    public void testJUnit44()
    {
        runMethodPattern( "junit44-method-pattern", Collections.<String, String>emptyMap() );
    }

    @Test
    public void testJUnit48Provider4()
    {
        runMethodPattern( "junit48-method-pattern", Collections.<String, String>emptyMap(), "-P surefire-junit4" );
    }

    @Test
    public void testJUnit48Provider47()
    {
        runMethodPattern( "junit48-method-pattern", Collections.<String, String>emptyMap(), "-P surefire-junit47" )
            .verifyTextInLog( RUNNING_WITH_PROVIDER47 );
    }

    @Test
    public void testJUnit48WithCategoryFilter()
    {
        unpack( "junit48-method-pattern" )
            .addGoal( "-Dgroups=junit4.SampleCategory" )
            .executeTest()
            .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test
    public void testTestNgMethodBefore()
    {
        Map<String, String> props = new HashMap<>();
        props.put( "testNgVersion", "5.7" );
        props.put( "testNgClassifier", "jdk15" );
        runMethodPattern( "testng-method-pattern-before", props );
    }

    @Test
    public void testTestNGMethodPattern()
    {
        Map<String, String> props = new HashMap<>();
        props.put( "testNgVersion", "5.7" );
        props.put( "testNgClassifier", "jdk15" );
        runMethodPattern( "/testng-method-pattern", props );
    }

    @Test
    public void testMethodPatternAfter()
    {
        unpack( "testng-method-pattern-after" )
                .sysProp( "testNgVersion", "5.7" )
                .sysProp( "testNgClassifier", "jdk15" )
                .executeTest()
                .verifyErrorFree( 2 )
                .verifyTextInLog( "Called tearDown" );
    }

}
