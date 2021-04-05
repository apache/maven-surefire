package org.apache.maven.surefire.extensions.util;

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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts down the calls {@link #countDown()} and the last reaching zero executes the {@link #job()}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public abstract class CountDownLauncher
{
    private final AtomicInteger countDown;

    public CountDownLauncher( int count )
    {
        if ( count <= 0 )
        {
            throw new IllegalStateException( "count=" + count + " should be greater than zero" );
        }

        countDown = new AtomicInteger( count );
    }

    protected abstract void job() throws IOException, InterruptedException;

    public void countDown() throws IOException, InterruptedException
    {
        if ( countDown.decrementAndGet() == 0 )
        {
            job();
        }
    }
}
