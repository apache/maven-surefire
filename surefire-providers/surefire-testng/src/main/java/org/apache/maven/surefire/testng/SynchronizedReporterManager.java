package org.apache.maven.surefire.testng;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterException;

/**
 * A proxy that imposes synchronization on the Reporter.
 * <p/>
 * <p/>
 * At the moment this class only provides "compatible" synchronization that the testng runner can use,
 * and provides the same (faulty) level of synchronization as the <2.6 versions of surefire.
 * <p/>
 * In the "future"  when the concurrent junit provider is rid of all problems of childhood,
 * it should probably replace the entire reporting secion for testng too.
 * <p/>
 * <p/>
 * <p/>
 * This design is really only good for single-threaded test execution. Although it is currently
 * used by testng provider, the design does not really make sense (and is buggy).
 * <p/>
 * This is because to get correct results, the client basically needs to do something like this:
 * synchronized( ReporterManger.getClass()){
 * reporterManager.runStarted()
 * reporterManager.testSetStarting()
 * reporterManager.testStarting()
 * reporterManager.testSucceeded()
 * reporterManager.testSetCompleted()
 * reporterManager.runCompleted()
 * }
 * <p/>
 * This is because the underlying providers are singletons and keep state, if you remove the outer synchronized
 * block, you may get mixups between results from different tests; although the end result (total test count etc)
 * should probably be correct.
 * <p/>
 * <p/>
 *
 * @noinspection deprecation
 */
class SynchronizedReporterManager
    implements Reporter
{

    private final Reporter target;

    public SynchronizedReporterManager( Reporter target )
    {
        this.target = target;
    }

    public synchronized void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        target.testSetStarting( report );
    }

    public synchronized void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        target.testSetCompleted( report );
    }

    public synchronized void testStarting( ReportEntry report )
    {
        target.testStarting( report );
    }

    public synchronized void testSucceeded( ReportEntry report )
    {
        target.testSucceeded( report );
    }

    public synchronized void testError( ReportEntry report, String stdOut, String stdErr )
    {
        target.testError( report, stdOut, stdErr );
    }

    public synchronized void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        target.testFailed( report, stdOut, stdErr );
    }

    public synchronized void testSkipped( ReportEntry report )
    {
        target.testSkipped( report );
    }

    public synchronized void reset()
    {
        target.reset();
    }

    public synchronized void writeMessage( String message )
    {
        target.writeMessage( message );
    }


    public synchronized void testError( ReportEntry reportEntry )
    {
        target.testError( reportEntry );
    }

    public synchronized void testFailed( ReportEntry reportEntry )
    {
        target.testFailed( reportEntry );
    }

    public synchronized void writeDetailMessage( String message )
    {
        target.writeDetailMessage( message );
    }

    public synchronized void writeFooter( String footer )
    {
        target.writeFooter( footer );
    }

    public synchronized void testAssumptionFailure( ReportEntry report )
    {
        target.testAssumptionFailure( report );
    }
}
