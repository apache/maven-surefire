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

import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;

import java.util.concurrent.ExecutionException;

/**
 * Encapsulates access to JUnitCore
 *
 * @author Kristian Rosenvold
 */

class JUnitCoreWrapper
{
    public static void execute( Class[] classes, ReporterManagerFactory reporterManagerFactory,
                                JUnitCoreParameters jUnitCoreParameters )
        throws TestSetFailedException
    {
        final ConcurrentReportingRunListener listener =
            ConcurrentReportingRunListener.createInstance( reporterManagerFactory,
                                                           jUnitCoreParameters.isParallelClasses(),
                                                           jUnitCoreParameters.isParallelBoth() );
        Computer computer = getComputer( jUnitCoreParameters );
        JUnitCore junitCore = new JUnitCore();
        junitCore.addListener( listener );
        try
        {
            junitCore.run( computer, classes );
        }
        finally
        {
            junitCore.removeListener( listener );
            closeIfConfigurable( computer );
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

    private static Computer getConfigurableParallelComputer( JUnitCoreParameters JUnitCoreParameters )
        throws TestSetFailedException
    {
        if ( JUnitCoreParameters.isUseUnlimitedThreads() )
        {
            return new ConfigurableParallelComputer();
        }
        else
        {
            return new ConfigurableParallelComputer( JUnitCoreParameters.isParallelClasses(),
                                                     JUnitCoreParameters.isParallelMethod(),
                                                     JUnitCoreParameters.getThreadCount(),
                                                     JUnitCoreParameters.isPerCoreThreadCount() );
        }
    }
}
