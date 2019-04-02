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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;

import static java.io.File.separator;
import static org.apache.commons.lang3.JavaVersion.JAVA_9;
import static org.apache.commons.lang3.JavaVersion.JAVA_RECENT;
import static org.apache.commons.lang3.SystemUtils.IS_OS_FREE_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_NET_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_OPEN_BSD;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Test of {@link SystemUtils}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
@RunWith( Enclosed.class )
public class SystemUtilsTest
{
    public static class PlainUnitTests
    {

        @Test
        public void shouldMatchJavaSpecVersion() throws Exception
        {
            BigDecimal actual = invokeMethod( SystemUtils.class, "getJavaSpecificationVersion" );
            BigDecimal expected =
                    new BigDecimal( System.getProperty( "java.specification.version" ) ).stripTrailingZeros();
            assertThat( actual ).isEqualTo( expected );
            assertThat( SystemUtils.JAVA_SPECIFICATION_VERSION ).isEqualTo( expected );
        }

        @Test
        public void shouldParseProprietaryReleaseFile() throws IOException
        {
            String classes = new File( "." ).getCanonicalPath() + separator + "target" + separator + "test-classes";

            File path = new File( classes, "jdk8-IBM" + separator + "bin" + separator + "java" );
            assertThat( SystemUtils.isJava9AtLeast( path.getAbsolutePath() ) ).isFalse();

            path = new File( classes, "jdk8-oracle" + separator + "bin" + separator + "java" );
            assertThat( SystemUtils.isJava9AtLeast( path.getAbsolutePath() ) ).isFalse();

            path = new File( classes, "jdk9-oracle" + separator + "bin" + separator + "java" );
            assertThat( SystemUtils.isJava9AtLeast( path.getAbsolutePath() ) ).isTrue();
        }

        @Test
        public void incorrectJdkPath()
        {
            File jre = new File( System.getProperty( "java.home" ) );
            File jdk = jre.getParentFile();
            File incorrect = jdk.getParentFile();
            assertThat( SystemUtils.isJava9AtLeast( incorrect.getAbsolutePath() ) ).isFalse();
        }

        @Test
        public void shouldHaveJavaPath()
        {
            String javaPath = System.getProperty( "java.home" ) + separator + "bin" + separator + "java";
            assertThat( SystemUtils.endsWithJavaPath( javaPath ) ).isTrue();
        }

        @Test
        public void shouldNotHaveJavaPath()
        {
            assertThat( SystemUtils.endsWithJavaPath( "/jdk" ) ).isFalse();
        }

        @Test
        public void shouldNotExtractJdkHomeFromJavaExec()
        {
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec( "/jdk/binx/java" );
            assertThat( pathToJdk ).isNull();
        }

        @Test
        public void shouldExtractJdkHomeFromJavaExec()
        {
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec( "/jdk/bin/java" );
            assertThat( pathToJdk ).isEqualTo( new File( "/jdk" ).getAbsoluteFile() );
        }

        @Test
        public void shouldNotExtractJdkHomeFromJreExec() throws IOException
        {
            String classes = new File( "." ).getCanonicalPath() + separator + "target" + separator + "test-classes";
            File jdk = new File( classes, "jdk" );
            String pathToJreExec = jdk.getAbsolutePath() + separator + "jre" + separator + "binx" + separator + "java";
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec( pathToJreExec );
            assertThat( pathToJdk ).isNull();
        }

        @Test
        public void shouldExtractJdkHomeFromJreExec() throws IOException
        {
            String classes = new File( "." ).getCanonicalPath() + separator + "target" + separator + "test-classes";
            File jdk = new File( classes, "jdk" );
            String pathToJreExec = jdk.getAbsolutePath() + separator + "jre" + separator + "bin" + separator + "java";
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec( pathToJreExec );
            assertThat( pathToJdk ).isEqualTo( jdk );
        }

        @Test
        public void shouldExtractJdkHomeFromJre()
        {
            File pathToJdk = SystemUtils.toJdkHomeFromJre( "/jdk/jre" );
            assertThat( pathToJdk ).isEqualTo( new File( "/jdk" ).getAbsoluteFile() );
        }

        @Test
        public void shouldExtractJdkHomeFromJdk()
        {
            File pathToJdk = SystemUtils.toJdkHomeFromJre( "/jdk/" );
            assertThat( pathToJdk ).isEqualTo( new File( "/jdk" ).getAbsoluteFile() );
        }

