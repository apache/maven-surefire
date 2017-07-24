package org.apache.maven.surefire.junitcore;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runners.model.RunnerScheduler;

/**
 * Since SUREFIRE 2.18 this class is deprecated.
 * Please use {@link org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder} instead.
 *
 * @author <a href="mailto:kristian@zenior.no">Kristian Rosenvold</a>
 */
@Deprecated
public class AsynchronousRunner
    implements RunnerScheduler
{
    private final List<Future<Object>> futures = Collections.synchronizedList( new ArrayList<Future<Object>>() );

    private final ExecutorService fService;

    public AsynchronousRunner( ExecutorService fService )
    {
        this.fService = fService;
    }

    @Override
    public void schedule( final Runnable childStatement )
    {
        futures.add( fService.submit( Executors.callable( childStatement ) ) );
    }


    @Override
    public void finished()
    {
        try
        {
            waitForCompletion();
        }
        catch ( ExecutionException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void waitForCompletion()
        throws ExecutionException
    {
        for ( Future<Object> each : futures )
        {
            try
            {
                each.get();
            }
            catch ( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
        }
    }
}
