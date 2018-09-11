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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Iterator;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS_7;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS_8;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS_10;
import static org.apache.maven.surefire.its.jiras.Surefire1295AttributeJvmCrashesToTestsIT.ForkMode.DEFAULT;
import static org.apache.maven.surefire.its.jiras.Surefire1295AttributeJvmCrashesToTestsIT.ForkMode.ONE_FORK_NO_REUSE;
import static org.apache.maven.surefire.its.jiras.Surefire1295AttributeJvmCrashesToTestsIT.ForkMode.ONE_FORK_REUSE;
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
@RunWith( Parameterized.class )
public class Surefire1295AttributeJvmCrashesToTestsIT
        extends SurefireJUnit4IntegrationTestCase
{
    public enum ForkMode
    {
        DEFAULT,
        ONE_FORK_NO_REUSE,
        ONE_FORK_REUSE
    }

    @Parameters
    public static Iterable<Object[]> parameters()
    {
        return asList(new Object[][] {
//                exit() does not stop all Threads immediately,
//                see https://github.com/michaeltandy/crashjvm/issues/1
                { "exit", DEFAULT },
                { "exit", ONE_FORK_NO_REUSE },
                { "exit", ONE_FORK_REUSE },
                { "abort", DEFAULT },
                { "abort", ONE_FORK_NO_REUSE },
                { "abort", ONE_FORK_REUSE },
                { "segfault", DEFAULT },
                { "segfault", ONE_FORK_NO_REUSE },
                { "segfault", ONE_FORK_REUSE }
        });
    }

    @Parameter( 0 )
    public static String crashStyle;

    @Parameter( 1 )
    public static ForkMode forkStyle;

    @Test
    public void test()
            throws Exception
    {
        assumeTrue( IS_OS_LINUX || IS_OS_MAC_OSX || IS_OS_WINDOWS_7 || IS_OS_WINDOWS_8 || IS_OS_WINDOWS_10 );

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

        for ( Iterator<String> it = validator.loadLogLines().iterator(); it.hasNext(); )
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
