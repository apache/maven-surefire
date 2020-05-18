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

import org.apache.maven.surefire.junitcore.JUnitCoreParameters;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.junit.runner.Description;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * An algorithm which configures {@link ParallelComputer} with allocated thread resources by given
 * {@link org.apache.maven.surefire.junitcore.JUnitCoreParameters}.
 * The {@code AbstractSurefireMojo} has to provide correct combinations of thread-counts and
 * configuration parameter {@code parallel}.
 *
 * @author Tibor Digana (tibor17)
 * @see org.apache.maven.surefire.junitcore.pc.ParallelComputerBuilder
 * @since 2.16
 */
final class ParallelComputerUtil
{
    private static final Collection<Description> UNUSED_DESCRIPTIONS =
            Arrays.asList( null, Description.createSuiteDescription( "null" ), Description.TEST_MECHANISM,
                    Description.EMPTY );

    private static int availableProcessors = Runtime.getRuntime().availableProcessors();

    private ParallelComputerUtil()
    {
        throw new IllegalStateException( "Suppresses calling constructor, ensuring non-instantiability." );
    }

    /*
    * For testing purposes.
    */
    static void overrideAvailableProcessors( int availableProcessors )
    {
        ParallelComputerUtil.availableProcessors = availableProcessors;
    }

    /*
    * For testing purposes.
    */
    static void setDefaultAvailableProcessors()
    {
        ParallelComputerUtil.availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    static Concurrency resolveConcurrency( JUnitCoreParameters params, RunnerCounter counts )
        throws TestSetFailedException
    {
        if ( !params.isParallelismSelected() )
        {
            throw new TestSetFailedException( "Unspecified parameter '" + JUnitCoreParameters.PARALLEL_KEY + "'." );
        }

        if ( !params.isUseUnlimitedThreads() && !hasThreadCount( params ) && !hasThreadCounts( params ) )
        {
            throw new TestSetFailedException( "Unspecified thread-count(s). "
                                                  + "See the parameters "
                                                  + JUnitCoreParameters.USEUNLIMITEDTHREADS_KEY + ", "
                                                  + JUnitCoreParameters.THREADCOUNT_KEY + ", "
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

    static boolean isUnusedDescription( Description examined )
    {
        if ( UNUSED_DESCRIPTIONS.contains( examined ) )
        {
            return true;
        }
        else
        {
            // UNUSED_DESCRIPTIONS ensures that "examined" cannot be null
            for ( Description unused : UNUSED_DESCRIPTIONS )
            {
                if ( unused != null && unused.getDisplayName().equals( examined.getDisplayName() ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    static void removeUnusedDescriptions( Collection<Description> examined )
    {
        for ( Iterator<Description> it = examined.iterator(); it.hasNext(); )
        {
            if ( isUnusedDescription( it.next() ) )
            {
                it.remove();
            }
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
        return ( jUnitCoreParameters.isParallelSuites() && jUnitCoreParameters.getThreadCountSuites() > 0 )
            || ( jUnitCoreParameters.isParallelClasses() && jUnitCoreParameters.getThreadCountClasses() > 0 )
            || ( jUnitCoreParameters.isParallelMethods() && jUnitCoreParameters.getThreadCountMethods() > 0 );
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
}
