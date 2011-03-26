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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Attaches the currently executing test method to the thread, allowing
 * test output to be directed to the proper test set.
 *
 * @author Kristian Rosenvold
 */
public abstract class TestConsoleOutputRunListener
    implements RunListener, ConsoleOutputReceiver
{
    private final ReporterFactory reporterFactory;

    protected TestConsoleOutputRunListener( ReporterFactory reporterFactory )
    {
        this.reporterFactory = reporterFactory;
    }

    public static TestConsoleOutputRunListener createInstance( ReporterFactory reporterFactory,
                                                               boolean oneThreadPerClass )
    {
        return oneThreadPerClass ? (TestConsoleOutputRunListener) new OneThreadPerClassConsoleOutputRunListener(
            reporterFactory ) : new UnknownThreadPerClassConsoleOutputRunListener( reporterFactory );
    }

    protected abstract RunListener getTestSetRunListener( ReportEntry reportEntry );

    protected abstract void clearTestSetRunListener( ReportEntry reportEntry );

    protected abstract RunListener getTestMethodRunListener( ReportEntry report );

    protected abstract void clearTestMethodRunListener( ReportEntry reportEntry );

    protected abstract ConsoleOutputReceiver getConsoleOutputReceiver();

    protected ReporterFactory getReporterFactory()
    {
        return reporterFactory;
    }


    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        getConsoleOutputReceiver().writeTestOutput( buf, off, len, stdout );
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        getTestSetRunListener( report ).testSetStarting( report );
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        getTestSetRunListener( report ).testSetCompleted( report );
        clearTestSetRunListener( report );
    }

    public void testStarting( ReportEntry report )
    {
        getTestMethodRunListener( report ).testStarting( report );
    }

    public void testSucceeded( ReportEntry report )
    {
        getTestMethodRunListener( report ).testSucceeded( report );
        clearTestMethodRunListener( report );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        getTestMethodRunListener( report ).testAssumptionFailure( report );
        clearTestMethodRunListener( report );
    }

    public void testError( ReportEntry report )
    {
        getTestMethodRunListener( report ).testError( report );
        clearTestMethodRunListener( report );
    }

    public void testFailed( ReportEntry report )
    {
        getTestMethodRunListener( report ).testFailed( report );
        clearTestMethodRunListener( report );
    }

    public void testSkipped( ReportEntry report )
    {
        getTestMethodRunListener( report ).testSkipped( report );
        clearTestMethodRunListener( report );
    }

    public static class OneThreadPerClassConsoleOutputRunListener
        extends TestConsoleOutputRunListener
    {
        private final ThreadLocal currentTestMethodListener = new InheritableThreadLocal();

        public OneThreadPerClassConsoleOutputRunListener( ReporterFactory reporterFactory )
        {
            super( reporterFactory );
        }

        protected RunListener getTestSetRunListener( ReportEntry reportEntry )
        {
            return getTestMethodRunListener( reportEntry );
        }

        protected void clearTestSetRunListener( ReportEntry reportEntry )
        {
            currentTestMethodListener.remove();
        }

        protected void clearTestMethodRunListener( ReportEntry reportEntry )
        {
            // Dont clear, we do this in testset.
        }

        protected RunListener getTestMethodRunListener( ReportEntry report )
        {
            RunListener runListener = (RunListener) currentTestMethodListener.get();
            if ( runListener == null )
            {
                runListener = getReporterFactory().createReporter();
                currentTestMethodListener.set( runListener );
            }
            return runListener;
        }

        protected ConsoleOutputReceiver getConsoleOutputReceiver()
        {
            return (ConsoleOutputReceiver) currentTestMethodListener.get();
        }


    }

    public static class UnknownThreadPerClassConsoleOutputRunListener
        extends TestConsoleOutputRunListener
    {
        private final ThreadLocal currentTestMethodListener = new InheritableThreadLocal();

        private final ThreadLocal currentTestSetListener = new InheritableThreadLocal();

        private final Map testSetToRunListener = Collections.synchronizedMap( new HashMap() );

        public UnknownThreadPerClassConsoleOutputRunListener( ReporterFactory reporterFactory )
        {
            super( reporterFactory );
        }


        protected RunListener getTestSetRunListener( ReportEntry reportEntry )
        {
            RunListener result = (RunListener) testSetToRunListener.get( reportEntry.getSourceName() );
            if ( result == null )
            {
                result = getReporterFactory().createReporter();
                testSetToRunListener.put( reportEntry.getSourceName(), result );
            }
            currentTestSetListener.set( result );
            return result;
        }

        protected void clearTestSetRunListener( ReportEntry reportEntry )
        {
            currentTestSetListener.remove();
        }

        protected RunListener getTestMethodRunListener( ReportEntry report )
        {
            RunListener runListener;
            runListener = (RunListener) testSetToRunListener.get( report.getSourceName() );
            if ( runListener == null )
            {
                runListener = getReporterFactory().createReporter();
                testSetToRunListener.put( report.getSourceName(), runListener );
            }
            currentTestMethodListener.set( runListener );
            return runListener;
        }

        protected void clearTestMethodRunListener( ReportEntry reportEntry )
        {
            currentTestMethodListener.remove();
        }

        protected ConsoleOutputReceiver getConsoleOutputReceiver()
        {
            ConsoleOutputReceiver consoleOutputReceiver = (ConsoleOutputReceiver) currentTestMethodListener.get();
            if ( consoleOutputReceiver == null )
            {
                consoleOutputReceiver = (ConsoleOutputReceiver) currentTestSetListener.get();
            }
            return consoleOutputReceiver;
        }


    }

}
