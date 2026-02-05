/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.booter;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.util.SureFireFileManager;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.join;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.readAllBytes;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.regex.Pattern.compile;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.apache.maven.surefire.booter.ProcessInfo.ERR_PROCESS_INFO;
import static org.apache.maven.surefire.booter.ProcessInfo.INVALID_PROCESS_INFO;
import static org.apache.maven.surefire.booter.ProcessInfo.unixProcessInfo;
import static org.apache.maven.surefire.booter.ProcessInfo.windowsProcessInfo;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isNotBlank;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_HP_UX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_UNIX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;

/**
 * Recognizes PID of Plugin process and determines lifetime.
 * <p>
 * This implementation uses native commands ({@code ps} on Unix, {@code wmic} on Windows)
 * to check the parent process status. On Java 9+, consider using {@code ProcessHandleChecker}
 * instead, which uses the Java {@code ProcessHandle} API and doesn't require spawning external processes.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 * @see ProcessChecker
 * @deprecated Use {@code ProcessHandleChecker} via {@link ProcessChecker#of(String)} instead
 */
@Deprecated
final class PpidChecker implements ProcessChecker {
    private static final long MINUTES_TO_MILLIS = 60L * 1000L;
    // 25 chars https://superuser.com/questions/937380/get-creation-time-of-file-in-milliseconds/937401#937401
    private static final int WMIC_CREATION_DATE_VALUE_LENGTH = 25;
    private static final int WMIC_CREATION_DATE_TIMESTAMP_LENGTH = 18;
    private static final SimpleDateFormat WMIC_CREATION_DATE_FORMAT =
            IS_OS_WINDOWS ? createWindowsCreationDateFormat() : null;
    private static final String WMIC_CREATION_DATE = "CreationDate";
    private static final String WINDOWS_SYSTEM_ROOT_ENV = "SystemRoot";
    private static final String RELATIVE_PATH_TO_POWERSHELL = "System32\\WindowsPowerShell\\v1.0";
    private static final String SYSTEM_PATH_TO_POWERSHELL =
            System.getenv(WINDOWS_SYSTEM_ROOT_ENV) + "\\" + RELATIVE_PATH_TO_POWERSHELL + "\\";
    private static final String PS_ETIME_HEADER = "ELAPSED";
    private static final String PS_PID_HEADER = "PID";

    private final Queue<Process> destroyableCommands = new ConcurrentLinkedQueue<>();

    /**
     * The etime is in the form of [[dd-]hh:]mm:ss on Unix like systems.
     * See the workaround https://issues.apache.org/jira/browse/SUREFIRE-1451.
     */
    static final Pattern UNIX_CMD_OUT_PATTERN = compile("^(((\\d+)-)?(\\d{1,2}):)?(\\d{1,2}):(\\d{1,2})\\s+(\\d+)$");

    static final Pattern BUSYBOX_CMD_OUT_PATTERN = compile("^(\\d+)[hH](\\d{1,2})\\s+(\\d+)$");

    private final String ppid;

    private volatile ProcessInfo parentProcessInfo;
    private volatile boolean stopped;

    PpidChecker(@Nonnull String ppid) {
        this.ppid = ppid;
    }

