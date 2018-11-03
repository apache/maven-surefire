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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireVerifierException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

import static org.fest.assertions.Assertions.assertThat;

/**
 * SUREFIRE-613 Asserts proper test counts when running in parallel
 *
 * @author Kristian Rosenvold
 */
public class Surefire141PluggableProvidersIT
    extends SurefireJUnit4IntegrationTestCase
{
    @BeforeClass
    public static void installProvider()
    {
        unpack( Surefire141PluggableProvidersIT.class, "surefire-141-pluggableproviders-provider", "prov" )
            .executeInstall();
    }

    @Test
    public void pluggableProviderPresent()
    {
        unpack( "surefire-141-pluggableproviders" )
            .setForkJvm()
            .maven()
            .showExceptionMessages()
            .debugLogging()
            .executeTest()
            .verifyTextInLog( "Using configured provider org.apache.maven.surefire.testprovider.TestProvider" )
            .verifyTextInLog( "Using configured provider org.apache.maven.surefire.junit.JUnit3Provider" )
            .verifyErrorFreeLog();
    }

    @Test
    public void invokeRuntimeException()
    {
        final String errorText = "Let's fail with a runtimeException";

        OutputValidator validator = unpack( "surefire-141-pluggableproviders" )
            .setForkJvm()
            .sysProp( "invokeCrash", "runtimeException" )
            .maven()
            .withFailure()
            .executeTest();

        assertErrorMessage( validator, errorText );

        boolean hasErrorInLog = verifiedErrorInLog( validator, "There was an error in the forked process" );
        boolean verifiedInLog = verifiedErrorInLog( validator, errorText );
        assertThat( hasErrorInLog && verifiedInLog )
                .describedAs( "'" + errorText + "' could not be verified in log.txt nor *.dump file. ("
                                      + hasErrorInLog + ", " + verifiedInLog + ")" )
                .isTrue();
    }

    @Test
    public void invokeReporterException()
    {
        final String errorText = "Let's fail with a reporterexception";

        OutputValidator validator = unpack( "surefire-141-pluggableproviders" )
            .setForkJvm()
            .sysProp( "invokeCrash", "reporterException" )
            .maven()
            .withFailure()
            .executeTest();

        assertErrorMessage( validator, errorText );

        boolean hasErrorInLog = verifiedErrorInLog( validator, "There was an error in the forked process" );
        boolean verifiedInLog = verifiedErrorInLog( validator, errorText );
        assertThat( hasErrorInLog && verifiedInLog )
                .describedAs( "'" + errorText + "' could not be verified in log.txt nor *.dump file. ("
                                      + hasErrorInLog + ", " + verifiedInLog + ")" )
                .isTrue();
    }

    @Test
    public void constructorRuntimeException()
    {
        final String errorText = "Let's fail with a runtimeException";

        OutputValidator validator = unpack( "surefire-141-pluggableproviders" )
                                            .setForkJvm()
                                            .sysProp( "constructorCrash", "runtimeException" )
                                            .maven()
                                            .withFailure()
                                            .executeTest();

        assertErrorMessage( validator, errorText );

        boolean hasErrorInLog = verifiedErrorInLog( validator, "There was an error in the forked process" );
        boolean verifiedInLog = verifiedErrorInLog( validator, errorText );
        assertThat( hasErrorInLog && verifiedInLog )
                .describedAs( "'" + errorText + "' could not be verified in log.txt nor *.dump file. ("
                                      + hasErrorInLog + ", " + verifiedInLog + ")" )
                .isTrue();
    }

    private static void assertErrorMessage( OutputValidator validator, String message )
    {
        File reportDir = validator.getSurefireReportsDirectory();
        String[] dumpFiles = reportDir.list( new FilenameFilter()
                                             {
                                                 @Override
                                                 public boolean accept( File dir, String name )
                                                 {
                                                     return name.endsWith( "-jvmRun1.dump" );
                                                 }
                                             });
        assertThat( dumpFiles )
                .isNotNull()
                .isNotEmpty();
        for ( String dump : dumpFiles )
        {
            validator.getSurefireReportsFile( dump )
                    .assertContainsText( message );
        }
    }

    private static boolean verifiedErrorInLog( OutputValidator validator, String errorText )
    {
        try
        {
            validator.verifyTextInLog( errorText );
            return true;
        }
        catch ( SurefireVerifierException e )
        {
            return false;
        }
    }
}
