package org.apache.maven.surefire.report;

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

/**
 * Ensures that the current thread has a RunListener instance attached, and forwards calls to it.
 * @author Kristian Rosenvold
 */
public class ThreadLocalRunListener implements RunListener
{
    private final InheritableThreadLocal target = new InheritableThreadLocal();

    private final ReporterFactory reporterFactory;


    public RunListener getTarget()
    {
        Object o = target.get();
        if (o == null){
            o = reporterFactory.createReporter();
            target.set(o);
        }
        return (RunListener) o;
    }

    public ThreadLocalRunListener(ReporterFactory reporterFactory)
    {
        this.reporterFactory = reporterFactory;
    }

    public void testSetStarting( ReportEntry report )
    {
        getTarget().testSetStarting( report );
    }

    public void testSetCompleted( ReportEntry report )
    {
        getTarget().testSetCompleted( report );
    }

    public void testStarting( ReportEntry report )
    {
        getTarget().testStarting( report );
    }

    public void testSucceeded( ReportEntry report )
    {
        getTarget().testSucceeded( report );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        getTarget().testAssumptionFailure( report );
    }

    public void testError( ReportEntry report )
    {
        getTarget().testError( report );
    }

    public void testFailed( ReportEntry report )
    {
        getTarget().testFailed( report );
    }

    public void testSkipped( ReportEntry report )
    {
        getTarget().testSkipped( report );
    }

}
