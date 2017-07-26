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
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.JavaVersion.JAVA_9;
import static org.apache.commons.lang3.JavaVersion.JAVA_RECENT;
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
    private static final int PROC_STATUS_PID_FIRST_CHARS = 20;

    public SystemUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
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
