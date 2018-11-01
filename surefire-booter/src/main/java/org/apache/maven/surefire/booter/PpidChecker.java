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
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.SystemUtils.IS_OS_HP_UX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.surefire.booter.ProcessInfo.unixProcessInfo;
import static org.apache.maven.surefire.booter.ProcessInfo.windowsProcessInfo;
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
    private static final int MINUTES_TO_MILLIS = 60 * 1000;
    // 25 chars https://superuser.com/questions/937380/get-creation-time-of-file-in-milliseconds/937401#937401
    private static final int WMIC_CREATION_DATE_VALUE_LENGTH = 25;
    private static final int WMIC_CREATION_DATE_TIMESTAMP_LENGTH = 18;
    private static final SimpleDateFormat WMIC_CREATION_DATE_FORMAT =
            IS_OS_WINDOWS ? createWindowsCreationDateFormat() : null;
    private static final String WMIC_CREATION_DATE = "CreationDate";
    private static final String WINDOWS_SYSTEM_ROOT_ENV = "SystemRoot";
    private static final String RELATIVE_PATH_TO_WMIC = "System32\\Wbem";
    private static final String SYSTEM_PATH_TO_WMIC =
            "%" + WINDOWS_SYSTEM_ROOT_ENV + "%\\" + RELATIVE_PATH_TO_WMIC + "\\";

    private final Queue<Process> destroyableCommands = new ConcurrentLinkedQueue<>();

    /**
     * The etime is in the form of [[dd-]hh:]mm:ss on Unix like systems.
     * See the workaround https://issues.apache.org/jira/browse/SUREFIRE-1451.
     */
    static final Pattern UNIX_CMD_OUT_PATTERN = compile( "^(((\\d+)-)?(\\d{1,2}):)?(\\d{1,2}):(\\d{1,2})$" );

    private final long ppid;

    private volatile ProcessInfo parentProcessInfo;
    private volatile boolean stopped;

    PpidChecker( long ppid )
    {
        this.ppid = ppid;
        //todo WARN logger (after new logger is finished) that (IS_OS_UNIX && canExecuteUnixPs()) is false
    }

    boolean canUse()
    {
        final ProcessInfo ppi = parentProcessInfo;
        return ppi == null ? IS_OS_WINDOWS || IS_OS_UNIX && canExecuteUnixPs() : ppi.canUse();
    }

    /**
     * This method can be called only after {@link #canUse()} has returned {@code true}.
     *
     * @return {@code true} if parent process is alive; {@code false} otherwise
     * @throws IllegalStateException if {@link #canUse()} returns {@code false}, error to read process
     *                               or this object has been {@link #destroyActiveCommands() destroyed}
     * @throws NullPointerException if extracted e-time is null
     */
    @SuppressWarnings( "unchecked" )
    boolean isProcessAlive()
    {
        if ( !canUse() )
        {
            throw new IllegalStateException( "irrelevant to call isProcessAlive()" );
        }

        final ProcessInfo previousInfo = parentProcessInfo;
        try
        {
            if ( IS_OS_WINDOWS )
            {
                parentProcessInfo = windows();
                checkProcessInfo();

                // let's compare creation time, should be same unless killed or PID is reused by OS into another process
                return previousInfo == null || parentProcessInfo.isTimeEqualTo( previousInfo );
            }
            else if ( IS_OS_UNIX )
            {
                parentProcessInfo = unix();
                checkProcessInfo();

                // let's compare elapsed time, should be greater or equal if parent process is the same and still alive
                return previousInfo == null || !parentProcessInfo.isTimeBefore( previousInfo );
            }

            throw new IllegalStateException( "unknown platform or you did not call canUse() before isProcessAlive()" );
        }
        finally
        {
            if ( parentProcessInfo == null )
            {
                parentProcessInfo = INVALID_PROCESS_INFO;
            }
        }
    }

    private void checkProcessInfo()
    {
        if ( isStopped() )
        {
            throw new IllegalStateException( "error [STOPPED] to read process " + ppid );
        }

        if ( parentProcessInfo.isError() )
        {
            throw new IllegalStateException( "error to read process " + ppid );
        }

        if ( !parentProcessInfo.canUse() )
        {
            throw new IllegalStateException( "Cannot use PPID " + ppid + " process information. "
                    + "Going to use NOOP events." );
        }
    }

    // https://www.freebsd.org/cgi/man.cgi?ps(1)
    // etimes elapsed running time, in decimal integer seconds

    // http://manpages.ubuntu.com/manpages/xenial/man1/ps.1.html
    // etimes elapsed time since the process was started, in seconds.

    // http://hg.openjdk.java.net/jdk7/jdk7/jdk/file/9b8c96f96a0f/test/java/lang/ProcessBuilder/Basic.java#L167
    ProcessInfo unix()
    {
        ProcessInfoConsumer reader = new ProcessInfoConsumer( Charset.defaultCharset().name() )
        {
            @Override
            ProcessInfo consumeLine( String line, ProcessInfo previousOutputLine )
            {
                if ( previousOutputLine.isInvalid() )
                {
                    Matcher matcher = UNIX_CMD_OUT_PATTERN.matcher( line );
                    if ( matcher.matches() )
                    {
                        long pidUptime = fromDays( matcher )
                                                 + fromHours( matcher )
                                                 + fromMinutes( matcher )
                                                 + fromSeconds( matcher );
                        return unixProcessInfo( ppid, pidUptime );
                    }
                }
                return previousOutputLine;
            }
        };

        return reader.execute( "/bin/sh", "-c", unixPathToPS() + " -o etime= -p " + ppid );
    }

    ProcessInfo windows()
    {
        ProcessInfoConsumer reader = new ProcessInfoConsumer( "US-ASCII" )
        {
            private boolean hasHeader;

            @Override
            ProcessInfo consumeLine( String line, ProcessInfo previousProcessInfo ) throws Exception
            {
                if ( previousProcessInfo.isInvalid() && !line.isEmpty() )
                {
                    if ( hasHeader )
                    {
                        // now the line is CreationDate, e.g. 20180406142327.741074+120
                        if ( line.length() != WMIC_CREATION_DATE_VALUE_LENGTH )
                        {
                            throw new IllegalStateException( "WMIC CreationDate should have 25 characters "
                                    + line );
                        }
                        String startTimestamp = line.substring( 0, WMIC_CREATION_DATE_TIMESTAMP_LENGTH );
                        int indexOfTimeZone = WMIC_CREATION_DATE_VALUE_LENGTH - 4;
                        long startTimestampMillisUTC =
                                WMIC_CREATION_DATE_FORMAT.parse( startTimestamp ).getTime()
                                        - parseInt( line.substring( indexOfTimeZone ) ) * MINUTES_TO_MILLIS;
                        return windowsProcessInfo( ppid, startTimestampMillisUTC );
                    }
                    else
                    {
                        hasHeader = WMIC_CREATION_DATE.equals( line );
                    }
                }
                return previousProcessInfo;
            }
        };
        String wmicPath = hasWmicStandardSystemPath() ? SYSTEM_PATH_TO_WMIC : "";
        return reader.execute( "CMD", "/A", "/X", "/C",
                wmicPath + "wmic process where (ProcessId=" + ppid + ") get " + WMIC_CREATION_DATE
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

    private static String unixPathToPS()
    {
        return canExecuteLocalUnixPs() ? "/usr/bin/ps" : "/bin/ps";
    }

    static boolean canExecuteUnixPs()
    {
        return canExecuteLocalUnixPs() || canExecuteStandardUnixPs();
    }

    private static boolean canExecuteLocalUnixPs()
    {
        return new File( "/usr/bin/ps" ).canExecute();
    }

    private static boolean canExecuteStandardUnixPs()
    {
        return new File( "/bin/ps" ).canExecute();
    }

    private static boolean hasWmicStandardSystemPath()
    {
        String systemRoot = System.getenv( WINDOWS_SYSTEM_ROOT_ENV );
        return isNotBlank( systemRoot ) && new File( systemRoot, RELATIVE_PATH_TO_WMIC + "\\wmic.exe" ).isFile();
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
     * The beginning part of Windows WMIC format yyyymmddHHMMSS.xxx <br>
     * https://technet.microsoft.com/en-us/library/ee198928.aspx <br>
     * We use UTC time zone which avoids DST changes, see SUREFIRE-1512.
     *
     * @return Windows WMIC format yyyymmddHHMMSS.xxx
     */
    private static SimpleDateFormat createWindowsCreationDateFormat()
    {
        SimpleDateFormat formatter = new SimpleDateFormat( "yyyyMMddHHmmss'.'SSS" );
        formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return formatter;
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
        private final String charset;

        ProcessInfoConsumer( String charset )
        {
            this.charset = charset;
        }

        abstract ProcessInfo consumeLine( String line, ProcessInfo previousProcessInfo ) throws Exception;

        ProcessInfo execute( String... command )
        {
            ProcessBuilder processBuilder = new ProcessBuilder( command );
            processBuilder.redirectErrorStream( true );
            Process process = null;
            ProcessInfo processInfo = INVALID_PROCESS_INFO;
            try
            {
                if ( IS_OS_HP_UX ) // force to run shell commands in UNIX Standard mode on HP-UX
                {
                    processBuilder.environment().put( "UNIX95", "1" );
                }
                process = processBuilder.start();
                destroyableCommands.add( process );
                Scanner scanner = new Scanner( process.getInputStream(), charset );
                while ( scanner.hasNextLine() )
                {
                    String line = scanner.nextLine().trim();
                    processInfo = consumeLine( line, processInfo );
                }
                checkValid( scanner );
                int exitCode = process.waitFor();
                return exitCode == 0 ? processInfo : INVALID_PROCESS_INFO;
            }
            catch ( Exception e )
            {
                DumpErrorSingleton.getSingleton()
                        .dumpException( e );

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
