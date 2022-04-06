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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.its.fixture.IsRegex.regex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Sets.set;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 *
 */
@RunWith( Parameterized.class )
@SuppressWarnings( "checkstyle:magicnumber" )
public class JUnitPlatformEnginesIT extends SurefireJUnit4IntegrationTestCase
{
    private static final String XML_TESTSUITE_FRAGMENT =
            "<testsuite xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation="
                    + "\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd\" "
                    + "version=\"3.0\" name=\"&lt;&lt; ✨ &gt;&gt;\"";

    @Parameter
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String platform;

    @Parameter( 1 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String jupiter;

    @Parameter( 2 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String opentest;

    @Parameter( 3 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String apiguardian;

    @Parameters( name = "{0}" )
    public static Iterable<Object[]> artifactVersions()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] {"1.0.3", "5.0.3", "1.0.0", "1.0.0"} );
        args.add( new Object[] {"1.1.1", "5.1.1", "1.0.0", "1.0.0"} );
        args.add( new Object[] {"1.2.0", "5.2.0", "1.1.0", "1.0.0"} );
        args.add( new Object[] {"1.3.2", "5.3.2", "1.1.1", "1.0.0"} );
        args.add( new Object[] {"1.4.2", "5.4.2", "1.1.1", "1.0.0"} );
        args.add( new Object[] {"1.5.2", "5.5.2", "1.2.0", "1.1.0"} );
        args.add( new Object[] {"1.6.2", "5.6.2", "1.2.0", "1.1.0"} );
        //args.add( new Object[] { "1.6.0-SNAPSHOT", "5.6.0-SNAPSHOT", "1.2.0", "1.1.0" } );
        return args;
    }

    @Test
    public void testToRegex()
    {
        String regex = toRegex( ".[]()*" );
        assertThat( regex ).isEqualTo( "\\.\\[\\]\\(\\).*" );
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

        assertThat( lines ).hasSize( 1 );

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

        assertThat( lines ).hasSize( 1 );

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

        assertThat( lines ).hasSize( 1 );

        line = lines.get( 0 );

        assertThat( set( line ), allOf(
                regex( toRegex( "*[DEBUG] boot(compact) classpath:*" ) ),
                regex( toRegex( "*surefire-booter-*.jar*" ) ),
                regex( toRegex( "*surefire-api-*.jar*" ) ),
                regex( toRegex( "*surefire-logger-api-*.jar*" ) ),
                regex( toRegex( "*  test-classes*" ) ),
                regex( toRegex( "*  classes*" ) ),
                regex( toRegex( "*junit-jupiter-engine-" + jupiter + ".jar*" ) ),
                regex( toRegex( "*apiguardian-api-" + apiguardian + ".jar*"  ) ),
                regex( toRegex( "*junit-platform-engine-" + platform + ".jar*" ) ),
                regex( toRegex( "*junit-platform-commons-" + platform + ".jar*" ) ),
                regex( toRegex( "*opentest4j-" + opentest + ".jar*" ) ),
                regex( toRegex( "*junit-jupiter-api-" + jupiter + ".jar*" ) ),
                regex( toRegex( "*surefire-junit-platform-*.jar*" ) ),
                regex( toRegex( "*junit-platform-launcher-" + platform + ".jar*" ) ) ) );
    }

    @Test
    public void testJupiterEngine()
    {
        unpack( "junit-platform-engine-jupiter", "-" + jupiter )
                .setTestToRun( "Basic*Test" )
                .sysProp( "junit5.version", jupiter )
                .executeTest()
                .verifyErrorFree( 5 );
    }

