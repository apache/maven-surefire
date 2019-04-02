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

import org.apache.maven.surefire.booter.BaseProviderFactory;
import org.apache.maven.surefire.testset.TestRequest;
import org.junit.Test;
import org.junit.runner.Description;

import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.runner.Description.createSuiteDescription;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4ProviderTest
{
    @Test
    public void testCreateProvider()
    {
        assertNotNull( getJUnit4Provider() );
    }

    private JUnit4Provider getJUnit4Provider()
    {
        BaseProviderFactory providerParameters = new BaseProviderFactory( null, true );
        providerParameters.setProviderProperties( new HashMap<String, String>() );
        providerParameters.setClassLoaders( getClass().getClassLoader() );
        providerParameters.setTestRequest( new TestRequest( null, null, null ) );
        return new JUnit4Provider( providerParameters );
    }

    @Test
    public void testShouldCreateDescription()
    {
        class A {
        }

        class B {
        }

        Description d = JUnit4Provider.createTestsDescription( asList( A.class, B.class ) );
        assertThat( d, is( notNullValue() ) );
        assertThat( d.getDisplayName(), not( isEmptyOrNullString() ) );
        assertThat( d.getDisplayName(), is( "null" ) );
        assertThat( d.getChildren(), hasSize( 2 ) );
        Description a = createSuiteDescription( A.class );
        Description b = createSuiteDescription( B.class );
        assertThat( d.getChildren(), contains( a, b ) );
    }

}
