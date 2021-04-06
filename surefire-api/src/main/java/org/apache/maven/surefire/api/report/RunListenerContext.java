package org.apache.maven.surefire.api.report;

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
 * Memento object handled by {@link RunListener} (and Providers) which carries a runtime status related
 * to the listener important for the concrete implementations in the context of Provider where the listener is utilized.
 */
public final class RunListenerContext
{
    private final ThreadLocal<Long> currentTestIds = new ThreadLocal<>();
    private volatile RunMode runMode;

    public RunListenerContext( @Nonnull RunMode runMode )
    {
        this.runMode = runMode;
    }

    public void setRunMode( @Nonnull RunMode runMode )
    {
        this.runMode = runMode;
    }

    public RunMode getRunMode()
    {
        return runMode;
    }

    public void removeCurrentTestId()
    {
        currentTestIds.remove();
    }

    public void setCurrentTestId( long testId )
    {
        currentTestIds.set( testId );
    }

    public Long getCurrentTestId()
    {
        return currentTestIds.get();
    }

    public boolean hasCurrentTestId()
    {
        return currentTestIds.get() != null;
    }
}
