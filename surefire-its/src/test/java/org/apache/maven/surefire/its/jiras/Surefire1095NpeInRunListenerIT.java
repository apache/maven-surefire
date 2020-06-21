package org.apache.maven.surefire.its.jiras;

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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

@SuppressWarnings( { "javadoc", "checkstyle:javadoctype", "checkstyle:linelength" } )
/**
 *
 * In the surefire plugin, it is possible to specify one or more RunListener when running tests with JUnit.
 * However, it does not look like the listener is properly called by the plugin. In particular, there is a problem
 * with the method:
 * <pre>
 * public void testRunStarted(Description description)
 * </pre>
 * it's javadoc at
 * <a href="http://junit.sourceforge.net/javadoc/org/junit/runner/notification/RunListener.html#testRunStarted%28org.junit.runner.Description%29"/>
 * states:
 * "Parameters:
 * description - describes the tests to be run "
 * however, in all maven projects I tried ("mvn test"), the surefire plugin seems like passing a null reference instead
 * of a Description instance that "describes the tests to be run "
 * Note: other methods in the RunListener I tested seems fine (i.e., they get a valid Description object as input)
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1095}"/>
 * @since 2.18
 */
public final class Surefire1095NpeInRunListenerIT
    extends SurefireJUnit4IntegrationTestCase
{

    /**
     * Method Request.classes( String, Class[] ); exists in JUnit 4.0 - 4.4
     * See JUnit4Reflector.
     */
    @Test
    public void testRunStartedWithJUnit40()
    {
        unpack().setJUnitVersion( "4.0" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "Running JUnit 4.0" )
            .verifyTextInLog( "testRunStarted [jiras.surefire1095.SomeTest]" );
    }

    /**
     * Method Request.classes( Class[] ); Since of JUnit 4.5
     * See JUnit4Reflector.
     */
    @Test
    public void testRunStartedWithJUnit45()
    {
        unpack().setJUnitVersion( "4.5" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "Running JUnit 4.5" )
            .verifyTextInLog( "testRunStarted [jiras.surefire1095.SomeTest]" );
    }

    @Test
    public void testRunStartedWithJUnit47()
    {
        unpack().setJUnitVersion( "4.7" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "Running JUnit 4.7" )
            .verifyTextInLog( "testRunStarted [jiras.surefire1095.SomeTest]" );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1095-npe-in-runlistener" );
    }
}
