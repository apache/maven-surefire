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

import org.apache.maven.surefire.util.ReflectionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.StringTokenizer;

import static java.lang.Character.isDigit;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.SystemUtils.IS_OS_FREE_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_NET_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_OPEN_BSD;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeMethodChain;
import static org.apache.maven.surefire.util.ReflectionUtils.tryLoadClass;

/**
 * JDK 9 support.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class SystemUtils
{
    public static final BigDecimal JAVA_SPECIFICATION_VERSION = getJavaSpecificationVersion();

    private static final BigDecimal JIGSAW_JAVA_VERSION = new BigDecimal( 9 ).stripTrailingZeros();

    private static final int PROC_STATUS_PID_FIRST_CHARS = 20;

    private SystemUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    /**
     * @param jvmExecPath    e.g. /jdk/bin/java, /jdk/jre/bin/java
     * @return {@code true} if {@code jvmExecPath} is path to java binary executor
     */
    public static boolean endsWithJavaPath( String jvmExecPath )
    {
        File javaExec = new File( jvmExecPath ).getAbsoluteFile();
        File bin = javaExec.getParentFile();
        String exec = javaExec.getName();
        return exec.startsWith( "java" ) && bin != null && bin.getName().equals( "bin" );
    }

    /**
     * If {@code jvmExecutable} is <tt>/jdk/bin/java</tt> (since jdk9) or <tt>/jdk/jre/bin/java</tt> (prior to jdk9)
     * then the absolute path to JDK home is returned <tt>/jdk</tt>.
     * <br>
     * Null is returned if {@code jvmExecutable} is incorrect.
     *
     * @param jvmExecutable    /jdk/bin/java* or /jdk/jre/bin/java*
     * @return path to jdk directory; or <tt>null</tt> if wrong path or directory layout of JDK installation.
     */
    public static File toJdkHomeFromJvmExec( String jvmExecutable )
    {
        File bin = new File( jvmExecutable ).getAbsoluteFile().getParentFile();
        if ( "bin".equals( bin.getName() ) )
        {
            File parent = bin.getParentFile();
            if ( "jre".equals( parent.getName() ) )
            {
                File jdk = parent.getParentFile();
                return new File( jdk, "bin" ).isDirectory() ? jdk : null;
            }
            return parent;
        }
        return null;
    }

    /**
     * If system property <tt>java.home</tt> is <tt>/jdk</tt> (since jdk9) or <tt>/jdk/jre</tt> (prior to jdk9) then
     * the absolute path to
     * JDK home is returned <tt>/jdk</tt>.
     *
     * @return path to JDK
     */
    public static File toJdkHomeFromJre()
    {
        return toJdkHomeFromJre( System.getProperty( "java.home" ) );
    }

    /**
     * If {@code jreHome} is <tt>/jdk</tt> (since jdk9) or <tt>/jdk/jre</tt> (prior to jdk9) then
     * the absolute path to JDK home is returned <tt>/jdk</tt>.
     * <br>
     * JRE home directory {@code jreHome} must be taken from system property <tt>java.home</tt>.
     *
     * @param jreHome    path to /jdk or /jdk/jre
     * @return path to JDK
     */
    static File toJdkHomeFromJre( String jreHome )
    {
        File pathToJreOrJdk = new File( jreHome ).getAbsoluteFile();
        return "jre".equals( pathToJreOrJdk.getName() ) ? pathToJreOrJdk.getParentFile() : pathToJreOrJdk;
    }

    public static BigDecimal toJdkVersionFromReleaseFile( File jdkHome )
    {
        File release = new File( requireNonNull( jdkHome ).getAbsoluteFile(), "release" );
        if ( !release.isFile() )
        {
            return null;
        }
        Properties properties = new Properties();
        try ( InputStream is = new FileInputStream( release ) )
        {
            properties.load( is );
            String javaVersion = properties.getProperty( "JAVA_VERSION" ).replace( "\"", "" );
            StringTokenizer versions = new StringTokenizer( javaVersion, "._" );

            if ( versions.countTokens() == 1 )
            {
                javaVersion = versions.nextToken();
            }
            else if ( versions.countTokens() >= 2 )
            {
                String majorVersion = versions.nextToken();
                String minorVersion = versions.nextToken();
                javaVersion = isNumeric( minorVersion ) ? majorVersion + "." + minorVersion : majorVersion;
            }
            else
            {
                return null;
            }

            return new BigDecimal( javaVersion );
        }
        catch ( IOException e )
        {
            return null;
        }
    }

    /**
     * Safely extracts major and minor version as fractional number from
     * <pre>
     *     $MAJOR.$MINOR.$SECURITY
     * </pre>.
     * <br>
     *     The security version is usually not needed to know.
     *     It can be applied to not certified JRE.
     *
     * @return major.minor version derived from java specification version of <em>this</em> JVM, e.g. 1.8, 9, etc.
     */
    private static BigDecimal getJavaSpecificationVersion()
    {
        StringBuilder fractionalVersion = new StringBuilder( "0" );
        for ( char c : org.apache.commons.lang3.SystemUtils.JAVA_SPECIFICATION_VERSION.toCharArray() )
        {
            if ( isDigit( c ) )
            {
                fractionalVersion.append( c );
            }
            else if ( c == '.' )
            {
                if ( fractionalVersion.indexOf( "." ) == -1 )
                {
                    fractionalVersion.append( '.' );
                }
                else
                {
                    break;
                }
            }
        }
        String majorMinorVersion = fractionalVersion.toString();
        return new BigDecimal( majorMinorVersion.endsWith( "." ) ? majorMinorVersion + "0" : majorMinorVersion )
                .stripTrailingZeros();
    }

    public static boolean isJava9AtLeast( String jvmExecutablePath )
    {
        File externalJavaHome = toJdkHomeFromJvmExec( jvmExecutablePath );
        File thisJavaHome = toJdkHomeFromJre();
        if ( thisJavaHome.equals( externalJavaHome ) )
        {
            return isBuiltInJava9AtLeast();
        }
        else
        {
            BigDecimal releaseFileVersion =
                    externalJavaHome == null ? null : toJdkVersionFromReleaseFile( externalJavaHome );
            return isJava9AtLeast( releaseFileVersion );
        }
    }

    public static boolean isBuiltInJava9AtLeast()
    {
        return JAVA_SPECIFICATION_VERSION.compareTo( JIGSAW_JAVA_VERSION ) >= 0;
    }

    public static boolean isJava9AtLeast( BigDecimal version )
    {
        return version != null && version.compareTo( JIGSAW_JAVA_VERSION ) >= 0;
    }

    public static ClassLoader platformClassLoader()
    {
        if ( isBuiltInJava9AtLeast() )
        {
            return reflectClassLoader( ClassLoader.class, "getPlatformClassLoader" );
        }
        return null;
    }

    public static Long pid()
    {
        if ( isBuiltInJava9AtLeast() )
        {
            Long pid = pidOnJava9();
            if ( pid != null )
            {
                return pid;
            }
        }

        if ( IS_OS_LINUX )
        {
            try
            {
                return pidStatusOnLinux();
            }
            catch ( Exception e )
            {
                // examine PID via JMX
            }
        }
        else if ( IS_OS_FREE_BSD || IS_OS_NET_BSD || IS_OS_OPEN_BSD )
        {
            try
            {
                return pidStatusOnBSD();
            }
            catch ( Exception e )
            {
                // examine PID via JMX
            }
        }

        return pidOnJMX();
    }

    static Long pidOnJMX()
    {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if ( processName.contains( "@" ) )
        {
            String pid = processName.substring( 0, processName.indexOf( '@' ) ).trim();
            try
            {
                return Long.parseLong( pid );
            }
            catch ( NumberFormatException e )
            {
                return null;
            }
        }

        return null;
    }

    /**
     * $ cat /proc/self/stat
     * <br>
     * 48982 (cat) R 9744 48982 9744 34818 48982 8192 185 0 0 0 0 0 0 0 20 0 1 0
     * 137436614 103354368 134 18446744073709551615 4194304 4235780 140737488346592
     * 140737488343784 252896458544 0 0 0 0 0 0 0 17 2 0 0 0 0 0
     * <br>
     * $ SELF_PID=$(cat /proc/self/stat)
     * <br>
     * $ echo $CPU_ID | gawk '{print $1}'
     * <br>
     * 48982
     *
     * @return self PID
     * @throws Exception i/o and number format exc
     */
    static Long pidStatusOnLinux() throws Exception
    {
        return pidStatusOnLinux( "" );
    }

    /**
     * For testing purposes only.
     *
     * @param root    shifted to test-classes
     * @return same as in {@link #pidStatusOnLinux()}
     * @throws Exception same as in {@link #pidStatusOnLinux()}
     */
    static Long pidStatusOnLinux( String root ) throws Exception
    {
        try ( FileReader input = new FileReader( root + "/proc/self/stat" ) )
        {
            // Reading and encoding 20 characters is bit faster than whole line.
            // size of (long) = 19, + 1 space
            char[] buffer = new char[PROC_STATUS_PID_FIRST_CHARS];
            String startLine = new String( buffer, 0, input.read( buffer ) );
            return Long.parseLong( startLine.substring( 0, startLine.indexOf( ' ' ) ) );
        }
    }

    /**
     * The process status.  This file is read-only and returns a single
     * line containing multiple space-separated fields.
     * <br>
     * See <a href="https://www.freebsd.org/cgi/man.cgi?query=procfs&sektion=5">procfs status</a>
     * <br>
     * # cat /proc/curproc/status
     * <br>
     * cat 60424 60386 60424 60386 5,0 ctty 972854153,236415 0,0 0,1043 nochan 0 0 0,0 prisoner
     * <br>
     * Fields are:
     * <br>
     * comm pid ppid pgid sid maj, min ctty, sldr start user/system time wmsg euid ruid rgid,egid,
     * groups[1 .. NGROUPS] hostname
     *
     * @return current PID
     * @throws Exception if could not read /proc/curproc/status
     */
    static Long pidStatusOnBSD() throws Exception
    {
        return pidStatusOnBSD( "" );
    }

    /**
     * For testing purposes only.
     *
     * @param root    shifted to test-classes
     * @return same as in {@link #pidStatusOnBSD()}
     * @throws Exception same as in {@link #pidStatusOnBSD()}
     */
    static Long pidStatusOnBSD( String root ) throws Exception
    {
        try ( BufferedReader input = new BufferedReader( new FileReader( root + "/proc/curproc/status" ) ) )
        {
            String line = input.readLine();
            int i1 = 1 + line.indexOf( ' ' );
            int i2 = line.indexOf( ' ', i1 );
            return Long.parseLong( line.substring( i1, i2 ) );
        }
    }

    static Long pidOnJava9()
    {
        ClassLoader classLoader = currentThread().getContextClassLoader();
        Class<?> processHandle = tryLoadClass( classLoader, "java.lang.ProcessHandle" );
        Class<?>[] classesChain = { processHandle, processHandle };
        String[] methodChain = { "current", "pid" };
        return (Long) invokeMethodChain( classesChain, methodChain, null );
    }

    static ClassLoader reflectClassLoader( Class<?> target, String getterMethodName )
    {
        try
        {
            Method getter = ReflectionUtils.getMethod( target, getterMethodName );
            return (ClassLoader) ReflectionUtils.invokeMethodWithArray( null, getter );
        }
        catch ( RuntimeException e )
        {
            return null;
        }
    }
}
