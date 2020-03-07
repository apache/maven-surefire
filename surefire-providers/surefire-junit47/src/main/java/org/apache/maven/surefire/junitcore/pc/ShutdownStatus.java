package org.apache.maven.surefire.junitcore.pc;

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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper of {@link ParallelComputer ParallelComputer status information} and tests been populated before
 * a shutdown hook has been triggered.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see ParallelComputer
 * @since 2.18
 */
final class ShutdownStatus
{
    private final AtomicReference<ExecutionStatus> status =
        new AtomicReference<>( ExecutionStatus.STARTED );

    private Future<ShutdownResult> descriptionsBeforeShutdown;

    boolean tryFinish()
    {
        return status.compareAndSet( ExecutionStatus.STARTED, ExecutionStatus.FINISHED );
    }

    boolean tryTimeout()
    {
        return status.compareAndSet( ExecutionStatus.STARTED, ExecutionStatus.TIMEOUT );
    }

    boolean isTimeoutElapsed()
    {
        return status.get() == ExecutionStatus.TIMEOUT;
    }

    Future<ShutdownResult> getDescriptionsBeforeShutdown()
    {
        return descriptionsBeforeShutdown;
    }

    void setDescriptionsBeforeShutdown( Future<ShutdownResult> descriptionsBeforeShutdown )
    {
        this.descriptionsBeforeShutdown = descriptionsBeforeShutdown;
    }
}