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

import org.apache.maven.surefire.api.report.RunMode;

import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_SYSPROPS;

/**
 * The event of system property.
 *
 * @since 3.0.0-M5
 */
public final class SystemPropertyEvent extends Event
{
    private final RunMode runMode;
    private final Long testRunId;
    private final String key;
    private final String value;

    public SystemPropertyEvent( RunMode runMode, Long testRunId, String key, String value )
    {
        super( BOOTERCODE_SYSPROPS );
        this.runMode = runMode;
        this.testRunId = testRunId;
        this.key = key;
        this.value = value;
    }

    public RunMode getRunMode()
    {
        return runMode;
    }

    public Long getTestRunId()
    {
        return testRunId;
    }

    public String getKey()
    {
        return key;
    }

    public String getValue()
    {
        return value;
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
        return true;
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
