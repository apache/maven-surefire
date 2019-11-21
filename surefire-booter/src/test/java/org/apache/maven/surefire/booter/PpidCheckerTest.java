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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.surefire.booter.ProcessInfo.unixProcessInfo;
import static org.apache.maven.surefire.booter.ProcessInfo.windowsProcessInfo;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;

/**
 * Testing {@link PpidChecker} on a platform.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class PpidCheckerTest
{
    @Rule
    public final ExpectedException exceptions = ExpectedException.none();

    @Test
    public void canExecuteUnixPs()
    {
        assumeTrue( IS_OS_UNIX );
        assertThat( PpidChecker.canExecuteUnixPs() )
                .as( "Surefire should be tested on real box OS, e.g. Ubuntu or FreeBSD." )
                .isTrue();
    }

    @Test
    public void shouldHavePidAtBegin()
    {
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();

        PpidChecker checker = new PpidChecker( expectedPid );
        ProcessInfo processInfo = IS_OS_UNIX ? checker.unix() : checker.windows();

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
    public void shouldHavePid() throws Exception
    {
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();
        System.out.println( "java version " + System.getProperty( "java.version" ) + " expectedPid=" + expectedPid );

        PpidChecker checker = new PpidChecker( expectedPid );
        setInternalState( checker, "parentProcessInfo",
                IS_OS_UNIX
                        ? unixProcessInfo( expectedPid, 0L )
                        : windowsProcessInfo( expectedPid, windowsProcessStartTime( checker ) ) );

        // the etime in Unix is measured in seconds. So let's wait 1s at least.
        SECONDS.sleep( 1L );

        ProcessInfo processInfo = IS_OS_UNIX ? checker.unix() : checker.windows();

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

        assertThat( checker.toString() )
                .contains( "ppid=" + expectedPid )
                .contains( "stopped=false" )
                .contains( "invalid=false" )
                .contains( "error=false" );

        checker.destroyActiveCommands();
        assertThat( checker.canUse() )
                .isFalse();
        assertThat( (boolean) invokeMethod( checker, "isStopped" ) )
                .isTrue();
    }

    @Test
    public void shouldBeStopped()
    {
        PpidChecker checker = new PpidChecker( "0" );
        checker.stop();

        assertThat( checker.canUse() )
                .isFalse();

        exceptions.expect( IllegalStateException.class );
        exceptions.expectMessage( "irrelevant to call isProcessAlive()" );

        checker.isProcessAlive();

        fail( "this test should throw exception" );
    }

    @Test
    public void shouldNotFindSuchPID()
    {
        PpidChecker checker = new PpidChecker( "1000000" );
        setInternalState( checker, "parentProcessInfo", ProcessInfo.ERR_PROCESS_INFO );

        assertThat( checker.canUse() )
                .isFalse();

        exceptions.expect( IllegalStateException.class );
        exceptions.expectMessage( "irrelevant to call isProcessAlive()" );

        checker.isProcessAlive();

        fail( "this test should throw exception" );
    }

    @Test
    public void shouldNotBeAlive()
    {
        PpidChecker checker = new PpidChecker( "1000000" );

        assertThat( checker.canUse() )
                .isTrue();

        assertThat( checker.isProcessAlive() )
                .isFalse();
    }

    @Test
    public void shouldParseEtime()
    {
        Matcher m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "38 1234567890" );
        assertThat( m.matches() )
                .isFalse();

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "05:38 1234567890" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );
        assertThat( PpidChecker.fromPID( m ) ).isEqualTo( "1234567890" );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "00:05:38 1234567890" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );
        assertThat( PpidChecker.fromPID( m ) ).isEqualTo( "1234567890" );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "01:05:38 1234567890" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 0L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 3600L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );
        assertThat( PpidChecker.fromPID( m ) ).isEqualTo( "1234567890" );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "02-01:05:38 1234567890" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 2 * 24 * 3600L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 3600L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 38L );
        assertThat( PpidChecker.fromPID( m ) ).isEqualTo( "1234567890" );

        m = PpidChecker.UNIX_CMD_OUT_PATTERN.matcher( "02-1:5:3 1234567890" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromDays( m ) ).isEqualTo( 2 * 24 * 3600L );
        assertThat( PpidChecker.fromHours( m ) ).isEqualTo( 3600L );
        assertThat( PpidChecker.fromMinutes( m ) ).isEqualTo( 300L );
        assertThat( PpidChecker.fromSeconds( m ) ).isEqualTo( 3L );
        assertThat( PpidChecker.fromPID( m ) ).isEqualTo( "1234567890" );
    }

    @Test
    public void shouldParseBusyboxHoursEtime()
    {
        Matcher m = PpidChecker.BUSYBOX_CMD_OUT_PATTERN.matcher( "38 1234567890" );
        assertThat( m.matches() )
                .isFalse();

        m = PpidChecker.BUSYBOX_CMD_OUT_PATTERN.matcher( "05h38 1234567890" );
        assertThat( m.matches() )
                .isTrue();
        assertThat( PpidChecker.fromBusyboxHours( m ) ).isEqualTo( 3600 * 5L );
        assertThat( PpidChecker.fromBusyboxMinutes( m ) ).isEqualTo( 60 * 38L );
        assertThat( PpidChecker.fromBusyboxPID( m ) ).isEqualTo( "1234567890" );
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

    @Test
    public void shouldBeTypeNull()
    {
        assertThat( ProcessCheckerType.toEnum( null ) )
                .isNull();

        assertThat( ProcessCheckerType.toEnum( "   " ) )
                .isNull();

        assertThat( ProcessCheckerType.isValid( null ) )
                .isTrue();
    }

    @Test
    public void shouldBeException()
    {
        exceptions.expect( IllegalArgumentException.class );
        exceptions.expectMessage( "unknown process checker" );

        assertThat( ProcessCheckerType.toEnum( "anything else" ) )
                .isNull();
    }

    @Test
    public void shouldNotBeValid()
    {
        assertThat( ProcessCheckerType.isValid( "anything" ) )
                .isFalse();
    }

    @Test
    public void shouldBeTypePing()
    {
        assertThat( ProcessCheckerType.toEnum( "ping" ) )
                .isEqualTo( ProcessCheckerType.PING );

        assertThat( ProcessCheckerType.isValid( "ping" ) )
                .isTrue();

        assertThat( ProcessCheckerType.PING.getType() )
                .isEqualTo( "ping" );
    }

    @Test
    public void shouldBeTypeNative()
    {
        assertThat( ProcessCheckerType.toEnum( "native" ) )
                .isEqualTo( ProcessCheckerType.NATIVE );

        assertThat( ProcessCheckerType.isValid( "native" ) )
                .isTrue();

        assertThat( ProcessCheckerType.NATIVE.getType() )
                .isEqualTo( "native" );
    }

    @Test
    public void shouldBeTypeAll()
    {
        assertThat( ProcessCheckerType.toEnum( "all" ) )
                .isEqualTo( ProcessCheckerType.ALL );

        assertThat( ProcessCheckerType.isValid( "all" ) )
                .isTrue();

        assertThat( ProcessCheckerType.ALL.getType() )
                .isEqualTo( "all" );
    }

    private static long windowsProcessStartTime( PpidChecker checker )
    {
        return (long) checker.windows().getTime();
    }
}
