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

import java.io.File;
import java.io.IOException;
import java.util.Queue;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.surefire.booter.ProcessInfo.ERR_PROCESS_INFO;
import static org.apache.maven.surefire.booter.ProcessInfo.INVALID_PROCESS_INFO;

/**
 * Recognizes PID of Plugin process and determines lifetime.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
final class PpidChecker
{
    private static final String WMIC_CREATION_DATE = "CreationDate";

    private final Queue<Process> destroyableCommands = new ConcurrentLinkedQueue<Process>();

    /**
     * The etime is in the form of [[dd-]hh:]mm:ss on Unix like systems.
     */
    static final Pattern UNIX_CMD_OUT_PATTERN = compile( "^(((\\d+)-)?(\\d{2}):)?(\\d{2}):(\\d{2})$" );

    private final long pluginPid;

    private volatile ProcessInfo pluginProcessInfo;
    private volatile boolean stopped;

    PpidChecker( long pluginPid )
    {
        this.pluginPid = pluginPid;
    }

    boolean canUse()
    {
        return pluginProcessInfo == null
                       ? IS_OS_WINDOWS || IS_OS_UNIX
                       : pluginProcessInfo.isValid() && !pluginProcessInfo.isError();
    }

    /**
     * This method can be called only after {@link #canUse()} has returned {@code true}.
     *
     * @return {@code true} if parent process is alive; {@code false} otherwise
     * @throws IllegalStateException if {@link #canUse()} returns {@code false}
     *                               or the object has been {@link #destroyActiveCommands() destroyed}
     */
    @SuppressWarnings( "unchecked" )
    boolean isProcessAlive()
    {
        if ( !canUse() )
        {
            throw new IllegalStateException( "irrelevant to call isProcessAlive()" );
        }

        if ( IS_OS_WINDOWS )
        {
            ProcessInfo previousPluginProcessInfo = pluginProcessInfo;
            pluginProcessInfo = windows();
            if ( isStopped() || pluginProcessInfo.isError() )
            {
                throw new IllegalStateException( "error to read process" );
            }
            // let's compare creation time, should be same unless killed or PID is reused by OS into another process
            return pluginProcessInfo.isValid()
                           && ( previousPluginProcessInfo == null
                                        || pluginProcessInfo.isTimeEqualTo( previousPluginProcessInfo ) );
        }
        else if ( IS_OS_UNIX )
        {
            ProcessInfo previousPluginProcessInfo = pluginProcessInfo;
            pluginProcessInfo = unix();
            if ( isStopped() || pluginProcessInfo.isError() )
            {
                throw new IllegalStateException( "error to read process" );
            }
            // let's compare elapsed time, should be greater or equal if parent process is the same and still alive
            return pluginProcessInfo.isValid()
                           && ( previousPluginProcessInfo == null
                                        || pluginProcessInfo.isTimeEqualTo( previousPluginProcessInfo )
                                        || pluginProcessInfo.isTimeAfter( previousPluginProcessInfo ) );
        }

        throw new IllegalStateException();
    }

    // https://www.freebsd.org/cgi/man.cgi?ps(1)
    // etimes elapsed running time, in decimal integer seconds

    // http://manpages.ubuntu.com/manpages/xenial/man1/ps.1.html
    // etimes elapsed time since the process was started, in seconds.

    // http://hg.openjdk.java.net/jdk7/jdk7/jdk/file/9b8c96f96a0f/test/java/lang/ProcessBuilder/Basic.java#L167
    ProcessInfo unix()
    {
        ProcessInfoConsumer reader = new ProcessInfoConsumer()
        {
            @Override
            ProcessInfo consumeLine( String line, ProcessInfo previousProcessInfo )
            {
                if ( !previousProcessInfo.isValid() )
                {
                    Matcher matcher = UNIX_CMD_OUT_PATTERN.matcher( line );
                    if ( matcher.matches() )
                    {
                        long pidUptime = fromDays( matcher )
                                                 + fromHours( matcher )
                                                 + fromMinutes( matcher )
                                                 + fromSeconds( matcher );
                        return ProcessInfo.unixProcessInfo( pluginPid, pidUptime );
                    }
                }
                return previousProcessInfo;
            }
        };

        return reader.execute( "/bin/sh", "-c", unixPathToPS() + " -o etime= -p " + pluginPid );
    }

    ProcessInfo windows()
    {
        ProcessInfoConsumer reader = new ProcessInfoConsumer()
        {
            private boolean hasHeader;

            @Override
            ProcessInfo consumeLine( String line, ProcessInfo previousProcessInfo )
            {
                if ( !previousProcessInfo.isValid() )
                {
                    StringTokenizer args = new StringTokenizer( line );
                    if ( args.countTokens() == 1 )
                    {
                        if ( hasHeader )
                        {
                            String startTimestamp = args.nextToken();
                            return ProcessInfo.windowsProcessInfo( pluginPid, startTimestamp );
                        }
                        else
                        {
                            hasHeader = WMIC_CREATION_DATE.equals( args.nextToken() );
                        }
                    }
                }
                return previousProcessInfo;
            }
        };
        String pid = String.valueOf( pluginPid );
        return reader.execute( "CMD", "/A", "/X", "/C",
                                     "wmic process where (ProcessId=" + pid + ") get " + WMIC_CREATION_DATE
        );
    }

    void destroyActiveCommands()
    {
        stopped = true;
        for ( Process p = destroyableCommands.poll(); p != null; p = destroyableCommands.poll() )
        {
            p.destroy();
        }
    }

    private boolean isStopped()
    {
        return stopped;
    }

    static String unixPathToPS()
    {
        return new File( "/usr/bin/ps" ).canExecute() ? "/usr/bin/ps" : "/bin/ps";
    }

    static long fromDays( Matcher matcher )
    {
        String s = matcher.group( 3 );
        return s == null ? 0L : DAYS.toSeconds( parseLong( s ) );
    }

    static long fromHours( Matcher matcher )
    {
        String s = matcher.group( 4 );
        return s == null ? 0L : HOURS.toSeconds( parseLong( s ) );
    }

    static long fromMinutes( Matcher matcher )
    {
        String s = matcher.group( 5 );
        return s == null ? 0L : MINUTES.toSeconds( parseLong( s ) );
    }

    static long fromSeconds( Matcher matcher )
    {
        String s = matcher.group( 6 );
        return s == null ? 0L : parseLong( s );
    }

    private static void checkValid( Scanner scanner )
            throws IOException
    {
        IOException exception = scanner.ioException();
        if ( exception != null )
        {
            throw exception;
        }
    }

    /**
     * Reads standard output from {@link Process}.
     * <br>
     * The artifact maven-shared-utils has non-daemon Threads which is an issue in Surefire to satisfy System.exit.
     * This implementation is taylor made without using any Thread.
     * It's easy to destroy Process from other Thread.
     */
    private abstract class ProcessInfoConsumer
    {
        abstract ProcessInfo consumeLine( String line, ProcessInfo previousProcessInfo );

        ProcessInfo execute( String... command )
        {
            ProcessBuilder processBuilder = new ProcessBuilder( command );
            processBuilder.redirectErrorStream( true );
            Process process = null;
            ProcessInfo processInfo = INVALID_PROCESS_INFO;
            try
            {
                process = processBuilder.start();
                destroyableCommands.add( process );
                Scanner scanner = new Scanner( process.getInputStream() );
                while ( scanner.hasNextLine() )
                {
                    String line = scanner.nextLine().trim();
                    processInfo = consumeLine( line, processInfo );
                }
                checkValid( scanner );
                int exitCode = process.waitFor();
                return exitCode == 0 ? processInfo : INVALID_PROCESS_INFO;
            }
            catch ( IOException e )
            {
                return ERR_PROCESS_INFO;
            }
            catch ( InterruptedException e )
            {
                return ERR_PROCESS_INFO;
            }
            finally
            {
                if ( process != null )
                {
                    destroyableCommands.remove( process );
                    process.destroy();
                    closeQuietly( process.getInputStream() );
                    closeQuietly( process.getErrorStream() );
                    closeQuietly( process.getOutputStream() );
                }
            }
        }
    }

}
