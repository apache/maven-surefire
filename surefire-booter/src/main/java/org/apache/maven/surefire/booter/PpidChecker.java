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

import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.StreamConsumer;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isDigit;
import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.shared.utils.cli.CommandLineUtils.executeCommandLine;
import static org.apache.maven.surefire.booter.ProcessInfo.INVALID_PROCESS_INFO;

/**
 * Recognizes PPID. Determines lifetime of parent process.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
final class PpidChecker
{
    private static final String WMIC_CL = "CommandLine";

    private static final String WMIC_PID = "ProcessId";

    private static final String WMIC_PPID = "ParentProcessId";

    private static final String WMIC_CREATION_DATE = "CreationDate";

    private static final String WINDOWS_CMD =
            "wmic process where (ProcessId=%s) get " + WMIC_CREATION_DATE + "," + WMIC_PPID;

    private static final String[] WINDOWS_PID_CMD =
            { "wmic", "process", "where", "(Name='java.exe')", "get", WMIC_PID, ",", WMIC_CL };

    private static final String UNIX_CMD1 = "/usr/bin/ps -o etime= -p $PPID";

    private static final String UNIX_CMD2 = "/bin/ps -o etime= -p $PPID";

    /**
     * etime is in the form of [[dd-]hh:]mm:ss
     */
    static final Pattern UNIX_CMD_OUT_PATTERN = compile( "^(((\\d+)-)?(\\d{2}):)?(\\d{2}):(\\d{2})$" );

    private static final Pattern NUMBER_PATTERN = compile( "^\\d+$" );

    static volatile String uniqueCommandLineToken;

    private final ProcessInfo parentProcessInfo;

    PpidChecker()
    {
        ProcessInfo parentProcess = INVALID_PROCESS_INFO;
        if ( IS_OS_WINDOWS )
        {
            String pid = pid();
            if ( pid == null && uniqueCommandLineToken != null )
            {
                pid = pidOnWindows();
            }

            if ( pid != null )
            {
                ProcessInfo currentProcessInfo = windows( pid );
                String ppid = currentProcessInfo.getPPID();
                parentProcess = currentProcessInfo.isValid() ? windows( ppid ) : INVALID_PROCESS_INFO;
            }
        }
        else if ( IS_OS_UNIX )
        {
            parentProcess = unix();
        }
        parentProcessInfo = parentProcess.isValid() ? parentProcess : INVALID_PROCESS_INFO;
    }

    boolean canUse()
    {
        return parentProcessInfo.isValid();
    }

    @SuppressWarnings( "unchecked" )
    boolean isParentProcessAlive()
    {
        if ( !canUse() )
        {
            throw new IllegalStateException();
        }

        if ( IS_OS_WINDOWS )
        {
            ProcessInfo pp = windows( parentProcessInfo.getPID() );
            // let's compare creation time, should be same unless killed or PPID is reused by OS into another process
            return pp.isValid() && pp.getTime().compareTo( parentProcessInfo.getTime() ) == 0;
        }
        else if ( IS_OS_UNIX )
        {
            ProcessInfo pp = unix();
            // let's compare elapsed time, should be greater or equal if parent process is the same and still alive
            return pp.isValid() && pp.getTime().compareTo( parentProcessInfo.getTime() ) >= 0;
        }

        throw new IllegalStateException();
    }

    // https://www.freebsd.org/cgi/man.cgi?ps(1)
    // etimes elapsed running time, in decimal integer seconds

    // http://manpages.ubuntu.com/manpages/xenial/man1/ps.1.html
    // etimes elapsed time since the process was started, in seconds.

    // http://hg.openjdk.java.net/jdk7/jdk7/jdk/file/9b8c96f96a0f/test/java/lang/ProcessBuilder/Basic.java#L167
    static ProcessInfo unix()
    {
        Commandline cli = new Commandline();
        cli.getShell().setQuotedArgumentsEnabled( false );
        cli.createArg().setLine( new File( "/usr/bin/ps" ).canExecute() ? UNIX_CMD1 : UNIX_CMD2 );
        final AtomicReference<ProcessInfo> processInfo = new AtomicReference<ProcessInfo>( INVALID_PROCESS_INFO );
        try
        {
            final int exitCode = executeCommandLine( cli, new StreamConsumer()
                    {
                        @Override
                        public void consumeLine( String line )
                        {
                            if ( processInfo.get().isValid() )
                            {
                                return;
                            }
                            line = line.trim();
                            if ( !line.isEmpty() )
                            {
                                Matcher matcher = UNIX_CMD_OUT_PATTERN.matcher( line );
                                if ( matcher.matches() )
                                {
                                    long pidUptime = fromDays( matcher )
                                                             + fromHours( matcher )
                                                             + fromMinutes( matcher )
                                                             + fromSeconds( matcher );
                                    processInfo.set( ProcessInfo.unixProcessInfo( pidUptime ) );
                                }
                            }
                        }
                    }, null
            );
            return exitCode == 0 ? processInfo.get() : INVALID_PROCESS_INFO;
        }
        catch ( CommandLineException e )
        {
            return INVALID_PROCESS_INFO;
        }
    }

    static String pid()
    {
        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if ( processName != null && processName.contains( "@" ) )
        {
            String pid = processName.substring( 0, processName.indexOf( '@' ) ).trim();
            if ( NUMBER_PATTERN.matcher( pid ).matches() )
            {
                return pid;
            }
        }
        return null;
    }

    static String pidOnWindows()
    {
        final AtomicReference<String> pid = new AtomicReference<String>();
        Commandline cli = new Commandline();
        cli.getShell().setQuotedArgumentsEnabled( false );
        cli.addArguments( WINDOWS_PID_CMD );
        try
        {
            final int exitCode = executeCommandLine( cli, new StreamConsumer()
                    {
                        private boolean hasHeader;
                        private boolean isPidFirst;

                        @Override
                        public void consumeLine( String line )
                        {
                            line = line.trim();
                            if ( line.isEmpty() )
                            {
                                return;
                            }

                            if ( hasHeader )
                            {
                                if ( line.contains( uniqueCommandLineToken ) )
                                {
                                    String extractedPid =
                                            isPidFirst ? extractNumberFromBegin( line ) : extractNumberFromEnd( line );
                                    pid.set( extractedPid );
                                }
                            }
                            else
                            {
                                StringTokenizer args = new StringTokenizer( line );
                                if ( args.countTokens() == 2 )
                                {
                                    String arg0 = args.nextToken();
                                    String arg1 = args.nextToken();
                                    isPidFirst = WMIC_PID.equals( arg0 );
                                    hasHeader = WMIC_PID.equals( arg0 ) || WMIC_CL.equals( arg0 );
                                    hasHeader &= WMIC_PID.equals( arg1 ) || WMIC_CL.equals( arg1 );
                                }
                            }
                        }
                    }, null
            );
            return exitCode == 0 ? pid.get() : null;
        }
        catch ( CommandLineException e )
        {
            return null;
        }
    }

    static ProcessInfo windows( final String pid )
    {
        Commandline cli = new Commandline();
        cli.getShell().setQuotedArgumentsEnabled( false );
        cli.createArg().setLine( String.format( Locale.ROOT, WINDOWS_CMD, pid ) );

        final AtomicReference<ProcessInfo> processInfo = new AtomicReference<ProcessInfo>( INVALID_PROCESS_INFO );
        try
        {
            final int exitCode = executeCommandLine( cli, new StreamConsumer()
                    {
                        private boolean hasHeader;
                        private boolean isStartTimestampFirst;

                        @Override
                        public void consumeLine( String line )
                        {
                            if ( processInfo.get().isValid() )
                            {
                                return;
                            }

                            line = line.trim();

                            if ( line.isEmpty() )
                            {
                                return;
                            }

                            if ( hasHeader )
                            {
                                StringTokenizer args = new StringTokenizer( line );
                                if ( args.countTokens() == 2 )
                                {
                                    if ( isStartTimestampFirst )
                                    {
                                        String startTimestamp = args.nextToken();
                                        String ppid = args.nextToken();
                                        processInfo.set( ProcessInfo.windowsProcessInfo( pid, startTimestamp, ppid ) );
                                    }
                                    else
                                    {
                                        String ppid = args.nextToken();
                                        String startTimestamp = args.nextToken();
                                        processInfo.set( ProcessInfo.windowsProcessInfo( pid, startTimestamp, ppid ) );
                                    }
                                }
                            }
                            else
                            {
                                StringTokenizer args = new StringTokenizer( line );
                                if ( args.countTokens() == 2 )
                                {
                                    String arg0 = args.nextToken();
                                    String arg1 = args.nextToken();
                                    isStartTimestampFirst = WMIC_CREATION_DATE.equals( arg0 );
                                    hasHeader = WMIC_CREATION_DATE.equals( arg0 ) || WMIC_PPID.equals( arg0 );
                                    hasHeader &= WMIC_CREATION_DATE.equals( arg1 ) || WMIC_PPID.equals( arg1 );
                                }
                            }
                        }
                    }, null
            );
            return exitCode == 0 ? processInfo.get() : INVALID_PROCESS_INFO;
        }
        catch ( CommandLineException e )
        {
            return INVALID_PROCESS_INFO;
        }
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

    static String extractNumberFromBegin( String line )
    {
        StringBuilder number = new StringBuilder();
        for ( int i = 0, len = line.length(); i < len; i++ )
        {
            char c = line.charAt( i );
            if ( isDigit( c ) )
            {
                number.append( c );
            }
            else
            {
                break;
            }
        }
        return number.toString();
    }

    static String extractNumberFromEnd( String line )
    {
        StringBuilder number = new StringBuilder();
        for ( int i = line.length() - 1; i >= 0; i-- )
        {
            char c = line.charAt( i );
            if ( isDigit( c ) )
            {
                number.insert( 0, c );
            }
            else
            {
                break;
            }
        }
        return number.toString();
    }
}
