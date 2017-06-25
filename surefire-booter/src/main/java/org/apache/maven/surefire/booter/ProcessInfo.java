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

/**
 * PID, PPID, elapsed time (Unix) or start time (Windows).
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
final class ProcessInfo
{
    static final ProcessInfo INVALID_PROCESS_INFO = new ProcessInfo( null, null, null );

    /**
     * On Unix we do not get PID due to the command is interested only to etime of PPID:
     * <br>
     * <pre>/bin/ps -o etime= -p $PPID</pre>
     */
    static ProcessInfo unixProcessInfo( long etime )
    {
        return new ProcessInfo( "pid not needed on Unix", etime, null );
    }

    static ProcessInfo windowsProcessInfo( String pid, String startTimestamp, String ppid )
    {
        return new ProcessInfo( pid, startTimestamp, ppid );
    }

    private final String pid;
    private final Comparable time;
    private final String ppid;

    private ProcessInfo( String pid, Comparable time, String ppid )
    {
        this.pid = pid;
        this.time = time;
        this.ppid = ppid;
    }

    boolean isValid()
    {
        return pid != null && time != null;
    }

    String getPID()
    {
        return pid;
    }

    Comparable getTime()
    {
        return time;
    }

    String getPPID()
    {
        return ppid;
    }
}
