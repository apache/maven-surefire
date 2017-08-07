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
import java.util.Properties;
import java.util.StringTokenizer;

import static java.lang.Thread.currentThread;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.JavaVersion.JAVA_9;
import static org.apache.commons.lang3.JavaVersion.JAVA_RECENT;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.SystemUtils.IS_OS_FREE_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_NET_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_OPEN_BSD;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeMethodChain;
import static org.apache.maven.surefire.util.ReflectionUtils.tryLoadClass;
import static org.apache.maven.surefire.util.internal.ObjectUtils.requireNonNull;

/**
 * JDK 9 support.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class SystemUtils
{
    private static final double JIGSAW_JAVA_VERSION = 9.0d;

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

    public static Double toJdkVersionFromReleaseFile( File jdkHome )
    {
        File release = new File( requireNonNull( jdkHome ).getAbsoluteFile(), "release" );
        if ( !release.isFile() )
        {
            return null;
        }
        InputStream is = null;
        try
        {
            Properties properties = new Properties();
            is = new FileInputStream( release );
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

            return Double.valueOf( javaVersion );
        }
        catch ( IOException e )
        {
            return null;
        }
        finally
        {
            closeQuietly( is );
        }
    }

    public static boolean isJava9AtLeast( String jvmExecutablePath )
    {
        File externalJavaHome = toJdkHomeFromJvmExec( jvmExecutablePath );
        File thisJavaHome = toJdkHomeFromJre();
        if ( thisJavaHome.equals( externalJavaHome ) )
        {
            return isBuiltInJava9AtLeast();
        }
        Double releaseFileVersion = externalJavaHome == null ? null : toJdkVersionFromReleaseFile( externalJavaHome );
        return SystemUtils.isJava9AtLeast( releaseFileVersion );
    }

    static boolean isBuiltInJava9AtLeast()
    {
        return isJavaVersionAtLeast( JAVA_9 );
    }

    public static boolean isJava9AtLeast( Double version )
    {
        return version != null && version >= JIGSAW_JAVA_VERSION;
    }

    public static ClassLoader platformClassLoader()
    {
        if ( JAVA_RECENT.atLeast( JAVA_9 ) )
        {
            return reflectClassLoader( ClassLoader.class, "getPlatformClassLoader" );
        }
        return null;
    }

    public static Long pid()
    {
        if ( JAVA_RECENT.atLeast( JAVA_9 ) )
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

    static Long pidStatusOnLinux() throws Exception
    {
        FileReader input = new FileReader( "/proc/self/stat" );
        try
        {
            // Reading and encoding 20 characters is bit faster than whole line.
            // size of (long) = 19, + 1 space
            char[] buffer = new char[PROC_STATUS_PID_FIRST_CHARS];
            String startLine = new String( buffer, 0, input.read( buffer ) );
            return Long.parseLong( startLine.substring( 0, startLine.indexOf( ' ' ) ) );
        }
        finally
        {
            input.close();
        }
    }

    /**
     * The process status.  This file is read-only and returns a single
     * line containing multiple space-separated fields.
     * See <a href="https://www.freebsd.org/cgi/man.cgi?query=procfs&sektion=5">procfs status</a>
     *
     * @return current PID
     * @throws Exception if could not read /proc/curproc/status
     */
    static Long pidStatusOnBSD() throws Exception
    {
        BufferedReader input = new BufferedReader( new FileReader( "/proc/curproc/status" ) );
        try
        {
            String line = input.readLine();
            int i1 = 1 + line.indexOf( ' ' );
            int i2 = line.indexOf( ' ', i1 );
            return Long.parseLong( line.substring( i1, i2 ) );
        }
        finally
        {
            input.close();
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
