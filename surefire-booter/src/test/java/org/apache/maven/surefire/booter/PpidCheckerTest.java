package org.apache.maven.surefire.booter;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Testing {@link PpidChecker} on a platform.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class PpidCheckerTest
{
    @Rule
    public final ExpectedException exceptions = ExpectedException.none();

    @Test
    public void shouldHavePpidAsWindows()
    {
        assumeTrue( IS_OS_WINDOWS );

        long expectedPid = Long.parseLong( ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim() );

        PpidChecker checker = new PpidChecker( expectedPid );
        ProcessInfo processInfo = checker.windows();

        assertThat( processInfo )
                .isNotNull();

        assertThat( checker.canUse() )
                .isTrue();

        assertThat( checker.isProcessAlive() )
                .isTrue();

        assertThat( processInfo.getPID() )
                .isEqualTo( expectedPid );

        assertThat( processInfo.getTime() )
                .isNotNull();
    }

    @Test
    public void shouldHavePpidAsUnix()
    {
        assumeTrue( IS_OS_UNIX );

        assertThat( PpidChecker.canExecuteUnixPs() )
                .as( "Surefire should be tested on real box OS, e.g. Ubuntu or FreeBSD." )
                .isTrue();

        long expectedPid = Long.parseLong( ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim() );

        PpidChecker checker = new PpidChecker( expectedPid );
        ProcessInfo processInfo = checker.unix();

        assertThat( processInfo )
                .isNotNull();

        assertThat( checker.canUse() )
                .isTrue();

        assertThat( checker.isProcessAlive() )
                .isTrue();

        assertThat( processInfo.getPID() )
                .isEqualTo( expectedPid );

        assertThat( processInfo.getTime() )
                .isNotNull();
    }

    @Test
    public void shouldNotFindSuchPID()
    {
        long ppid = 1000000L;

        PpidChecker checker = new PpidChecker( ppid );

        assertThat( checker.canUse() )
                .isTrue();

        exceptions.expect( IllegalStateException.class );
        exceptions.expectMessage( "Cannot use PPID " + ppid + " process information. Going to use NOOP events." );

        checker.isProcessAlive();

        fail( "this test should throw exception" );
    }

    @Test
    public void shouldParseEtime()
    {
        Matcher m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "38" );
        assertThat( m.matches() )
                .isFalse();

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "05:38" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "00:05:38" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "01:05:38" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 3600L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "02-01:05:38" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 2 * 24 * 3600L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 3600L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "02-1:5:3" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 2 * 24 * 3600L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 3600L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 3L );
    }

    @Test
    public void shouldHaveSystemPathToWmicOnWindows() throws Exception
    {
        assumeTrue( IS_OS_WINDOWS );
        assumeThat( System.getenv( "SystemRoot" ), is( notNullValue() ) );
        assumeThat( System.getenv( "SystemRoot" ), is( not( "" ) ) );
        assumeTrue( new File( System.getenv( "SystemRoot" ), "System32\\Wbem" ).isDirectory() );
        assumeTrue( new File( System.getenv( "SystemRoot" ), "System32\\Wbem\\wmic.exe" ).isFile() );
        assertThat( (Boolean) invokeMethod( PpidChecker.class, "hasWmicStandardSystemPath" ) ).isTrue();
        assertThat( new File( System.getenv( "SystemRoot" ), "System32\\Wbem\\wmic.exe" ) ).isFile();
    }
}
