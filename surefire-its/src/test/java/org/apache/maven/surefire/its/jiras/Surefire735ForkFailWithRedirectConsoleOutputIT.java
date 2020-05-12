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

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * @author Kristian Rosenvold
 */
public class Surefire735ForkFailWithRedirectConsoleOutputIT
        extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void vmStartFail() throws VerificationException
    {
        OutputValidator outputValidator = unpack().failNever().executeTest();
        assertJvmCrashed( outputValidator );
    }

    @Test
    public void vmStartFailShouldFailBuildk() throws VerificationException
    {
        OutputValidator outputValidator = unpack().maven().withFailure().executeTest();
        assertJvmCrashed( outputValidator );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "fork-fail" );
    }

    private static void assertJvmCrashed( OutputValidator outputValidator ) throws VerificationException
    {
        Collection<String> matchedLines =
            outputValidator.loadLogLines( containsString( "Invalid maximum heap size: -Xmxxxx712743m" ) );
        if ( !matchedLines.isEmpty() )
        {
            // the error line was printed in std/err by the JVM
            return;
        }

        // the error line should be printed in std/out by the JVM if we use the process pipes
        // then the ForkClient caught it and printed in the dump stream
        File reportDir = outputValidator.getSurefireReportsDirectory();
        String[] dumpFiles = reportDir.list( new FilenameFilter()
                                             {
                                                 @Override
                                                 public boolean accept( File dir, String name )
                                                 {
                                                     return name.endsWith( ".dumpstream" )
                                                         && !name.contains( "-jvmRun1" );
                                                 }
                                             }
        );

        assertThat( dumpFiles ).isNotEmpty();

        for ( String dump : dumpFiles )
        {
            outputValidator.getSurefireReportsFile( dump )
                    .assertContainsText( "Invalid maximum heap size: -Xmxxxx712743m" );
        }
    }
}
