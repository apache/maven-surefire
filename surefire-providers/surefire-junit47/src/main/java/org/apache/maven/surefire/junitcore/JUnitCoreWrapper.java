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

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

/**
 * Encapsulates access to JUnitCore
 *
 * @author Kristian Rosenvold
 */

class JUnitCoreWrapper
{
    public static void execute( TestsToRun testsToRun, JUnitCoreParameters jUnitCoreParameters,
                                List<RunListener> listeners )
        throws TestSetFailedException
    {
        Computer computer = getComputer( jUnitCoreParameters );
        JUnitCore junitCore = new JUnitCore();
        for ( RunListener runListener : listeners )
        {
            junitCore.addListener( runListener );
        }
        try
        {
            final Result run = junitCore.run( computer, testsToRun.getLocatedClasses() );

            if ( run.getFailureCount() > 0 )
            {
                // There is something interesting going on here;
                // the "run" result can contain other exceptions that did not occur as
                // part of the test run, for instance if something bad happened in the
                // RunListener. But it also contains regular problems from the test-run.
                // I am not entirely sure of what to do with this; it might even be
                // that these errors are the correct errors to report back to the client.
            }
        }
        finally
        {
            closeIfConfigurable( computer );
            for ( RunListener runListener : listeners )
            {
                junitCore.removeListener( runListener );
            }
        }
    }

    private static void closeIfConfigurable( Computer computer )
        throws TestSetFailedException
    {
        if ( computer instanceof ConfigurableParallelComputer )
        {
            try
            {
                ( (ConfigurableParallelComputer) computer ).close();
            }
            catch ( ExecutionException e )
            {
                throw new TestSetFailedException( e );
            }
        }
    }

    private static Computer getComputer( JUnitCoreParameters jUnitCoreParameters )
        throws TestSetFailedException
    {
        if ( jUnitCoreParameters.isNoThreading() )
        {
            return new Computer();
        }
        return getConfigurableParallelComputer( jUnitCoreParameters );
    }

    private static Computer getConfigurableParallelComputer( JUnitCoreParameters jUnitCoreParameters )
        throws TestSetFailedException
    {
        if ( jUnitCoreParameters.isUseUnlimitedThreads() )
        {
            return new ConfigurableParallelComputer();
        }
        else
        {
            return new ConfigurableParallelComputer(
                jUnitCoreParameters.isParallelClasses() | jUnitCoreParameters.isParallelBoth(),
                jUnitCoreParameters.isParallelMethod() | jUnitCoreParameters.isParallelBoth(),
                jUnitCoreParameters.getThreadCount(), jUnitCoreParameters.isPerCoreThreadCount() );
        }
    }
}
