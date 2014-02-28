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

import org.apache.maven.surefire.junitcore.pc.ParallelComputer;
import org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder;
import org.apache.maven.surefire.junitcore.pc.RunnerCounter;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.concurrent.TimeUnit;

/**
 * An algorithm which configures {@link ParallelComputer} with allocated thread resources by given
 * {@link JUnitCoreParameters}.
 * The <code>AbstractSurefireMojo</code> has to provide correct combinations of thread-counts and <em>parallel</em>.
 *
 * @author Tibor Digana (tibor17)
 * @see org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder
 * @since 2.16
 */
final class ParallelComputerFactory
{
    private static int availableProcessors = Runtime.getRuntime().availableProcessors();

    private ParallelComputerFactory()
    {
        throw new IllegalStateException( "Suppresses calling constructor, ensuring non-instantiability." );
    }

    /*
    * For testing purposes.
    */
    static void overrideAvailableProcessors( int availableProcessors )
    {
        ParallelComputerFactory.availableProcessors = availableProcessors;
    }

    /*
    * For testing purposes.
    */
    static void setDefaultAvailableProcessors()
    {
        ParallelComputerFactory.availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    static ParallelComputer createParallelComputer( JUnitCoreParameters params, RunnerCounter counts )
        throws TestSetFailedException
    {
        Concurrency concurrency = resolveConcurrency( params, counts );
        ParallelComputerBuilder builder = new ParallelComputerBuilder();

        if ( params.isParallelSuites() )
        {
            resolveSuitesConcurrency( builder, concurrency.suites );
        }

        if ( params.isParallelClasses() )
        {
            resolveClassesConcurrency( builder, concurrency.classes );
        }

        if ( params.isParallelMethods() )
        {
            resolveMethodsConcurrency( builder, concurrency.methods );
        }

        long timeout = secondsToNanos( params.getParallelTestsTimeoutInSeconds() );
        long timeoutForced = secondsToNanos( params.getParallelTestsTimeoutForcedInSeconds() );
        resolveCapacity( builder, concurrency.capacity );
        return builder.buildComputer( timeout, timeoutForced, TimeUnit.NANOSECONDS );
    }

    static Concurrency resolveConcurrency( JUnitCoreParameters params, RunnerCounter counts )
        throws TestSetFailedException
    {
        if ( !params.isAnyParallelitySelected() )
        {
            throw new TestSetFailedException( "Unspecified parameter '" + JUnitCoreParameters.PARALLEL_KEY + "'." );
        }

        if ( !params.isUseUnlimitedThreads() && !hasThreadCount( params ) && !hasThreadCounts( params ) )
        {
            throw new TestSetFailedException( "Unspecified thread-count(s). " +
                                                  "See the parameters " + JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY
                                                  + ", " + JUnitCoreParameters.THREADCOUNT_KEY + ", "
                                                  + JUnitCoreParameters.THREADCOUNTSUITES_KEY + ", "
                                                  + JUnitCoreParameters.THREADCOUNTCLASSES_KEY + ", "
                                                  + JUnitCoreParameters.THREADCOUNTMETHODS_KEY + "." );
        }

        if ( params.isUseUnlimitedThreads() )
        {
            return concurrencyForUnlimitedThreads( params );
        }
        else if ( hasThreadCount( params ) )
        {
            if ( hasThreadCounts( params ) )
            {
                return isLeafUnspecified( params )
                    ? concurrencyFromAllThreadCountsButUnspecifiedLeafCount( params, counts )
                    : concurrencyFromAllThreadCounts( params );
            }
            else
            {
                return estimateConcurrency( params, counts );
            }
        }
        else
        {
            return concurrencyFromThreadCounts( params );
        }
    }

    private static long secondsToNanos( double seconds )
    {
        double nanos = seconds > 0 ? seconds * 1E9 : 0;
        return Double.isInfinite( nanos ) || nanos >= Long.MAX_VALUE ? 0 : (long) nanos;
    }

    private static void resolveSuitesConcurrency( ParallelComputerBuilder builder, int concurrency )
    {
        if ( concurrency > 0 )
        {
            if ( concurrency == Integer.MAX_VALUE )
            {
                builder.parallelSuites();
            }
            else
            {
                builder.parallelSuites( concurrency );
            }
        }
    }

    private static void resolveClassesConcurrency( ParallelComputerBuilder builder, int concurrency )
    {
        if ( concurrency > 0 )
        {
            if ( concurrency == Integer.MAX_VALUE )
            {
                builder.parallelClasses();
            }
            else
            {
                builder.parallelClasses( concurrency );
            }
        }
    }

    private static void resolveMethodsConcurrency( ParallelComputerBuilder builder, int concurrency )
    {
        if ( concurrency > 0 )
        {
            if ( concurrency == Integer.MAX_VALUE )
            {
                builder.parallelMethods();
            }
            else
            {
                builder.parallelMethods( concurrency );
            }
        }
    }

    private static void resolveCapacity( ParallelComputerBuilder builder, int capacity )
    {
        if ( capacity > 0 )
        {
            builder.useOnePool( capacity );
        }
    }

    private static Concurrency concurrencyForUnlimitedThreads( JUnitCoreParameters params )
    {
        Concurrency concurrency = new Concurrency();
        concurrency.suites = params.isParallelSuites() ? threadCountSuites( params ) : 0;
        concurrency.classes = params.isParallelClasses() ? threadCountClasses( params ) : 0;
        concurrency.methods = params.isParallelMethods() ? threadCountMethods( params ) : 0;
        concurrency.capacity = Integer.MAX_VALUE;
        return concurrency;
    }

    private static Concurrency estimateConcurrency( JUnitCoreParameters params, RunnerCounter counts )
    {
        final Concurrency concurrency = new Concurrency();
        final int parallelEntities = countParallelEntities( params );
        concurrency.capacity = multiplyByCoreCount( params, params.getThreadCount() );
        if ( parallelEntities == 1 || counts == null || counts.classes == 0 )
        {
            // Estimate parallel thread counts.
            double ratio = 1d / parallelEntities;
            int threads = multiplyByCoreCount( params, ratio * params.getThreadCount() );
            concurrency.suites = params.isParallelSuites() ? minSuites( threads, counts ) : 0;
            concurrency.classes = params.isParallelClasses() ? minClasses( threads, counts ) : 0;
            concurrency.methods = params.isParallelMethods() ? minMethods( threads, counts ) : 0;
            if ( parallelEntities == 1 )
            {
                concurrency.capacity = 0;
            }
            else
            {
                adjustLeaf( params, concurrency );
            }
        }
        else
        {
            // Try to allocate suites+classes+methods within threadCount,
            concurrency.suites = params.isParallelSuites() ? toNonNegative( counts.suites ) : 0;
            concurrency.classes = params.isParallelClasses() ? toNonNegative( counts.classes ) : 0;
            concurrency.methods =
                params.isParallelMethods() ? toNonNegative( Math.ceil( counts.methods / (double) counts.classes ) ) : 0;
            double sum = toNonNegative( concurrency.suites + concurrency.classes + concurrency.methods );
            if ( concurrency.capacity < sum && sum != 0 )
            {
                // otherwise allocate them using the weighting factor < 1.
                double weight = concurrency.capacity / sum;
                concurrency.suites *= weight;
                concurrency.classes *= weight;
                concurrency.methods *= weight;
            }
            adjustLeaf( params, concurrency );
        }
        return concurrency;
    }

    private static Concurrency concurrencyFromAllThreadCountsButUnspecifiedLeafCount( JUnitCoreParameters params,
                                                                                      RunnerCounter counts )
    {
        Concurrency concurrency = new Concurrency();
        concurrency.suites = params.isParallelSuites() ? params.getThreadCountSuites() : 0;
        concurrency.suites = params.isParallelSuites() ? multiplyByCoreCount( params, concurrency.suites ) : 0;
        concurrency.classes = params.isParallelClasses() ? params.getThreadCountClasses() : 0;
        concurrency.classes = params.isParallelClasses() ? multiplyByCoreCount( params, concurrency.classes ) : 0;
        concurrency.methods = params.isParallelMethods() ? params.getThreadCountMethods() : 0;
        concurrency.methods = params.isParallelMethods() ? multiplyByCoreCount( params, concurrency.methods ) : 0;
        concurrency.capacity = multiplyByCoreCount( params, params.getThreadCount() );

        if ( counts != null )
        {
            concurrency.suites = toNonNegative( Math.min( concurrency.suites, counts.suites ) );
            concurrency.classes = toNonNegative( Math.min( concurrency.classes, counts.classes ) );
        }

        setLeafInfinite( params, concurrency );

        return concurrency;
    }

    private static Concurrency concurrencyFromAllThreadCounts( JUnitCoreParameters params )
    {
        Concurrency concurrency = new Concurrency();
        concurrency.suites = params.isParallelSuites() ? params.getThreadCountSuites() : 0;
        concurrency.classes = params.isParallelClasses() ? params.getThreadCountClasses() : 0;
        concurrency.methods = params.isParallelMethods() ? params.getThreadCountMethods() : 0;
        concurrency.capacity = params.getThreadCount();
        double all = sumThreadCounts( concurrency );

        concurrency.suites = params.isParallelSuites() ? multiplyByCoreCount( params, concurrency.capacity * (
            concurrency.suites / all ) ) : 0;

        concurrency.classes = params.isParallelClasses() ? multiplyByCoreCount( params, concurrency.capacity * (
            concurrency.classes / all ) ) : 0;

        concurrency.methods = params.isParallelMethods() ? multiplyByCoreCount( params, concurrency.capacity * (
            concurrency.methods / all ) ) : 0;

        concurrency.capacity = multiplyByCoreCount( params, concurrency.capacity );
        adjustPrecisionInLeaf( params, concurrency );
        return concurrency;
    }

    private static Concurrency concurrencyFromThreadCounts( JUnitCoreParameters params )
    {
        Concurrency concurrency = new Concurrency();
        concurrency.suites = params.isParallelSuites() ? threadCountSuites( params ) : 0;
        concurrency.classes = params.isParallelClasses() ? threadCountClasses( params ) : 0;
        concurrency.methods = params.isParallelMethods() ? threadCountMethods( params ) : 0;
        concurrency.capacity = toNonNegative( sumThreadCounts( concurrency ) );
        return concurrency;
    }

    private static int countParallelEntities( JUnitCoreParameters params )
    {
        int count = 0;
        if ( params.isParallelSuites() )
        {
            count++;
        }

        if ( params.isParallelClasses() )
        {
            count++;
        }

        if ( params.isParallelMethods() )
        {
            count++;
        }
        return count;
    }

    private static void adjustPrecisionInLeaf( JUnitCoreParameters params, Concurrency concurrency )
    {
        if ( params.isParallelMethods() )
        {
            concurrency.methods = concurrency.capacity - concurrency.suites - concurrency.classes;
        }
        else if ( params.isParallelClasses() )
        {
            concurrency.classes = concurrency.capacity - concurrency.suites;
        }
    }

    private static void adjustLeaf( JUnitCoreParameters params, Concurrency concurrency )
    {
        if ( params.isParallelMethods() )
        {
            concurrency.methods = Integer.MAX_VALUE;
        }
        else if ( params.isParallelClasses() )
        {
            concurrency.classes = Integer.MAX_VALUE;
        }
    }

    private static void setLeafInfinite( JUnitCoreParameters params, Concurrency concurrency )
    {
        if ( params.isParallelMethods() )
        {
            concurrency.methods = Integer.MAX_VALUE;
        }
        else if ( params.isParallelClasses() )
        {
            concurrency.classes = Integer.MAX_VALUE;
        }
        else if ( params.isParallelSuites() )
        {
            concurrency.suites = Integer.MAX_VALUE;
        }
    }

    private static boolean isLeafUnspecified( JUnitCoreParameters params )
    {
        int maskOfParallel = params.isParallelSuites() ? 4 : 0;
        maskOfParallel |= params.isParallelClasses() ? 2 : 0;
        maskOfParallel |= params.isParallelMethods() ? 1 : 0;

        int maskOfConcurrency = params.getThreadCountSuites() > 0 ? 4 : 0;
        maskOfConcurrency |= params.getThreadCountClasses() > 0 ? 2 : 0;
        maskOfConcurrency |= params.getThreadCountMethods() > 0 ? 1 : 0;

        maskOfConcurrency &= maskOfParallel;

        int leaf = Integer.lowestOneBit( maskOfParallel );
        return maskOfConcurrency == maskOfParallel - leaf;
    }

    private static double sumThreadCounts( Concurrency concurrency )
    {
        double sum = concurrency.suites;
        sum += concurrency.classes;
        sum += concurrency.methods;
        return sum;
    }

    private static boolean hasThreadCounts( JUnitCoreParameters jUnitCoreParameters )
    {
        return jUnitCoreParameters.isParallelSuites() && jUnitCoreParameters.getThreadCountSuites() > 0 ||
            jUnitCoreParameters.isParallelClasses() && jUnitCoreParameters.getThreadCountClasses() > 0 ||
            jUnitCoreParameters.isParallelMethods() && jUnitCoreParameters.getThreadCountMethods() > 0;
    }

    private static boolean hasThreadCount( JUnitCoreParameters jUnitCoreParameters )
    {
        return jUnitCoreParameters.getThreadCount() > 0;
    }

    private static int threadCountMethods( JUnitCoreParameters jUnitCoreParameters )
    {
        return multiplyByCoreCount( jUnitCoreParameters, jUnitCoreParameters.getThreadCountMethods() );
    }

    private static int threadCountClasses( JUnitCoreParameters jUnitCoreParameters )
    {
        return multiplyByCoreCount( jUnitCoreParameters, jUnitCoreParameters.getThreadCountClasses() );
    }

    private static int threadCountSuites( JUnitCoreParameters jUnitCoreParameters )
    {
        return multiplyByCoreCount( jUnitCoreParameters, jUnitCoreParameters.getThreadCountSuites() );
    }

    private static int multiplyByCoreCount( JUnitCoreParameters jUnitCoreParameters, double threadsPerCore )
    {
        double numberOfThreads =
            jUnitCoreParameters.isPerCoreThreadCount() ? threadsPerCore * (double) availableProcessors : threadsPerCore;

        return numberOfThreads > 0 ? toNonNegative( numberOfThreads ) : Integer.MAX_VALUE;
    }

    private static int minSuites( int threads, RunnerCounter counts )
    {
        long count = counts == null ? Integer.MAX_VALUE : counts.suites;
        return Math.min( threads, toNonNegative( count ) );
    }

    private static int minClasses( int threads, RunnerCounter counts )
    {
        long count = counts == null ? Integer.MAX_VALUE : counts.classes;
        return Math.min( threads, toNonNegative( count ) );
    }

    private static int minMethods( int threads, RunnerCounter counts )
    {
        long count = counts == null ? Integer.MAX_VALUE : counts.methods;
        return Math.min( threads, toNonNegative( count ) );
    }

    private static int toNonNegative( long num )
    {
        return (int) Math.min( num > 0 ? num : 0, Integer.MAX_VALUE );
    }

    private static int toNonNegative( double num )
    {
        return (int) Math.min( num > 0 ? num : 0, Integer.MAX_VALUE );
    }

    static class Concurrency
    {
        int suites, classes, methods, capacity;
    }
}