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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test simple TestNG listener and reporter
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
@RunWith(Parameterized.class)
public class CheckTestNgListenerReporterIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Parameterized.Parameters(name = "{index}: TestNG {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
            { "5.6" }, // First TestNG version with reporter support
            { "5.7" }, // default version from pom of the test case
            { "5.10" },
            { "5.13" }, // "reporterslist" param becomes String instead of List<ReporterConfig>
                        // "listener" param becomes String instead of List<Class>
            //{ "5.14.1" }, // "listener" param becomes List instead of String
                            // Fails: Issue with 5.14.1 and 5.14.2 => join with <space>, split with ","
                            // TODO will work with "configure(CommandLineArgs)"
            //{ "5.14.2" }, // ReporterConfig is not available
            //{ "5.14.3" }, // TestNG uses "reporter" instead of "reporterslist"
                          // Both String or List are possible for "listener"
                          // Fails: Bad formatted pom => transitive dependencies are missing
            { "5.14.4" }, // Usage of org.testng:guice
                          // Caution: Some TestNG features may fail with 5.14.4 and 5.14.5 due to missing dependency
            { "5.14.6" }, // Usage of org.testng:guice removed
            { "5.14.9" }, // Latest 5.14.x TestNG version
            { "6.0" },
            { "6.9.9" } // Current latest TestNG version
        });
    }

    @Parameterized.Parameter
    public String version;
    private SurefireLauncher verifierStarter;

    @Test
    public void testNgListenerReporter()
    {
        verifierStarter = unpack( "testng-listener-reporter", "_" + version );
        verifierStarter.resetInitialGoals( version );
        verifierStarter.executeTest().verifyErrorFree( 1 )
            .getTargetFile( "resultlistener-output.txt" ).assertFileExists()
            .getTargetFile( "suitelistener-output.txt" ).assertFileExists()
            .getTargetFile( "reporter-output.txt" ).assertFileExists();
    }
}
