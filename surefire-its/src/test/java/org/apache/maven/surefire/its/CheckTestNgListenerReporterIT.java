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

import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaMaxVersion;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

/**
 * Test simple TestNG listener and reporter
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
@RunWith( Parameterized.class )
@SuppressWarnings( { "checkstyle:magicnumber", "checkstyle:linelength" } )
public class CheckTestNgListenerReporterIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Parameters( name = "{index}: TestNG {0}" )
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][]
                {
            { "5.6", "jdk15" }, // First TestNG version with reporter support
            { "5.7", "jdk15" }, // default version from pom of the test case
            { "5.10", "jdk15" },
            { "5.13", null }, // "reporterslist" param becomes String instead of List<ReporterConfig>
                        // "listener" param becomes String instead of List<Class>

                // configure(Map) in 5.14.1 and 5.14.2 is transforming List<Class> into a String with a space as separator.
                // Then configure(CommandLineArgs) splits this String into a List<String> with , or ; as separator => fail.
                // If we used configure(CommandLineArgs), we would not have the problem with white spaces.
            //{ "5.14.1", null, "1.5" }, // "listener" param becomes List instead of String
                            // Fails: Issue with 5.14.1 and 5.14.2 => join with <space>, split with ","
                            // TODO will work with "configure(CommandLineArgs)"
            //{ "5.14.2", null, "1.5" }, // ReporterConfig is not available

            //{ "5.14.3", null, "1.5" }, // TestNG uses "reporter" instead of "reporterslist"
                          // Both String or List are possible for "listener"
                          // Fails: not able to test due to system dependency org.testng:guice missed the path and use to break CI
                          // ClassNotFoundException: com.beust.jcommander.ParameterException

            //{ "5.14.4", null, "1.5" }, { "5.14.5", null, "1.5" }, // Fails: not able to test due to system dependency org.testng:guice missed the path and use to break CI
                                        // ClassNotFoundException: com.beust.jcommander.ParameterException

            { "5.14.6", null }, // Usage of org.testng:guice removed
            { "5.14.9", null }, // Latest 5.14.x TestNG version
            { "6.0", null },
            { "6.14.3", null },
            { "7.0.0", null } // Currently latest TestNG version
        } );
    }

    @Parameter
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String version;

    @Parameter( 1 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String classifier;

    @Test
    public void testNgListenerReporter()
    {
        if ( version.equals( "5.13" ) )
        {
            // only 5.13 uses Google Guice, reflection which breaks jdk 16+
            // module java.base does not "opens java.lang" to unnamed module @209c0b14
            assumeJavaMaxVersion( 15 );
        }

        final SurefireLauncher launcher = unpack( "testng-listener-reporter", "_" + version )
                                                  .sysProp( "testNgVersion", version );

        if ( classifier != null )
        {
            launcher.sysProp( "testNgClassifier", "jdk15" );
        }

        launcher.executeTest()
                .assertTestSuiteResults( 1, 0, 0, 0 )
                .getTargetFile( "resultlistener-output.txt" ).assertFileExists()
                .getTargetFile( "suitelistener-output.txt" ).assertFileExists()
                .getTargetFile( "reporter-output.txt" ).assertFileExists();
    }
}
