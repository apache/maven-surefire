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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Iterator;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS_7;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS_8;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS_10;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * https://issues.apache.org/jira/browse/SUREFIRE-1295
 * https://github.com/apache/maven-surefire/pull/136
 *
 * @author michaeltandy
 * @since 2.20
 */
@RunWith( Theories.class )
public class Surefire1295AttributeJvmCrashesToTestsIT
        extends SurefireJUnit4IntegrationTestCase
{
    public enum ForkMode
    {
        DEFAULT,
        ONE_FORK_NO_REUSE,
        ONE_FORK_REUSE
    }

    @DataPoints( "crashStyle" )
    public static String[] crashStyle = { "exit", "abort", "segfault" };

    @DataPoints( "forkStyle" )
    public static ForkMode[] forkStyle = ForkMode.values();

    @Theory
    public void test( @FromDataPoints( "crashStyle" ) String crashStyle,
                      @FromDataPoints( "forkStyle" ) ForkMode forkStyle )
            throws Exception
    {
        // JUnit Assumptions not supported by Theories runner.
        if ( !IS_OS_LINUX && !IS_OS_MAC_OSX && !( IS_OS_WINDOWS_7 || IS_OS_WINDOWS_8 || IS_OS_WINDOWS_10 ) )
        {
            return;
        }

        SurefireLauncher launcher =
                unpack( "crash-during-test", "_" + crashStyle + "_" + forkStyle.ordinal() )
                .setForkJvm();

        switch ( forkStyle )
        {
            case DEFAULT:
                break;
            case ONE_FORK_NO_REUSE:
                launcher.forkCount( 1 )
                        .reuseForks( false );
                break;
            case ONE_FORK_REUSE:
                launcher.forkPerThread()
                        .reuseForks( true )
                        .threadCount( 1 );
                break;
            default:
                fail();
        }

        checkCrash( launcher.addGoal( "-DcrashType=" + crashStyle ) );
    }

    private static void checkCrash( SurefireLauncher launcher ) throws Exception
    {
        OutputValidator validator = launcher.maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog( "The forked VM terminated without properly saying "
                        + "goodbye. VM crash or System.exit called?" )
                .verifyTextInLog( "Crashed tests:" );

        // Cannot flush log.txt stream because it is consumed internally by Verifier.
        // Waiting for the stream to become flushed on disk.
        SECONDS.sleep( 1L );

        for ( Iterator< String > it = validator.loadLogLines().iterator(); it.hasNext(); )
        {
            String line = it.next();
            if ( line.contains( "Crashed tests:" ) )
            {
                line = it.next();
                if ( it.hasNext() )
                {
                    assertThat( line )
                            .contains( "junit44.environment.Test1CrashedTest" );
                }
                else
                {
                    fail( "Could not find any line after 'Crashed tests:'." );
                }
            }
        }
    }
}
