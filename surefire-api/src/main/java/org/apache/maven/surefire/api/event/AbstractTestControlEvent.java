package org.apache.maven.surefire.api.event;

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

import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.report.ReportEntry;

/**
 * The base class of an event of test control.
 *
 * @since 3.0.0-M5
 * @param <T> TestSetReportEntry or ReportEntry
 */
public abstract class AbstractTestControlEvent<T extends ReportEntry> extends Event
{
    private final T reportEntry;

    public AbstractTestControlEvent( ForkedProcessEventType eventType, T reportEntry )
    {
        super( eventType );
        this.reportEntry = reportEntry;
    }

    public T getReportEntry()
    {
        return reportEntry;
    }

    @Override
    public boolean isControlCategory()
    {
        return false;
    }

    @Override
    public boolean isConsoleCategory()
    {
        return false;
    }

    @Override
    public boolean isConsoleErrorCategory()
    {
        return false;
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
        return true;
    }

    @Override
    public boolean isJvmExitError()
    {
        return false;
    }
}