    @Override
    public boolean canUse() {
        if (isStopped()) {
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
    @Override
    public boolean isProcessAlive() {
        if (!canUse()) {
            throw new IllegalStateException("irrelevant to call isProcessAlive()");
        }

        final ProcessInfo previousInfo = parentProcessInfo;
        if (IS_OS_WINDOWS) {
            parentProcessInfo = windows();
            checkProcessInfo();

            // let's compare creation time, should be same unless killed or PID is reused by OS into another process
            return !parentProcessInfo.isInvalid()
                    && (previousInfo == null || parentProcessInfo.isTimeEqualTo(previousInfo));
        } else if (IS_OS_UNIX) {
            parentProcessInfo = unix();
            checkProcessInfo();

            // let's compare elapsed time, should be greater or equal if parent process is the same and still alive
            return !parentProcessInfo.isInvalid()
                    && (previousInfo == null || !parentProcessInfo.isTimeBefore(previousInfo));
        }
        parentProcessInfo = ERR_PROCESS_INFO;
        throw new IllegalStateException("unknown platform or you did not call canUse() before isProcessAlive()");
    }

    private void checkProcessInfo() {
        if (isStopped()) {
            throw new IllegalStateException("error [STOPPED] to read process " + ppid);
        }

        if (!parentProcessInfo.canUse()) {
            throw new IllegalStateException(
                    "Cannot use PPID " + ppid + " process information. " + "Going to use NOOP events.");
        }
    }

    // https://www.freebsd.org/cgi/man.cgi?ps(1)
    // etimes elapsed running time, in decimal integer seconds

    // http://manpages.ubuntu.com/manpages/xenial/man1/ps.1.html
    // etimes elapsed time since the process was started, in seconds.

    // http://hg.openjdk.java.net/jdk7/jdk7/jdk/file/9b8c96f96a0f/test/java/lang/ProcessBuilder/Basic.java#L167
    ProcessInfo unix() {
        String charset = System.getProperty("native.encoding", System.getProperty("file.encoding", "UTF-8"));
        ProcessInfoConsumer reader = new ProcessInfoConsumer(charset) {
            @Override
            @Nonnull
            ProcessInfo consumeLine(String line, ProcessInfo previousOutputLine) {
                if (previousOutputLine.isInvalid()) {
                    if (hasHeader) {
                        Matcher matcher = UNIX_CMD_OUT_PATTERN.matcher(line);
                        if (matcher.matches() && ppid.equals(fromPID(matcher))) {
                            long pidUptime = fromDays(matcher)
                                    + fromHours(matcher)
                                    + fromMinutes(matcher)
                                    + fromSeconds(matcher);
                            return unixProcessInfo(ppid, pidUptime);
                        }
                        matcher = BUSYBOX_CMD_OUT_PATTERN.matcher(line);
                        if (matcher.matches() && ppid.equals(fromBusyboxPID(matcher))) {
                            long pidUptime = fromBusyboxHours(matcher) + fromBusyboxMinutes(matcher);
                            return unixProcessInfo(ppid, pidUptime);
                        }
                    } else {
                        hasHeader = line.contains(PS_ETIME_HEADER) && line.contains(PS_PID_HEADER);
                    }
                }
                return previousOutputLine;
            }
        };
        String cmd = unixPathToPS() + " -o etime,pid " + (IS_OS_LINUX ? "" : "-p ") + ppid;
        return reader.execute("/bin/sh", "-c", cmd);
    }

    ProcessInfo windows() {
        ProcessInfoConsumer reader = new ProcessInfoConsumer("US-ASCII") {
            @Override
            @Nonnull
            ProcessInfo consumeLine(String line, ProcessInfo previousProcessInfo) throws Exception {
                if (previousProcessInfo.isInvalid() && !line.isEmpty()) {
                    // we still use WMIC output format even though we now use PowerShell to produce it
                    if (hasHeader) {
                        // now the line is CreationDate, e.g. 20180406142327.741074+120
                        if (line.length() != WMIC_CREATION_DATE_VALUE_LENGTH) {
                            throw new IllegalStateException("WMIC CreationDate should have 25 characters " + line);
                        }
                        String startTimestamp = line.substring(0, WMIC_CREATION_DATE_TIMESTAMP_LENGTH);
                        int indexOfTimeZone = WMIC_CREATION_DATE_VALUE_LENGTH - 4;
                        long startTimestampMillisUTC =
                                WMIC_CREATION_DATE_FORMAT.parse(startTimestamp).getTime()
                                        - parseInt(line.substring(indexOfTimeZone)) * MINUTES_TO_MILLIS;
                        return windowsProcessInfo(ppid, startTimestampMillisUTC);
                    } else {
                        hasHeader = WMIC_CREATION_DATE.equals(line);
                    }
                }
                return previousProcessInfo;
            }
        };

        String psPath = hasPowerShellStandardSystemPath() ? SYSTEM_PATH_TO_POWERSHELL : "";
        // mimic output format of the original check:
        // wmic process where (ProcessId=<ppid>) get CreationDate
        String psCommand = String.format(
                "Add-Type -AssemblyName System.Management; "
                        + "$p = Get-CimInstance Win32_Process -Filter 'ProcessId=%2$s'; "
                        + "if ($p) { "
                        + "    Write-Output '%1$s'; "
                        + "    [System.Management.ManagementDateTimeConverter]::ToDmtfDateTime($p.CreationDate) "
                        + "}",
                WMIC_CREATION_DATE, ppid);
        return reader.execute(psPath + "powershell", "-NoProfile", "-NonInteractive", "-Command", psCommand);
    }

    @Override
    public void destroyActiveCommands() {
        stopped = true;
        for (Process p = destroyableCommands.poll(); p != null; p = destroyableCommands.poll()) {
            p.destroy();
        }
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    private static String unixPathToPS() {
        return canExecuteLocalUnixPs() ? "/usr/bin/ps" : "/bin/ps";
    }

    static boolean canExecuteUnixPs() {
        return canExecuteLocalUnixPs() || canExecuteStandardUnixPs();
    }

    private static boolean canExecuteLocalUnixPs() {
        try {
            return new File("/usr/bin/ps").canExecute();
        } catch (SecurityException e) {
            return false;
        }
    }

    private static boolean canExecuteStandardUnixPs() {
        try {
            return new File("/bin/ps").canExecute();
        } catch (SecurityException e) {
            return false;
        }
    }

    private static boolean hasPowerShellStandardSystemPath() {
        String systemRoot = System.getenv(WINDOWS_SYSTEM_ROOT_ENV);
        return isNotBlank(systemRoot)
                && new File(systemRoot, RELATIVE_PATH_TO_POWERSHELL + "\\powershell.exe").isFile();
    }

    static long fromDays(Matcher matcher) {
        String s = matcher.group(3);
        return s == null ? 0L : DAYS.toSeconds(parseLong(s));
    }

    static long fromHours(Matcher matcher) {
        String s = matcher.group(4);
        return s == null ? 0L : HOURS.toSeconds(parseLong(s));
    }

    static long fromMinutes(Matcher matcher) {
        String s = matcher.group(5);
        return s == null ? 0L : MINUTES.toSeconds(parseLong(s));
    }

    static long fromSeconds(Matcher matcher) {
        String s = matcher.group(6);
        return s == null ? 0L : parseLong(s);
    }

    static String fromPID(Matcher matcher) {
        return matcher.group(7);
    }

    static long fromBusyboxHours(Matcher matcher) {
        String s = matcher.group(1);
        return s == null ? 0L : HOURS.toSeconds(parseLong(s));
    }

    static long fromBusyboxMinutes(Matcher matcher) {
        String s = matcher.group(2);
        return s == null ? 0L : MINUTES.toSeconds(parseLong(s));
    }

    static String fromBusyboxPID(Matcher matcher) {
        return matcher.group(3);
    }

    private static void checkValid(Scanner scanner) throws IOException {
        IOException exception = scanner.ioException();
        if (exception != null) {
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
    private static SimpleDateFormat createWindowsCreationDateFormat() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss'.'SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public ProcessInfo processInfo() {
        return parentProcessInfo;
    }

    /**
     * Reads standard output from {@link Process}.
     * <br>
     * The artifact maven-shared-utils has non-daemon Threads which is an issue in Surefire to satisfy System.exit.
     * This implementation is taylor made without using any Thread.
     * It's easy to destroy Process from other Thread.
     */
    abstract class ProcessInfoConsumer {
        private final String charset;

        boolean hasHeader;

        ProcessInfoConsumer(String charset) {
            this.charset = charset;
        }

        abstract @Nonnull ProcessInfo consumeLine(String line, ProcessInfo previousProcessInfo) throws Exception;

        ProcessInfo execute(String... command) {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = null;
            ProcessInfo processInfo = INVALID_PROCESS_INFO;
            StringBuilder out = new StringBuilder(64);
            out.append(join(" ", command)).append(NL);
            Path stdErr = null;
            try {
                stdErr = SureFireFileManager.createTempFile("surefire", null).toPath();

                processBuilder.redirectError(stdErr.toFile());
                if (IS_OS_HP_UX) // force to run shell commands in UNIX Standard mode on HP-UX
                {
                    processBuilder.environment().put("UNIX95", "1");
                }
                process = processBuilder.start();
                destroyableCommands.add(process);
                Scanner scanner = new Scanner(process.getInputStream(), charset);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    out.append(line).append(NL);
                    processInfo = consumeLine(line.trim(), processInfo);
                }
                checkValid(scanner);
                int exitCode = process.waitFor();
                boolean isError = Thread.interrupted() || isStopped();
                if (exitCode != 0 || isError) {
                    out.append("<<exit>> <<")
                            .append(exitCode)
                            .append(">>")
                            .append(NL)
                            .append("<<stopped>> <<")
                            .append(isStopped())
                            .append(">>");
                    DumpErrorSingleton.getSingleton().dumpText(out.toString());
                }

                return isError ? ERR_PROCESS_INFO : (exitCode == 0 ? processInfo : INVALID_PROCESS_INFO);
            } catch (Exception e) {
                if (!(e instanceof InterruptedException
                        || e instanceof InterruptedIOException
                        || e.getCause() instanceof InterruptedException)) {
                    DumpErrorSingleton.getSingleton().dumpText(out.toString());

                    DumpErrorSingleton.getSingleton().dumpException(e);
                }

                //noinspection ResultOfMethodCallIgnored
                Thread.interrupted();

                return ERR_PROCESS_INFO;
            } finally {
                if (process != null) {
                    destroyableCommands.remove(process);
                    closeQuietly(process.getInputStream());
                    closeQuietly(process.getErrorStream());
                    closeQuietly(process.getOutputStream());
                }

                if (stdErr != null) {
                    try {
                        String error = new String(readAllBytes(stdErr)).trim();
                        if (!error.isEmpty()) {
                            DumpErrorSingleton.getSingleton().dumpText(error);
                        }
                        delete(stdErr);
                    } catch (IOException e) {
                        // cannot do anything about it, the dump file writes would fail as well
                    }
                }
            }
        }

        private void closeQuietly(AutoCloseable autoCloseable) {
            if (autoCloseable != null) {
                try {
                    autoCloseable.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public String toString() {
        String args = "ppid=" + ppid + ", stopped=" + stopped;

        ProcessInfo processInfo = parentProcessInfo;
        if (processInfo != null) {
            args += ", invalid=" + processInfo.isInvalid() + ", error=" + processInfo.isError();
        }

        if (IS_OS_UNIX) {
            args += ", canExecuteLocalUnixPs=" + canExecuteLocalUnixPs() + ", canExecuteStandardUnixPs="
                    + canExecuteStandardUnixPs();
        }

        return "PpidChecker{" + args + '}';
    }
}
