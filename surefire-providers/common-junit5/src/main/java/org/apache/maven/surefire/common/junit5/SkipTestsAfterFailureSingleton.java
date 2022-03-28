package org.apache.maven.surefire.common.junit5;

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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.surefire.api.util.internal.ConcurrencyUtils.runIfZeroCountDown;

/**
 *
 */
public final class SkipTestsAfterFailureSingleton
{
    private static final SkipTestsAfterFailureSingleton SINGLETON = new SkipTestsAfterFailureSingleton();

    public static SkipTestsAfterFailureSingleton getSingleton()
    {
        return SINGLETON;
    }

    private final AtomicInteger skipAfterFailureCount = new AtomicInteger();
    private volatile boolean active;
    private volatile Runnable onThresholdReached;
    private volatile boolean disableTests;

    private SkipTestsAfterFailureSingleton()
    {
    }

    void setDisableTests( boolean disableTests )
    {
        this.disableTests = disableTests;
    }

    public void setDisableTests()
    {
        setDisableTests( true );
    }

    public boolean isDisableTests()
    {
        return disableTests;
    }

    public void setReRunMode()
    {
        active = false;
    }

    /**
     * Sets the threshold of failure count in current JVM.
     *
     * @param count threshold
     */
    public void init( @Nonnegative int count, @Nonnull Runnable onThresholdReached )
    {
        this.onThresholdReached = onThresholdReached;
        active = count > 0;
        skipAfterFailureCount.set( count );
    }

    public void runIfFailureCountReachedThreshold()
    {
        if ( active )
        {
            runIfZeroCountDown( () -> disableTests = true, skipAfterFailureCount );
            onThresholdReached.run();
        }
    }

}
