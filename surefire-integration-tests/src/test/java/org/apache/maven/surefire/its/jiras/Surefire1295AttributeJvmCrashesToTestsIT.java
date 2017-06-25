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
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.apache.commons.lang.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang.SystemUtils.IS_OS_MAC_OSX;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * https://issues.apache.org/jira/browse/SUREFIRE-1295
 * https://github.com/apache/maven-surefire/pull/136
 *
 * @author michaeltandy
 * @since 2.20
 */
public class Surefire1295AttributeJvmCrashesToTestsIT
        extends SurefireJUnit4IntegrationTestCase
{
    @Before
    public void skipWindows()
    {
        assumeTrue( IS_OS_LINUX || IS_OS_MAC_OSX );
    }

    @Test
    public void crashInFork() throws VerificationException
    {
        SurefireLauncher launcher = unpack( "crash-during-test" );

        checkCrashTypes( launcher );
    }

    @Test
    public void crashInSingleUseFork() throws VerificationException
    {
        SurefireLauncher launcher = unpack( "crash-during-test" )
                                            .forkCount( 1 )
                                            .reuseForks( false );

        checkCrashTypes( launcher );
    }

    @Test
    public void crashInReusableFork() throws VerificationException
    {
        SurefireLauncher launcher = unpack( "crash-during-test" )
                                            .forkPerThread()
                                            .reuseForks( true )
                                            .threadCount( 1 );

        checkCrashTypes( launcher );
    }

    private static void checkCrashTypes( SurefireLauncher launcher )
            throws VerificationException
    {
        checkCrash( launcher.addGoal( "-DcrashType=exit" ) );
        checkCrash( launcher.addGoal( "-DcrashType=abort" ) );
        checkCrash( launcher.addGoal( "-DcrashType=segfault" ) );
    }

    private static void checkCrash( SurefireLauncher launcher ) throws VerificationException
    {
        OutputValidator validator = launcher.maven()
                                            .withFailure()
                                            .executeTest()
                                            .verifyTextInLog( "The forked VM terminated without properly saying "
                                                                      + "goodbye. VM crash or System.exit called?"
                                            )
                                            .verifyTextInLog( "Crashed tests:" );

        for ( Iterator<String> it = validator.loadLogLines().iterator(); it.hasNext(); )
        {
            String line = it.next();
            if ( line.contains( "Crashed tests:" ) )
            {
                line = it.next();
                if ( it.hasNext() )
                {
                    assertThat( line ).contains( "junit44.environment.BasicTest" );
                }
                else
                {
                    fail( "Could not find any line after 'Crashed tests:'." );
                }
            }
        }

    }


}
