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
import static org.apache.maven.surefire.its.fixture.IsRegex.regex;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.util.Collections.set;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

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
    public static Iterable<Object[]> regexVersions()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] { "1.0.3", "5.0.3", "1.0.0", "1.0.0" } );
        args.add( new Object[] { "1.1.1", "5.1.1", "1.0.0", "1.0.0" } );
        args.add( new Object[] { "1.2.0", "5.2.0", "1.1.0", "1.0.0" } );
        args.add( new Object[] { "1.3.2", "5.3.2", "1.1.1", "1.0.0" } );
        args.add( new Object[] { "1.4.2", "5.4.2", "1.1.1", "1.0.0" } );
        args.add( new Object[] { "1.5.0-M1", "5.5.0-M1", "1.1.1", "1.0.0" } );
        args.add( new Object[] { "1.5.0-SNAPSHOT", "5.5.0-SNAPSHOT", "1.2.0-SNAPSHOT", "1.0.0" } );
        return args;
    }

    @Before
    public void setUp()
    {
        assumeJavaVersion( 1.8d );
    }

    @Test
    public void testToRegex()
    {
        String regex = toRegex( ".[]()*" );
        assertThat( regex )
                .isEqualTo( "\\.\\[\\]\\(\\).*" );
    }

    @Test
    public void platform() throws VerificationException
    {
        OutputValidator validator = unpack( "junit-platform", '-' + platform )
                .sysProp( "jupiter.version", jupiter )
                .debugLogging()
                .executeTest()
                .verifyErrorFree( 1 );

        List<String> lines = validator.loadLogLines( startsWith( "[DEBUG] test(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        String line = lines.get( 0 );

        assertThat( set( line ), allOf(
                regex( toRegex( "*[DEBUG] test(compact) classpath:*" ) ),
                regex( toRegex( "*  test-classes*" ) ),
                regex( toRegex( "*  classes*" ) ),
                regex( toRegex( "*junit-jupiter-engine-" + jupiter + ".jar*" ) ),
                regex( toRegex( "*apiguardian-api-" + apiguardian + ".jar*" ) ),
                regex( toRegex( "*junit-platform-engine-" + platform + ".jar*" ) ),
                regex( toRegex( "*junit-platform-commons-" + platform + ".jar*" ) ),
                regex( toRegex( "*opentest4j-" + opentest + ".jar*" ) ),
                regex( toRegex( "*junit-jupiter-api-" + jupiter + ".jar*" ) )
        ) );

        lines = validator.loadLogLines( startsWith( "[DEBUG] provider(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        line = lines.get( 0 );

        assertThat( set( line ), allOf(
                regex( toRegex( "*[DEBUG] provider(compact) classpath:*" ) ),
                regex( toRegex( "*surefire-junit-platform-*.jar*" ) ),
                regex( toRegex( "*surefire-api-*.jar*" ) ),
                regex( toRegex( "*surefire-logger-api-*.jar*" ) ),
                regex( toRegex( "*common-java5-*.jar*" ) ),
                regex( toRegex( "*junit-platform-launcher-" + platform + ".jar*" ) )
        ) );

        lines = validator.loadLogLines( startsWith( "[DEBUG] boot(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        line = lines.get( 0 );

        assertThat( set( line ), allOf(
                regex( toRegex( "*[DEBUG] boot(compact) classpath:*" ) ),
                regex( toRegex( "*surefire-booter-*.jar*" ) ),
                regex( toRegex( "*surefire-api-*.jar*" ) ),
                regex( toRegex( "*surefire-logger-api-*.jar*" ) ),
                regex( toRegex( "*  test-classes*" ) ),
                regex( toRegex( "*  classes*" ) ),
                regex( toRegex( "*junit-jupiter-engine-" + jupiter + ".jar*" ) ),
                regex( toRegex("*apiguardian-api-" + apiguardian + ".jar*"  ) ),
                regex( toRegex( "*junit-platform-engine-" + platform + ".jar*" ) ),
                regex( toRegex(  "*junit-platform-commons-" + platform + ".jar*" ) ),
                regex( toRegex( "*opentest4j-" + opentest + ".jar*" ) ),
                regex( toRegex( "*junit-jupiter-api-" + jupiter + ".jar*" ) ),
                regex( toRegex( "*surefire-junit-platform-*.jar*" ) ),
                regex( toRegex(  "*junit-platform-launcher-" + platform + ".jar*" ) )
        ) );
    }

    private static String toRegex(String text) {
        return text.replaceAll( "\\.", "\\\\." )
                .replaceAll( "\\[", "\\\\[" )
                .replaceAll( "]", "\\\\]" )
                .replaceAll( "\\(", "\\\\(" )
                .replaceAll( "\\)", "\\\\)" )
                .replaceAll( "\\*", ".*" );
    }
}
