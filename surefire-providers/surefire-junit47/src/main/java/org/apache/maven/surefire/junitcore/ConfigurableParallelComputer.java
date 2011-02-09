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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.maven.surefire.util.NestedRuntimeException;
import org.junit.runner.Computer;
import org.junit.runner.Runner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;

/*
 * @author Kristian Rosenvold
 */
public class ConfigurableParallelComputer
    extends Computer
{
    private final boolean fClasses;

    private final boolean fMethods;

    private final boolean fixedPool;

    private final ExecutorService fService;

    private final List<AsynchronousRunner> nonBlockers =
        Collections.synchronizedList( new ArrayList<AsynchronousRunner>() );


    public ConfigurableParallelComputer()
    {
        this( true, true, Executors.newCachedThreadPool(), false );
    }

    public ConfigurableParallelComputer( boolean fClasses, boolean fMethods )
    {
        this( fClasses, fMethods, Executors.newCachedThreadPool(), false );
    }

    public ConfigurableParallelComputer( boolean fClasses, boolean fMethods, Integer numberOfThreads, boolean perCore )
    {
        this( fClasses, fMethods, Executors.newFixedThreadPool(
            numberOfThreads * ( perCore ? Runtime.getRuntime().availableProcessors() : 1 ) ), true );
    }

    private ConfigurableParallelComputer( boolean fClasses, boolean fMethods, ExecutorService executorService,
                                          boolean fixedPool )
    {
        this.fClasses = fClasses;
        this.fMethods = fMethods;
        fService = executorService;
        this.fixedPool = fixedPool;
    }

    @SuppressWarnings( { "UnusedDeclaration" } )
    public void close()
        throws ExecutionException
    {
        for ( AsynchronousRunner nonBlocker : nonBlockers )
        {
            nonBlocker.waitForCompletion();
        }

        fService.shutdown();
        try
        {
            fService.awaitTermination( 10, java.util.concurrent.TimeUnit.SECONDS );
        }
        catch ( InterruptedException e )
        {
            throw new NestedRuntimeException( e );
        }
    }

    private Runner parallelize( Runner runner, RunnerScheduler runnerInterceptor )
    {
        if ( runner instanceof ParentRunner<?> )
        {
            ( (ParentRunner<?>) runner ).setScheduler( runnerInterceptor );
        }
        return runner;
    }

    private RunnerScheduler getMethodInterceptor()
    {
        if ( fClasses && fMethods )
        {
            final AsynchronousRunner blockingAsynchronousRunner = new AsynchronousRunner( fService );
            nonBlockers.add( blockingAsynchronousRunner );
            return blockingAsynchronousRunner;
        }
        return fMethods ? new AsynchronousRunner( fService ) : new SynchronousRunner();
    }

    private RunnerScheduler getClassInterceptor()
    {
        if ( fClasses )
        {
            return fMethods ? new SynchronousRunner() : new AsynchronousRunner( fService );
        }
        return new SynchronousRunner();
    }

    @Override
    public Runner getSuite( RunnerBuilder builder, java.lang.Class<?>[] classes )
        throws InitializationError
    {
        Runner suite = super.getSuite( builder, classes );
        return fClasses ? parallelize( suite, getClassInterceptor() ) : suite;
    }

    @Override
    protected Runner getRunner( RunnerBuilder builder, Class<?> testClass )
        throws Throwable
    {
        Runner runner = super.getRunner( builder, testClass );
        return fMethods ? parallelize( runner, getMethodInterceptor() ) : runner;
    }

    @Override
    public String toString()
    {
        return "ConfigurableParallelComputer{" + "classes=" + fClasses + ", methods=" + fMethods + ", fixedPool="
            + fixedPool + '}';
    }

    private class SynchronousRunner
        implements RunnerScheduler
    {
        public void schedule( final Runnable childStatement )
        {
            childStatement.run();
        }

        public void finished()
        {
        }
    }

    public class AsynchronousRunner
        implements RunnerScheduler
    {
        private final List<Future<Object>> futures = Collections.synchronizedList( new ArrayList<Future<Object>>() );

        private final ExecutorService fService;

        public AsynchronousRunner( ExecutorService fService )
        {
            this.fService = fService;
        }

        public void schedule( final Runnable childStatement )
        {
            final Callable<Object> objectCallable = new Callable<Object>()
            {
                public Object call()
                    throws Exception
                {
                    childStatement.run();
                    return null;
                }
            };
            futures.add( fService.submit( objectCallable ) );
        }


        public void finished()
        {
            try
            {
                waitForCompletion();
            }
            catch ( ExecutionException e )
            {
                throw new NestedRuntimeException( e );
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
                    throw new NestedRuntimeException( e );
                }
            }
        }
    }
}
