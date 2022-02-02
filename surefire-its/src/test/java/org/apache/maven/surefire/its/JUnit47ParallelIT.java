package org.apache.maven.surefire.its;

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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing JUnitCoreWrapper with ParallelComputerBuilder.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.16
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class JUnit47ParallelIT extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void unknownThreadCountSuites()
    {
        unpack().parallelSuites().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use threadCount or threadCountSuites > 0 or useUnlimitedThreads=true for parallel='suites'" );
    }

    @Test
    public void unknownThreadCountClasses()
    {
        unpack().parallelClasses().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use threadCount or threadCountClasses > 0 or useUnlimitedThreads=true for parallel='classes'" );
    }

    @Test
    public void unknownThreadCountMethods()
    {
        unpack().parallelMethods().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use threadCount or threadCountMethods > 0 or useUnlimitedThreads=true for parallel='methods'" );

    }

    @Test
    public void unknownThreadCountBoth()
    {
        unpack().parallelBoth().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use useUnlimitedThreads=true, " + "or only threadCount > 0, " + "or (threadCountClasses > 0 and "
                        + "threadCountMethods > 0), " + "or (threadCount > 0 and threadCountClasses > 0 and "
                        + "threadCountMethods > 0), " + "or (threadCount > 0 and threadCountClasses > 0 and "
                        + "threadCount > threadCountClasses) " + "for parallel='both' or "
                        + "parallel='classesAndMethods'" );
    }

    @Test
    public void unknownThreadCountAll()
    {
        unpack().parallelAll().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use useUnlimitedThreads=true, " + "or only threadCount > 0, " + "or (threadCountSuites > 0 and "
                        + "threadCountClasses > 0 and threadCountMethods > 0), " + "or every thread-count is "
                        + "specified, " + "or (threadCount > 0 and threadCountSuites > 0 and threadCountClasses > 0 "
                        + "and threadCount > threadCountSuites + threadCountClasses) " + "for parallel='all'" );
    }

    @Test
    public void unknownThreadCountSuitesAndClasses()
    {
        unpack().parallelSuitesAndClasses().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use useUnlimitedThreads=true, " + "or only threadCount > 0, " + "or (threadCountSuites > 0 and "
                        + "threadCountClasses > 0), " + "or (threadCount > 0 and threadCountSuites > 0 and "
                        + "threadCountClasses > 0) " + "or (threadCount > 0 and threadCountSuites > 0 and threadCount"
                        + " > threadCountSuites) " + "for parallel='suitesAndClasses' or 'both'" );
    }

    @Test
    public void unknownThreadCountSuitesAndMethods()
    {
        unpack().parallelSuitesAndMethods().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use useUnlimitedThreads=true, " + "or only threadCount > 0, " + "or (threadCountSuites > 0 and "
                        + "threadCountMethods > 0), " + "or (threadCount > 0 and threadCountSuites > 0 and "
                        + "threadCountMethods > 0), " + "or (threadCount > 0 and threadCountSuites > 0 and "
                        + "threadCount > threadCountSuites) " + "for parallel='suitesAndMethods'" );
    }

    @Test
    public void unknownThreadCountClassesAndMethods()
    {
        unpack().parallelClassesAndMethods().setTestToRun( "TestClass" ).failNever().executeTest().verifyTextInLog(
                "Use useUnlimitedThreads=true, " + "or only threadCount > 0, " + "or (threadCountClasses > 0 and "
                        + "threadCountMethods > 0), " + "or (threadCount > 0 and threadCountClasses > 0 and "
                        + "threadCountMethods > 0), " + "or (threadCount > 0 and threadCountClasses > 0 and "
                        + "threadCount > threadCountClasses) " + "for parallel='both' or "
                        + "parallel='classesAndMethods'" );
    }

    @Test
    public void serial()
    {
        // takes 7.2 sec
        unpack().setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsSuites1()
    {
        // takes 3.6 sec
        unpack().parallelSuites().useUnlimitedThreads().setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void unlimitedThreadsSuites2()
    {
        // takes 3.6 sec
        unpack().parallelSuites().useUnlimitedThreads().threadCountSuites( 5 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsClasses1()
    {
        // takes 1.8 sec
        unpack().parallelClasses().useUnlimitedThreads().setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void unlimitedThreadsClasses2()
    {
        // takes 1.8 sec
        unpack().parallelClasses().useUnlimitedThreads().threadCountClasses( 5 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsMethods1()
    {
        // takes 2.4 sec
        unpack().parallelMethods().useUnlimitedThreads().setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void unlimitedThreadsMethods2()
    {
        // takes 2.4 sec
        unpack().parallelMethods().useUnlimitedThreads().threadCountMethods( 5 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsSuitesAndClasses1()
    {
        // takes 0.9 sec
        unpack().parallelSuitesAndClasses().useUnlimitedThreads().setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsSuitesAndClasses2()
    {
        // takes 0.9 sec
        // 1.8 sec with 4 parallel classes
        unpack().parallelSuitesAndClasses().useUnlimitedThreads().threadCountSuites( 5 ).threadCountClasses(
                15 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsSuitesAndMethods1()
    {
        // takes 1.2 sec
        unpack().parallelSuitesAndMethods().useUnlimitedThreads().setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsSuitesAndMethods2()
    {
        // takes 1.2 sec
        unpack().parallelSuitesAndMethods().useUnlimitedThreads().threadCountSuites( 5 ).threadCountMethods(
                15 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsClassesAndMethods1()
    {
        // takes 0.6 sec
        unpack().parallelClassesAndMethods().useUnlimitedThreads().setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsClassesAndMethods2()
    {
        // takes 0.6 sec
        unpack().parallelClassesAndMethods().useUnlimitedThreads().threadCountClasses( 5 ).threadCountMethods(
                15 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsAll1()
    {
        // takes 0.3 sec
        unpack().parallelAll().useUnlimitedThreads().setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void unlimitedThreadsAll2()
    {
        // takes 0.3 sec
        unpack().parallelAll().useUnlimitedThreads().threadCountSuites( 5 ).threadCountClasses( 15 ).threadCountMethods(
                30 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountSuites()
    {
        // takes 3.6 sec
        unpack().parallelSuites().threadCount( 3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountClasses()
    {
        // takes 3.6 sec for single core
        // takes 1.8 sec for double core
        unpack().parallelClasses().threadCount( 3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountMethods()
    {
        // takes 2.4 sec
        unpack().parallelMethods().threadCount( 3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountClassesAndMethodsOneCore()
    {
        // takes 4.8 sec
        unpack().disablePerCoreThreadCount().disableParallelOptimization().parallelClassesAndMethods().threadCount(
                3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountClassesAndMethodsOneCoreOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().disablePerCoreThreadCount().parallelClassesAndMethods().threadCount( 3 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountClassesAndMethods()
    {
        // takes 2.4 sec for double core CPU
        unpack().disableParallelOptimization().parallelClassesAndMethods().threadCount( 3 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountClassesAndMethodsOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelClassesAndMethods().threadCount( 3 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountSuitesAndMethods()
    {
        // usually 24 times 0.3 sec = 7.2 sec with one core CPU
        // takes 1.8 sec for double core CPU
        unpack().disableParallelOptimization().parallelSuitesAndMethods().threadCount( 3 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountSuitesAndMethodsOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelSuitesAndMethods().threadCount( 3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void threadCountSuitesAndClasses()
    {
        unpack().disableParallelOptimization().parallelSuitesAndClasses().threadCount( 3 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountSuitesAndClassesOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelSuitesAndClasses().threadCount( 3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void threadCountAll()
    {
        unpack().disableParallelOptimization().parallelAll().threadCount( 3 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void threadCountAllOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelAll().threadCount( 3 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void everyThreadCountSuitesAndClasses()
    {
        // takes 1.8 sec for double core CPU
        unpack().parallelSuitesAndClasses().threadCount( 3 ).threadCountSuites( 34 ).threadCountClasses(
                66 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void everyThreadCountSuitesAndMethods()
    {
        // takes 1.8 sec for double core CPU
        unpack().parallelSuitesAndMethods().threadCount( 3 ).threadCountSuites( 34 ).threadCountMethods(
                66 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void everyThreadCountClassesAndMethods()
    {
        // takes 1.8 sec for double core CPU
        unpack().parallelClassesAndMethods().threadCount( 3 ).threadCountClasses( 34 ).threadCountMethods(
                66 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void everyThreadCountAll()
    {
        // takes 2.4 sec for double core CPU
        unpack().parallelAll().threadCount( 3 ).threadCountSuites( 17 ).threadCountClasses( 34 ).threadCountMethods(
                49 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountSuitesAndClasses()
    {
        // 4 * cpu to 5 * cpu threads to run test classes
        // takes cca 1.8 sec
        unpack().disableParallelOptimization().parallelSuitesAndClasses().disablePerCoreThreadCount().threadCount(
                6 ).threadCountSuites( 2 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountSuitesAndClassesOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelSuitesAndClasses().disablePerCoreThreadCount().threadCount( 6 ).threadCountSuites(
                2 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountSuitesAndMethods()
    {
        // 4 * cpu to 5 * cpu threads to run test methods
        // takes cca 1.8 sec
        unpack().disableParallelOptimization().parallelSuitesAndMethods().disablePerCoreThreadCount().threadCount(
                6 ).threadCountSuites( 2 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountSuitesAndMethodsOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelSuitesAndMethods().disablePerCoreThreadCount().threadCount( 6 ).threadCountSuites(
                2 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountClassesAndMethods()
    {
        // 4 * cpu to 5 * cpu threads to run test methods
        // takes cca 1.8 sec
        unpack().disableParallelOptimization().parallelClassesAndMethods().disablePerCoreThreadCount().threadCount(
                6 ).threadCountClasses( 2 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountClassesAndMethodsOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelClassesAndMethods().disablePerCoreThreadCount().threadCount( 6 ).threadCountClasses(
                2 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountAll()
    {
        // 8 * cpu to 13 * cpu threads to run test methods
        // takes 0.9 sec
        unpack().disableParallelOptimization().parallelAll().disablePerCoreThreadCount().threadCount(
                14 ).threadCountSuites( 2 ).threadCountClasses( 4 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void reusableThreadCountAllOptimized()
    {
        // the number of reused threads in leafs depends on the number of runners and CPU
        unpack().parallelAll().disablePerCoreThreadCount().threadCount( 14 ).threadCountSuites( 2 ).threadCountClasses(
                4 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void suites()
    {
        // takes 3.6 sec
        unpack().parallelSuites().threadCountSuites( 5 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void classes()
    {
        // takes 1.8 sec on any CPU because the suites are running in a sequence
        unpack().parallelClasses().threadCountClasses( 5 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void methods()
    {
        // takes 2.4 sec on any CPU because every class has only three methods
        // and the suites and classes are running in a sequence
        unpack().parallelMethods().threadCountMethods( 5 ).setTestToRun( "Suite*Test" ).executeTest().verifyErrorFree(
                24 );
    }

    @Test
    public void suitesAndClasses()
    {
        // takes 0.9 sec
        unpack().parallelSuitesAndClasses().threadCountSuites( 5 ).threadCountClasses( 15 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void suitesAndMethods()
    {
        // takes 1.2 sec on any CPU
        unpack().parallelSuitesAndMethods().threadCountSuites( 5 ).threadCountMethods( 15 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void classesAndMethods()
    {
        // takes 0.6 sec on any CPU
        unpack().parallelClassesAndMethods().threadCountClasses( 5 ).threadCountMethods( 15 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void all()
    {
        // takes 0.3 sec on any CPU
        unpack().parallelAll().threadCountSuites( 5 ).threadCountClasses( 15 ).threadCountMethods( 30 ).setTestToRun(
                "Suite*Test" ).executeTest().verifyErrorFree( 24 );
    }

    @Test
    public void shutdown()
    {
        // executes for 2.5 sec until timeout has elapsed
        unpack().parallelMethods().threadCountMethods( 2 ).parallelTestsTimeoutInSeconds( 2.5d ).setTestToRun(
                "TestClass" ).failNever().executeTest().verifyTextInLog(
                "The test run has finished abruptly after timeout of 2.5 seconds." );
    }

    @Test
    public void forcedShutdown()
    {
        // executes for 2.5 sec until timeout has elapsed
        unpack().parallelMethods().threadCountMethods( 2 ).parallelTestsTimeoutForcedInSeconds( 2.5d ).setTestToRun(
                "TestClass" ).failNever().executeTest().verifyTextInLog(
                "The test run has finished abruptly after timeout of 2.5 seconds." );
    }

    @Test
    public void timeoutAndForcedShutdown()
    {
        // executes for one sec until timeout has elapsed
        unpack().parallelMethods().threadCountMethods( 2 ).parallelTestsTimeoutInSeconds(
                1 ).parallelTestsTimeoutForcedInSeconds( 2.5d ).setTestToRun(
                "TestClass" ).failNever().executeTest().verifyTextInLog(
                "The test run has finished abruptly after timeout of 1.0 seconds." );
    }

    @Test
    public void forcedShutdownVerifyingLogs()
        throws Exception
    {
        // attempts to run for 2.4 sec until timeout has elapsed
        OutputValidator validator = unpack()
            .parallelMethods()
            .threadCountMethods( 3 )
            .disablePerCoreThreadCount()
            .parallelTestsTimeoutForcedInSeconds( 1.05d )
            .setTestToRun( "Waiting*Test" )
            .failNever()
            .executeTest()
            .verifyTextInLog( "The test run has finished abruptly after timeout of 1.05 seconds." )
            .verifyTextInLog( "These tests were executed in prior to the shutdown operation:" );

        for ( Iterator<String> it = validator.loadLogLines().iterator(); it.hasNext(); )
        {
            String line = it.next();
            if ( line.contains( "These tests are incomplete:" ) )
            {
                assertThat( it.hasNext() ).isTrue();
                assertThat( it.next() ).matches( "^.*surefireparallel\\.Waiting(\\d{1,1})Test$" );
            }
        }
    }

    private SurefireLauncher unpack()
    {
        return unpack( "junit47-parallel" ).showErrorStackTraces().forkOnce().redirectToFile( false );
    }
}
