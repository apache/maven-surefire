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

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.apache.maven.surefire.junitcore.ConcurrentRunListener.createInstance;
import static org.apache.maven.surefire.report.ConsoleOutputCapture.startCapture;

/**
 * @author Kristian Rosenvold
 */
public class JUnitCoreTester
{
    private final Computer computer;

    public JUnitCoreTester()
    {
        this( new Computer() );
    }

    public JUnitCoreTester( Computer computer )
    {
        this.computer = computer;
    }

    public Result run( boolean parallelClasses, Class<?>... classes )
        throws TestSetFailedException, ExecutionException
    {
        ReporterFactory reporterManagerFactory = defaultNoXml();

        try
        {
            final HashMap<String, TestSet> classMethodCounts = new HashMap<>();
            RunListener reporter = createInstance( classMethodCounts, reporterManagerFactory, parallelClasses, false,
                                                         new DefaultDirectConsoleReporter( System.out ) );
            startCapture( (ConsoleOutputReceiver) reporter );

            JUnitCoreRunListener runListener = new JUnitCoreRunListener( reporter, classMethodCounts );
            JUnitCore junitCore = new JUnitCore();

            junitCore.addListener( runListener );
            final Result run = junitCore.run( computer, classes );
            junitCore.removeListener( runListener );
            return run;
        }
        finally
        {
            reporterManagerFactory.close();
            if ( computer instanceof ConfigurableParallelComputer )
            {
                ( (ConfigurableParallelComputer) computer ).close();
            }
        }
    }

    /**
     * For testing purposes only.
     *
     * @return DefaultReporterFactory for testing purposes
     */
    public static DefaultReporterFactory defaultNoXml()
    {
        return new DefaultReporterFactory( defaultStartupReportConfiguration(), new NullConsoleLogger() );
    }

    /**
     * For testing purposes only.
     *
     * @return StartupReportConfiguration fo testing purposes
     */
    private static StartupReportConfiguration defaultStartupReportConfiguration()
    {
        File target = new File( "./target" );
        File statisticsFile = new File( target, "TESTHASHxXML" );
        return new StartupReportConfiguration( true, true, "PLAIN", false, true, target, false, null, statisticsFile,
                false, 0, null, null, false );
    }
}