    @Test
    public void failingBeforeAllMethod()
    {
        OutputValidator validator = unpack( "surefire-1688", "-" + jupiter )
                .setTestToRun( "FailingBeforeAllJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "oneTimeSetUp() failed" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "jira1688.FailingBeforeAllJupiterTest.txt", UTF_8 )
                .assertContainsText( "oneTimeSetUp() failed" );
    }

    @Test
    public void errorInBeforeAllMethod()
    {
        OutputValidator validator = unpack( "surefire-1741", "-" + jupiter )
                .setTestToRun( "ErrorInBeforeAllJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "oneTimeSetUp() encountered an error" )
                .assertTestSuiteResults( 1, 1, 0, 0 );

        validator.getSurefireReportsFile( "jira1741.ErrorInBeforeAllJupiterTest.txt", UTF_8 )
                .assertContainsText( "oneTimeSetUp() encountered an error" );
    }

    @Test
    public void testJupiterEngineWithErrorInParameterizedSource()
    {
        OutputValidator validator = unpack( "surefire-1741", "-" + jupiter )
                .setTestToRun( "ErrorInParameterizedSourceJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "args() method source encountered an error" )
                .assertTestSuiteResults( 1, 1, 0, 0 );

        validator.getSurefireReportsFile( "jira1741.ErrorInParameterizedSourceJupiterTest.txt", UTF_8 )
                .assertContainsText( "args() method source encountered an error" );
    }

    @Test
    public void testJupiterEngineWithFailureInParameterizedSource()
    {
        OutputValidator validator = unpack( "surefire-1741", "-" + jupiter )
                .setTestToRun( "FailureInParameterizedSourceJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "args() method source failed" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "jira1741.FailureInParameterizedSourceJupiterTest.txt", UTF_8 )
                .assertContainsText( "args() method source failed" );
    }

    @Test
    public void testJupiterEngineWithErrorInTestFactory()
    {
        OutputValidator validator = unpack( "surefire-1727", "-" + jupiter )
                .setTestToRun( "ErrorInTestFactoryJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "Encountered error in TestFactory testFactory()" )
                .assertTestSuiteResults( 1, 1, 0, 0 );

        validator.getSurefireReportsFile( "jira1727.ErrorInTestFactoryJupiterTest.txt", UTF_8 )
                .assertContainsText( "Encountered error in TestFactory testFactory()" );
    }

    @Test
    public void testJupiterEngineWithFailureInTestFactory()
    {
        OutputValidator validator = unpack( "surefire-1727", "-" + jupiter )
                .setTestToRun( "FailureInTestFactoryJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "Encountered failure in TestFactory testFactory()" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "jira1727.FailureInTestFactoryJupiterTest.txt", UTF_8 )
                .assertContainsText( "Encountered failure in TestFactory testFactory()" );
    }

    @Test
    public void testJupiterEngineWithErrorInTestTemplateProvider()
    {
        OutputValidator validator = unpack( "surefire-1727", "-" + jupiter )
                .setTestToRun( "ErrorInTestTemplateProviderTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "Encountered error in TestTemplate provideTestTemplateInvocationContexts()" )
                .assertTestSuiteResults( 1, 1, 0, 0 );

        validator.getSurefireReportsFile( "jira1727.ErrorInTestTemplateProviderTest.txt", UTF_8 )
                .assertContainsText( "Encountered error in TestTemplate provideTestTemplateInvocationContexts()" );
    }

    @Test
    public void testJupiterEngineWithFailureInTestTemplateProvider()
    {
        OutputValidator validator = unpack( "surefire-1727", "-" + jupiter )
                .setTestToRun( "FailureInTestTemplateProviderTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "Encountered failure in TestTemplate provideTestTemplateInvocationContexts()" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "jira1727.FailureInTestTemplateProviderTest.txt", UTF_8 )
                .assertContainsText( "Encountered failure in TestTemplate provideTestTemplateInvocationContexts()" );
    }

    @Test
    public void testJupiterEngineWithAssertionsFailNoParameters()
    {
        // `Assertions.fail()` not supported until 5.2.0
        assumeThat( jupiter, is( not( "5.0.3" ) ) );
        assumeThat( jupiter, is( not( "5.1.1" ) ) );

        OutputValidator validator = unpack( "surefire-1748-fail-no-parameters", "-" + jupiter )
                .setTestToRun( "AssertionsFailNoParametersJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "AssertionsFailNoParametersJupiterTest.doTest:31" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "jira1748.AssertionsFailNoParametersJupiterTest.txt", UTF_8 )
                .assertContainsText( "AssertionsFailNoParametersJupiterTest.doTest"
                        + "(AssertionsFailNoParametersJupiterTest.java:31)" );
    }

    @Test
    public void testJupiterEngineWithAssertionsFailEmptyStringParameters()
    {
        OutputValidator validator = unpack( "surefire-1748", "-" + jupiter )
                .setTestToRun( "AssertionsFailEmptyStringParameterJupiterTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "AssertionsFailEmptyStringParameterJupiterTest.doTest:31" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "jira1748.AssertionsFailEmptyStringParameterJupiterTest.txt", UTF_8 )
                .assertContainsText( "AssertionsFailEmptyStringParameterJupiterTest.doTest"
                        + "(AssertionsFailEmptyStringParameterJupiterTest.java:31)" );
    }

    @Test
    public void testJupiterEngineWithAssertionsFailMessage()
    {
        OutputValidator validator = unpack( "surefire-1857-assertion-message", "-" + jupiter )
                .setTestToRun( "AssertionFailureMessageTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "AssertionFailureMessageTest.failedTest:31" )
                .assertTestSuiteResults( 1, 0, 1, 0 );

        validator.getSurefireReportsFile( "TEST-jira1857.AssertionFailureMessageTest.xml", UTF_8 )
                .assertContainsText( "message=\"fail_message\"" );
    }

    @Test
    public void testJupiterEngineWithExceptionMessage()
    {
        OutputValidator validator = unpack( "surefire-1857-exception-message", "-" + jupiter )
                .setTestToRun( "ExceptionMessageTest" )
                .sysProp( "junit5.version", jupiter )
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "ExceptionMessageTest.errorTest:28" )
                .assertTestSuiteResults( 1, 1, 0, 0 );

        validator.getSurefireReportsFile( "TEST-jira1857.ExceptionMessageTest.xml", UTF_8 )
                .assertContainsText( "message=\"error_message\"" );
    }

    @Test
    public void testJupiterEngineWithDisplayNames()
    {
        OutputValidator validator = unpack( "junit-platform-engine-jupiter", "-" + jupiter )
                .sysProp( "junit5.version", jupiter )
                .executeTest()
                .verifyErrorFree( 7 );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest.txt", UTF_8 )
                .assertContainsText( "<< ✨ >>" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest.txt", UTF_8 )
                .assertContainsText( "Test set: << ✨ >>" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest.txt", UTF_8 )
                .assertContainsText( " - in << ✨ >>" );


        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest-output.txt", UTF_8 )
                .assertContainsText( "<< ✨ >>" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest-output.txt", UTF_8 )
                .assertContainsText( "73$71 ✔" );

        validator.getSurefireReportsFile( "junitplatformenginejupiter.DisplayNameTest-output.txt", UTF_8 )
                .assertContainsText( "73$72 ✔" );


        validator.getSurefireReportsFile( "TEST-junitplatformenginejupiter.DisplayNameTest.xml", UTF_8 )
                .assertContainsText( "testcase name=\"73$71 ✔\" classname=\"&lt;&lt; ✨ &gt;&gt;\"" )
                .assertContainsText( "testcase name=\"73$72 ✔\" classname=\"&lt;&lt; ✨ &gt;&gt;\"" )
                .assertContainsText( XML_TESTSUITE_FRAGMENT );


        validator.getSurefireReportsFile( "TEST-junitplatformenginejupiter.BasicJupiterTest.xml", UTF_8 )
                .assertContainsText( "<testcase name=\"test(TestInfo)\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 0 + 1 = 1\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 1 + 2 = 3\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 49 + 51 = 100\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"" )
                .assertContainsText( "<testcase name=\"add(int, int, int) 1 + 100 = 101\" "
                        + "classname=\"junitplatformenginejupiter.BasicJupiterTest\"" );
    }

    @Test
    public void testTags()
    {
        // [don't & !forced] not supported in 5.0.3 as it seems
        // PreconditionViolationException: Tag name [don't & !forced] must be syntactically valid
        assumeThat( jupiter, is( not( "5.0.3" ) ) );

        unpack( "junit-platform-tags", "-" + jupiter )
                .sysProp( "junit5.version", jupiter )
                .executeTest()
                .verifyErrorFree( 2 );
    }

    private static String toRegex( String text )
    {
        return text.replaceAll( "\\.", "\\\\." )
                .replaceAll( "\\[", "\\\\[" )
                .replaceAll( "]", "\\\\]" )
                .replaceAll( "\\(", "\\\\(" )
                .replaceAll( "\\)", "\\\\)" )
                .replaceAll( "\\*", ".*" );
    }
}