        @Test
        public void shouldExtractJdkHomeFromRealPath()
        {
            File pathToJdk = SystemUtils.toJdkHomeFromJre();

            if ( JAVA_RECENT.atLeast( JAVA_9 ) )
            {
                File realJdkHome = new File( System.getProperty( "java.home" ) ).getAbsoluteFile();
                assertThat( realJdkHome ).isDirectory();
                assertThat( realJdkHome.getName() ).isNotEqualTo( "jre" );
                assertThat( pathToJdk ).isEqualTo( realJdkHome );
            }
            else
            {
                File realJreHome = new File( System.getProperty( "java.home" ) ).getAbsoluteFile();
                assertThat( realJreHome ).isDirectory();
                assertThat( realJreHome.getName() ).isEqualTo( "jre" );
                File realJdkHome = realJreHome.getParentFile();
                assertThat( pathToJdk ).isEqualTo( realJdkHome );
            }
        }

        @Test
        public void shouldBeJavaVersion()
        {
            assertThat( SystemUtils.isJava9AtLeast( (BigDecimal ) null ) ).isFalse();
            assertThat( SystemUtils.isJava9AtLeast( new BigDecimal( "1.8" ) ) ).isFalse();
            assertThat( SystemUtils.isJava9AtLeast( new BigDecimal( 9 ) ) ).isTrue();
        }

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
        public void shouldBeMockPidStatusOnLinux() throws Exception
        {
            String root = new File( System.getProperty( "user.dir" ), "target/test-classes" ).getAbsolutePath();
            Long actualPid = SystemUtils.pidStatusOnLinux( root );
            assertThat( actualPid )
                    .isEqualTo( 48982L );
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
        public void shouldBeMockPidStatusOnBSD() throws Exception
        {
            String root = new File( System.getProperty( "user.dir" ), "target/test-classes" ).getAbsolutePath();
            Long actualPid = SystemUtils.pidStatusOnBSD( root );
            assertThat( actualPid )
                    .isEqualTo( 60424L );
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

        @SuppressWarnings( "unused" )
        public static ClassLoader getPlatformClassLoader()
        {
            return ClassLoader.getSystemClassLoader();
        }

    }

    @RunWith( PowerMockRunner.class )
    @PrepareForTest( SystemUtils.class )
    @PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
    public static class MockTest
    {

        @Test
        public void shouldBeDifferentJdk9()
        {
            testIsJava9AtLeast( new File( System.getProperty( "java.home" ) ) );
        }

        @Test
        public void shouldBeSameJdk9()
        {
            // PowerMockJUnit44RunnerDelegateImpl does not work with Assumptions: assumeFalse
            if ( !JAVA_RECENT.atLeast( JAVA_9 ) )
            {
                testIsJava9AtLeast( new File( System.getProperty( "java.home" ) ).getParentFile() );
            }
        }

        private static void testIsJava9AtLeast( File pathInJdk )
        {
            File path = new File( pathInJdk, "bin" + separator + "java" );

            mockStatic( SystemUtils.class );

            when( SystemUtils.isJava9AtLeast( anyString() ) )
                    .thenCallRealMethod();

            when( SystemUtils.toJdkHomeFromJvmExec( anyString() ) )
                    .thenCallRealMethod();

            when( SystemUtils.toJdkHomeFromJre() )
                    .thenCallRealMethod();

            when( SystemUtils.toJdkHomeFromJre( anyString() ) )
                    .thenCallRealMethod();

            when( SystemUtils.isBuiltInJava9AtLeast() )
                    .thenCallRealMethod();

            when( SystemUtils.toJdkVersionFromReleaseFile( any( File.class ) ) )
                    .thenCallRealMethod();

            when( SystemUtils.isJava9AtLeast( any( BigDecimal.class ) ) )
                    .thenCallRealMethod();

            if ( JAVA_RECENT.atLeast( JAVA_9 ) )
            {
                assertThat( SystemUtils.isJava9AtLeast( path.getAbsolutePath() ) ).isTrue();
            }
            else
            {
                assertThat( SystemUtils.isJava9AtLeast( path.getAbsolutePath() ) ).isFalse();
            }

            verifyStatic( SystemUtils.class, times( 0 ) );
            SystemUtils.toJdkVersionFromReleaseFile( any( File.class ) );

            verifyStatic( SystemUtils.class, times( 1 ) );
            SystemUtils.isBuiltInJava9AtLeast();
        }
    }
}
