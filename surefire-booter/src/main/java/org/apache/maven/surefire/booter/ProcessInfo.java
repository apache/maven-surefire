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

import javax.annotation.Nonnull;

/**
 * Immutable object which encapsulates PID and elapsed time (Unix) or start time (Windows).
 * <br>
 * Methods
 * ({@link #getPID()}, {@link #getTime()}, {@link #isTimeBefore(ProcessInfo)}, {@link #isTimeEqualTo(ProcessInfo)})
 * throw {@link IllegalStateException}
 * if {@link #canUse()} returns {@code false} or {@link #isError()} returns {@code true}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
final class ProcessInfo
{
    static final ProcessInfo INVALID_PROCESS_INFO = new ProcessInfo( null, null );
    static final ProcessInfo ERR_PROCESS_INFO = new ProcessInfo( null, null );

    /**
     * On Unix we do not get PID due to the command is interested only to etime of PPID:
     * <br>
     * <pre>/bin/ps -o etime= -p 123</pre>
     */
    static @Nonnull ProcessInfo unixProcessInfo( String pid, long etime )
    {
        return new ProcessInfo( pid, etime );
    }

    static @Nonnull ProcessInfo windowsProcessInfo( String pid, long startTimestamp )
    {
        return new ProcessInfo( pid, startTimestamp );
    }

    private final String pid;
    private final Comparable time;

    private ProcessInfo( String pid, Comparable time )
    {
        this.pid = pid;
        this.time = time;
    }

    boolean canUse()
    {
        return !isError();
    }

    boolean isInvalid()
    {
        return this == INVALID_PROCESS_INFO;
    }

    boolean isError()
    {
        return this == ERR_PROCESS_INFO;
    }

    String getPID()
    {
        checkValid();
        return pid;
    }

    Comparable getTime()
    {
        checkValid();
        return time;
    }

    @SuppressWarnings( "unchecked" )
    boolean isTimeEqualTo( ProcessInfo that )
    {
        checkValid();
        that.checkValid();
        return time.compareTo( that.time ) == 0;
    }

    @SuppressWarnings( "unchecked" )
    boolean isTimeBefore( ProcessInfo that )
    {
        checkValid();
        that.checkValid();
        return time.compareTo( that.time ) < 0;
    }

    private void checkValid()
    {
        if ( !canUse() )
        {
            throw new IllegalStateException( "invalid process info" );
        }
    }
}
