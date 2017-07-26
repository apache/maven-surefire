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

import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.apache.commons.lang3.JavaVersion.JAVA_9;
import static org.apache.commons.lang3.JavaVersion.JAVA_RECENT;
import static org.apache.commons.lang3.SystemUtils.IS_OS_FREE_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_NET_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_OPEN_BSD;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Test of {@link SystemUtils}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class SystemUtilsTest
{
    @Test
    public void shouldBePlatformClassLoader()
    {
        ClassLoader cl = SystemUtils.platformClassLoader();
        if ( JAVA_RECENT.atLeast( JAVA_9 ) )
        {
            assertThat( cl ).isNotNull();
        }
        else
        {
            assertThat( cl ).isNull();
        }
    }

    @Test
    public void shouldNotFindClassLoader()
    {
        ClassLoader cl = SystemUtils.reflectClassLoader( getClass(), "_getPlatformClassLoader_" );
        assertThat( cl ).isNull();
    }

    @Test
    public void shouldFindClassLoader()
    {
        ClassLoader cl = SystemUtils.reflectClassLoader( getClass(), "getPlatformClassLoader" );
        assertThat( cl ).isSameAs( ClassLoader.getSystemClassLoader() );
    }

    @Test
    public void shouldBePidOnJigsaw()
    {
        assumeTrue( JAVA_RECENT.atLeast( JAVA_9 ) );

        Long actualPid = SystemUtils.pidOnJava9();
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();

        assertThat( actualPid + "" )
                .isEqualTo( expectedPid );
    }

    @Test
    public void shouldBePidStatusOnLinux() throws Exception
    {
        assumeTrue( IS_OS_LINUX );

        Long actualPid = SystemUtils.pidStatusOnLinux();
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();

        assertThat( actualPid + "" )
                .isEqualTo( expectedPid );
    }

    @Test
    public void shouldBePidStatusOnBSD() throws Exception
    {
        assumeTrue( IS_OS_FREE_BSD || IS_OS_NET_BSD || IS_OS_OPEN_BSD );

        Long actualPid = SystemUtils.pidStatusOnBSD();
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();

        assertThat( actualPid + "" )
                .isEqualTo( expectedPid );
    }

    @Test
    public void shouldBePidOnJMX()
    {
        Long actualPid = SystemUtils.pidOnJMX();
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();

        assertThat( actualPid + "" )
                .isEqualTo( expectedPid );
    }

    @Test
    public void shouldBePid()
    {
        Long actualPid = SystemUtils.pid();
        String expectedPid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0].trim();

        assertThat( actualPid + "" )
                .isEqualTo( expectedPid );
    }

    public static ClassLoader getPlatformClassLoader()
    {
        return ClassLoader.getSystemClassLoader();
    }
}
