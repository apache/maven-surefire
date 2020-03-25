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

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;

@RunWith( Parameterized.class )
public class JUnitPlatformEnginesIT
        extends SurefireJUnit4IntegrationTestCase
{
    @Parameter
    public String platform;

    @Parameter( 1 )
    public String jupiter;

    @Parameter( 2 )
    public String opentest;

    @Parameter( 3 )
    public String apiguardian;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> versions()
    {
        ArrayList<Object[]> args = new ArrayList<Object[]>();
        args.add( new Object[] { "1.0.3", "5.0.3", "1.0.0", "1.0.0" } );
        args.add( new Object[] { "1.1.1", "5.1.1", "1.0.0", "1.0.0" } );
        args.add( new Object[] { "1.2.0", "5.2.0", "1.1.0", "1.0.0" } );
        args.add( new Object[] { "1.3.2", "5.3.2", "1.1.1", "1.0.0" } );
        args.add( new Object[] { "1.4.1", "5.4.1", "1.1.1", "1.0.0" } );
        args.add( new Object[] { "1.5.2", "5.5.2", "1.2.0", "1.1.0" } );
        args.add( new Object[] { "1.6.1", "5.6.1", "1.2.0", "1.1.0" } );
        return args;
    }

    @Before
    public void setUp()
    {
        assumeJavaVersion( 1.8d );
    }

    @Test
    public void platform() throws VerificationException
    {
        OutputValidator validator = unpack( "junit-platform", '-' + platform )
                .sysProp( "jupiter.version", jupiter )
                .sysProp( "platform.version", platform )
                .addGoal( "-X" )
                .executeTest()
                .verifyErrorFree( 1 );

        List<String> lines = validator.loadLogLines( startsWith( "[DEBUG] test(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        assertThat( lines.get( 0 ) )
                .startsWith( "[DEBUG] test(compact) classpath" )
                .contains( "classes" )
                .contains( "test-classes" )
                .contains( "apiguardian-api-" + apiguardian + ".jar" )
                .contains( "junit-jupiter-api-" + jupiter + ".jar" )
                .contains( "junit-jupiter-engine-" + jupiter + ".jar" )
                .contains( "junit-platform-engine-" + platform + ".jar" )
                .contains( "junit-platform-commons-" + platform + ".jar" )
                .contains( "junit-platform-launcher-" + platform + ".jar" )
                .contains( "opentest4j-" + opentest + ".jar" );

        lines = validator.loadLogLines( startsWith( "[DEBUG] provider(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        assertThat( lines.get( 0 ) )
                .startsWith( "[DEBUG] provider(compact) classpath" )
                .contains( "surefire-api-" /* {VERSION}.jar */ )
                .contains( "surefire-junit-platform-" /* {VERSION}.jar */ )
                .contains( "surefire-logger-api-" /* {VERSION}.jar */ );

        lines = validator.loadLogLines( startsWith( "[DEBUG] boot(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        assertThat( lines.get( 0 ) )
                .startsWith( "[DEBUG] boot(compact) classpath" )
                .contains( "surefire-api-" /* {VERSION}.jar */ )
                .contains( "surefire-booter-" /* {VERSION}.jar */ )
                .contains( "surefire-junit-platform-" /* {VERSION}.jar */ )
                .contains( "surefire-logger-api-" /* {VERSION}.jar */ )
                .contains( "classes" )
                .contains( "test-classes" )
                .contains( "apiguardian-api-" + apiguardian + ".jar" )
                .contains( "junit-jupiter-api-" + jupiter + ".jar" )
                .contains( "junit-jupiter-engine-" + jupiter + ".jar" )
                .contains( "junit-platform-engine-" + platform + ".jar" )
                .contains( "junit-platform-commons-" + platform + ".jar" )
                .contains( "junit-platform-launcher-" + platform + ".jar" )
                .contains( "opentest4j-" + opentest + ".jar" );
    }
}
