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
        ArrayList<Object[]> args = new ArrayList<Object[]>();
        args.add( new Object[] { "1.0.0", "5.0.0", "1.0.0", "1.0.0" } );
        args.add( new Object[] { "1.1.1", "5.1.1", "1.0.0", "1.0.0" } );
        args.add( new Object[] { "1.2.0", "5.2.0", "1.1.0", "1.0.0" } );
        args.add( new Object[] { "1.3.1", "5.3.1", "1.1.1", "1.0.0" } );
        args.add( new Object[] { "1.4.0-SNAPSHOT", "5.4.0-SNAPSHOT", "1.1.1", "1.0.0" } );
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

        String testClasspath = "[DEBUG] test(compact) classpath:"
                + "  test-classes"
                + "  classes"
                + "  junit-jupiter-engine-" + jupiter + ".jar"
                + "  apiguardian-api-" + apiguardian + ".jar"
                + "  junit-platform-engine-" + platform + ".jar"
                + "  junit-platform-commons-" + platform + ".jar"
                + "  opentest4j-" + opentest + ".jar"
                + "  junit-jupiter-api-" + jupiter + ".jar";

        List<String> lines = validator.loadLogLines( startsWith( "[DEBUG] test(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        String line = lines.get( 0 );

        assertThat( set( line ), regex( toRegex( testClasspath ) ) );

        String providerClasspath = "[DEBUG] provider(compact) classpath:"
                + "  surefire-junit-platform-*.jar"
                + "  surefire-api-*.jar"
                + "  surefire-logger-api-*.jar"
                + "  common-java5-*.jar"
                + "  junit-platform-launcher-1.3.1.jar";

        lines = validator.loadLogLines( startsWith( "[DEBUG] provider(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        line = lines.get( 0 );

        assertThat( set( line ), regex( toRegex( providerClasspath ) ) );

        String bootClasspath = "[DEBUG] boot(compact) classpath:"
                + "  surefire-booter-*.jar"
                + "  surefire-api-*.jar"
                + "  surefire-logger-api-*.jar"
                + "  test-classes"
                + "  classes"
                + "  junit-jupiter-engine-" + jupiter + ".jar"
                + "  apiguardian-api-" + apiguardian + ".jar"
                + "  junit-platform-engine-" + platform + ".jar"
                + "  junit-platform-commons-" + platform + ".jar"
                + "  opentest4j-" + opentest + ".jar"
                + "  junit-jupiter-api-" + jupiter + ".jar"
                + "  surefire-junit-platform-*.jar"
                + "  junit-platform-launcher-1.3.1.jar";

        lines = validator.loadLogLines( startsWith( "[DEBUG] boot(compact) classpath" ) );

        assertThat( lines )
                .hasSize( 1 );

        line = lines.get( 0 );

        assertThat( set( line ), regex( toRegex( bootClasspath ) ) );
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
