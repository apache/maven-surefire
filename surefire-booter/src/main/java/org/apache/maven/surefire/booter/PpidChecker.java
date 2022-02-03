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

import org.apache.maven.surefire.api.booter.DumpErrorSingleton;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.join;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.readAllBytes;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.regex.Pattern.compile;
import static org.apache.maven.surefire.shared.io.IOUtils.closeQuietly;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isNotBlank;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_HP_UX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.surefire.booter.ProcessInfo.unixProcessInfo;
import static org.apache.maven.surefire.booter.ProcessInfo.windowsProcessInfo;
import static org.apache.maven.surefire.booter.ProcessInfo.ERR_PROCESS_INFO;
import static org.apache.maven.surefire.booter.ProcessInfo.INVALID_PROCESS_INFO;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;

/**
 * Recognizes PID of Plugin process and determines lifetime.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
final class PpidChecker
{
    private static final long MINUTES_TO_MILLIS = 60L * 1000L;
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
    private static final String PS_ETIME_HEADER = "ELAPSED";
    private static final String PS_PID_HEADER = "PID";

    private final Queue<Process> destroyableCommands = new ConcurrentLinkedQueue<>();

    /**
     * The etime is in the form of [[dd-]hh:]mm:ss on Unix like systems.
     * See the workaround https://issues.apache.org/jira/browse/SUREFIRE-1451.
     */
    static final Pattern UNIX_CMD_OUT_PATTERN = compile( "^(((\\d+)-)?(\\d{1,2}):)?(\\d{1,2}):(\\d{1,2})\\s+(\\d+)$" );

    static final Pattern BUSYBOX_CMD_OUT_PATTERN = compile( "^(\\d+)[hH](\\d{1,2})\\s+(\\d+)$" );

    private final String ppid;

    private volatile ProcessInfo parentProcessInfo;
    private volatile boolean stopped;

    PpidChecker( @Nonnull String ppid )
    {
        this.ppid = ppid;
    }

    boolean canUse()
    {
        if ( isStopped() )
        {
            return false;
        }
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
    boolean isProcessAlive()
    {
        if ( !canUse() )
        {
            throw new IllegalStateException( "irrelevant to call isProcessAlive()" );
        }

        final ProcessInfo previousInfo = parentProcessInfo;
        if ( IS_OS_WINDOWS )
        {
            parentProcessInfo = windows();
            checkProcessInfo();

            // let's compare creation time, should be same unless killed or PID is reused by OS into another process
            return !parentProcessInfo.isInvalid()
                    && ( previousInfo == null || parentProcessInfo.isTimeEqualTo( previousInfo ) );
        }
        else if ( IS_OS_UNIX )
        {
            parentProcessInfo = unix();
            checkProcessInfo();

            // let's compare elapsed time, should be greater or equal if parent process is the same and still alive
            return !parentProcessInfo.isInvalid()
                    && ( previousInfo == null || !parentProcessInfo.isTimeBefore( previousInfo ) );
        }
        parentProcessInfo = ERR_PROCESS_INFO;
        throw new IllegalStateException( "unknown platform or you did not call canUse() before isProcessAlive()" );
    }

    private void checkProcessInfo()
    {
        if ( isStopped() )
        {
            throw new IllegalStateException( "error [STOPPED] to read process " + ppid );
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
            @Nonnull
            ProcessInfo consumeLine( String line, ProcessInfo previousOutputLine )
            {
                if ( previousOutputLine.isInvalid() )
                {
                    if ( hasHeader )
                    {
                        Matcher matcher = UNIX_CMD_OUT_PATTERN.matcher( line );
                        if ( matcher.matches() && ppid.equals( fromPID( matcher ) ) )
                        {
                            long pidUptime = fromDays( matcher )
                                                     + fromHours( matcher )
                                                     + fromMinutes( matcher )
                                                     + fromSeconds( matcher );
                            return unixProcessInfo( ppid, pidUptime );
                        }
                        matcher = BUSYBOX_CMD_OUT_PATTERN.matcher( line );
                        if ( matcher.matches() && ppid.equals( fromBusyboxPID( matcher ) ) )
                        {
                            long pidUptime = fromBusyboxHours( matcher ) + fromBusyboxMinutes( matcher );
                            return unixProcessInfo( ppid, pidUptime );
                        }
                    }
                    else
                    {
                        hasHeader = line.contains( PS_ETIME_HEADER ) && line.contains( PS_PID_HEADER );
                    }
                }
                return previousOutputLine;
            }
        };
        String cmd = unixPathToPS() + " -o etime,pid " + ( IS_OS_LINUX ? "" : "-p " ) + ppid;
        return reader.execute( "/bin/sh", "-c", cmd );
    }

    ProcessInfo windows()
    {
        ProcessInfoConsumer reader = new ProcessInfoConsumer( "US-ASCII" )
        {
            @Override
            @Nonnull
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

    boolean isStopped()
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
        try
        {
            return new File( "/usr/bin/ps" ).canExecute();
        }
        catch ( SecurityException e )
        {
            return false;
        }
    }

    private static boolean canExecuteStandardUnixPs()
    {
        try
        {
            return new File( "/bin/ps" ).canExecute();
        }
        catch ( SecurityException e )
        {
            return false;
        }
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

    static String fromPID( Matcher matcher )
    {
        return matcher.group( 7 );
    }

    static long fromBusyboxHours( Matcher matcher )
    {
        String s = matcher.group( 1 );
        return s == null ? 0L : HOURS.toSeconds( parseLong( s ) );
    }

    static long fromBusyboxMinutes( Matcher matcher )
    {
        String s = matcher.group( 2 );
        return s == null ? 0L : MINUTES.toSeconds( parseLong( s ) );
    }

    static String fromBusyboxPID( Matcher matcher )
    {
        return matcher.group( 3 );
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

    public void stop()
    {
        stopped = true;
    }

    /**
     * Reads standard output from {@link Process}.
     * <br>
     * The artifact maven-shared-utils has non-daemon Threads which is an issue in Surefire to satisfy System.exit.
     * This implementation is taylor made without using any Thread.
     * It's easy to destroy Process from other Thread.
     */
    abstract class ProcessInfoConsumer
    {
        private final String charset;

        boolean hasHeader;

        ProcessInfoConsumer( String charset )
        {
            this.charset = charset;
        }

        abstract @Nonnull ProcessInfo consumeLine( String line, ProcessInfo previousProcessInfo ) throws Exception;

        ProcessInfo execute( String... command )
        {
            ProcessBuilder processBuilder = new ProcessBuilder( command );
            Process process = null;
            ProcessInfo processInfo = INVALID_PROCESS_INFO;
            StringBuilder out = new StringBuilder( 64 );
            out.append( join( " ", command ) )
                .append( NL );
            Path stdErr = null;
            try
            {
                stdErr = createTempFile( "surefire", null );
                processBuilder.redirectError( stdErr.toFile() );
                if ( IS_OS_HP_UX ) // force to run shell commands in UNIX Standard mode on HP-UX
                {
                    processBuilder.environment().put( "UNIX95", "1" );
                }
                process = processBuilder.start();
                destroyableCommands.add( process );
                Scanner scanner = new Scanner( process.getInputStream(), charset );
                while ( scanner.hasNextLine() )
                {
                    String line = scanner.nextLine();
                    out.append( line ).append( NL );
                    processInfo = consumeLine( line.trim(), processInfo );
                }
                checkValid( scanner );
                int exitCode = process.waitFor();
                boolean isError = Thread.interrupted() || isStopped();
                if ( exitCode != 0 || isError )
                {
                    out.append( "<<exit>> <<" ).append( exitCode ).append( ">>" )
                        .append( NL )
                        .append( "<<stopped>> <<" ).append( isStopped() ).append( ">>" );
                    DumpErrorSingleton.getSingleton()
                            .dumpText( out.toString() );
                }

                return isError ? ERR_PROCESS_INFO : ( exitCode == 0 ? processInfo : INVALID_PROCESS_INFO );
            }
            catch ( Exception e )
            {
                if ( !( e instanceof InterruptedException || e instanceof InterruptedIOException
                    || e.getCause() instanceof InterruptedException ) )
                {
                    DumpErrorSingleton.getSingleton()
                            .dumpText( out.toString() );

                    DumpErrorSingleton.getSingleton()
                            .dumpException( e );
                }

                //noinspection ResultOfMethodCallIgnored
                Thread.interrupted();

                return ERR_PROCESS_INFO;
            }
            finally
            {
                if ( process != null )
                {
                    destroyableCommands.remove( process );
                    closeQuietly( process.getInputStream() );
                    closeQuietly( process.getErrorStream() );
                    closeQuietly( process.getOutputStream() );
                }

                if ( stdErr != null )
                {
                    try
                    {
                        String error = new String( readAllBytes( stdErr ) ).trim();
                        if ( !error.isEmpty() )
                        {
                            DumpErrorSingleton.getSingleton()
                                .dumpText( error );
                        }
                        delete( stdErr );
                    }
                    catch ( IOException e )
                    {
                        // cannot do anything about it, the dump file writes would fail as well
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        String args = "ppid=" + ppid
                + ", stopped=" + stopped;

        ProcessInfo processInfo = parentProcessInfo;
        if ( processInfo != null )
        {
            args += ", invalid=" + processInfo.isInvalid()
                    + ", error=" + processInfo.isError();
        }

        if ( IS_OS_UNIX )
        {
            args += ", canExecuteLocalUnixPs=" + canExecuteLocalUnixPs()
                    + ", canExecuteStandardUnixPs=" + canExecuteStandardUnixPs();
        }

        return "PpidChecker{" + args + '}';
    }
}
