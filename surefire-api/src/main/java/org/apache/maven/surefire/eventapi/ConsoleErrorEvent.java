package org.apache.maven.surefire.eventapi;

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

import org.apache.maven.surefire.report.StackTraceWriter;

import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_ERROR;

/**
 * The event with a message for console error level.
 *
 * @since 3.0.0-M5
 */
public final class ConsoleErrorEvent extends Event
{
    private final StackTraceWriter stackTraceWriter;

    public ConsoleErrorEvent( StackTraceWriter stackTraceWriter )
    {
        super( BOOTERCODE_CONSOLE_ERROR );
        this.stackTraceWriter = stackTraceWriter;
    }

    public StackTraceWriter getStackTraceWriter()
    {
        return stackTraceWriter;
    }

    @Override
    public boolean isControlCategory()
    {
        return false;
    }

    @Override
    public boolean isConsoleCategory()
    {
        return true;
    }

    @Override
    public boolean isConsoleErrorCategory()
    {
        return true;
    }

    @Override
    public boolean isStandardStreamCategory()
    {
        return false;
    }

    @Override
    public boolean isSysPropCategory()
    {
        return false;
    }

    @Override
    public boolean isTestCategory()
    {
        return false;
    }

    @Override
    public boolean isJvmExitError()
    {
        return false;
    }
}
