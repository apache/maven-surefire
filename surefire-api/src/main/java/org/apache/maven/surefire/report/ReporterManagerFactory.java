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

import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.SurefireReflectionException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Creates ReporterManager instances for the providers.
 * <p/>
 * A ReporterManager and the underlying reporters are stateful objects. For safe concurrent usage
 * of the reporting infrastructure, each thread needs its own instance.
 * <p/>
 * This factory also ensures that runStarting/runCompleted is called on the FIRST reporterManger that
 * is allocated, but none of the subsequent managers.
 *
 * @author Jason van Zyl
 * @author Kristian Rosenvold
 */

/**
 * Note; this class contains "old" and "new" style instantiation, "old" style can be removed once we build 2.7.X + with 2.7
 */
public class ReporterManagerFactory
    implements ReporterFactory
{
    private final List reportDefinitions;

    private final ClassLoader surefireClassLoader;

    private final RunStatistics globalRunStatistics = new RunStatistics();

    private final ReporterConfiguration reporterConfiguration;

    private RunReporter first;

    private final Object lock = new Object();

    private List reports;

    private final SystemStreamCapturer systemStreamCapturer = new SystemStreamCapturer();

    public ReporterManagerFactory( ClassLoader surefireClassLoader, ReporterConfiguration reporterConfiguration )
    {
        this.reportDefinitions = reporterConfiguration.getReports();
        this.surefireClassLoader = surefireClassLoader;
        this.reporterConfiguration = reporterConfiguration;
        this.reports = instantiateReportsNewStyle( reportDefinitions, reporterConfiguration, surefireClassLoader );
    }

    public RunStatistics getGlobalRunStatistics()
    {
        return globalRunStatistics;
    }

    public Reporter createReporter()
    {
        reports = instantiateReportsNewStyle( reportDefinitions, reporterConfiguration, surefireClassLoader );
        return setupReporter( reports );
    }


    private Reporter setupReporter( List reports )
    {
        // Note, if we ever start making >1 reporter Managers, we have to aggregate run statistics
        // i.e. we cannot use a single "globalRunStatistics"
        final ReporterManager reporterManager =
            new ReporterManager( reports, globalRunStatistics, systemStreamCapturer );
        if ( first == null )
        {
            synchronized ( lock )
            {
                if ( first == null )
                {
                    first = reporterManager;
                    reporterManager.runStarting();
                }
            }
        }
        return reporterManager;
    }


    public RunResult close()
    {
        warnIfNoTests();
        synchronized ( lock )
        {
            if ( first != null )
            {
                first.runCompleted();
            }
            return globalRunStatistics.getRunResult();
        }
    }

    protected List instantiateReportsNewStyle( List reportDefinitions, ReporterConfiguration reporterConfiguration,
                                               ClassLoader classLoader )
    {
        List reports = new ArrayList();

        for ( Iterator i = reportDefinitions.iterator(); i.hasNext(); )
        {

            String className = (String) i.next();

            Reporter report = instantiateReportNewStyle( className, reporterConfiguration, classLoader );

            reports.add( report );
        }

        return reports;
    }

    private static Reporter instantiateReportNewStyle( String className, ReporterConfiguration params,
                                                       ClassLoader classLoader )
    {
        Class clazz = ReflectionUtils.loadClass( classLoader, className );

        if ( params != null )
        {
            Class[] paramTypes = new Class[1];
            paramTypes[0] = ReflectionUtils.loadClass( classLoader, ReporterConfiguration.class.getName() );
            Constructor constructor = ReflectionUtils.getConstructor( clazz, paramTypes );
            return (Reporter) ReflectionUtils.newInstance( constructor, new Object[]{ params } );
        }
        else
        {
            try
            {
                return (Reporter) clazz.newInstance();
            }
            catch ( IllegalAccessException e )
            {
                throw new SurefireReflectionException( e );
            }
            catch ( InstantiationException e )
            {
                throw new SurefireReflectionException( e );
            }
        }

    }

    private void warnIfNoTests()
    {
        if ( getGlobalRunStatistics().getRunResult().getCompletedCount() == 0 )
        {
            new MulticastingReporter( reports ).writeMessage( "There are no tests to run." );
        }
    }
}
