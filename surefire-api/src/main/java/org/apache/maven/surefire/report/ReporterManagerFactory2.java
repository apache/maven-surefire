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

import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
 * @author Kristian Rosenvold (extracted factory)
 */
public class ReporterManagerFactory2
    extends ReporterManagerFactory
{
    private final ReporterConfiguration reporterConfiguration;


    public ReporterManagerFactory2( ClassLoader surefireClassLoader, ReporterConfiguration reporterConfiguration )
    {
        super( reporterConfiguration.getReports(), surefireClassLoader );
        this.reporterConfiguration = reporterConfiguration;
/*        if (!reporterConfiguration.getClass().getClassLoader().equals(  surefireClassLoader )){
            throw new IllegalStateException( "Skunkt classloader stuff" + reporterConfiguration.getClass().getClassLoader() + "sfc" + surefireClassLoader + "Tread" + Thread.currentThread().getContextClassLoader());
        }*/
    }


    public RunStatistics getGlobalRunStatistics()
    {
        return globalRunStatistics;
    }

    public ReporterManager createReporterManager()
        throws TestSetFailedException
    {
        final List reports = instantiateReports( reportDefinitions, reporterConfiguration, surefireClassLoader );
        // Note, if we ever start making >1 reporter Managers, we have to aggregate run statistics
        // i.e. we cannot use a single "globalRunStatistics"
        final ReporterManager reporterManager = new ReporterManager( reports, globalRunStatistics );
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

    private List instantiateReports( List reportDefinitions, ReporterConfiguration reporterConfiguration,
                                     ClassLoader classLoader )
        throws TestSetFailedException
    {
        List reports = new ArrayList();

        for ( Iterator i = reportDefinitions.iterator(); i.hasNext(); )
        {

            String className = (String) i.next();

            Reporter report = instantiateReport( className, reporterConfiguration, classLoader );

            reports.add( report );
        }

        return reports;
    }

    private static Reporter instantiateReport( String className, ReporterConfiguration params, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            return (Reporter) instantiateObject( className, params, classLoader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "Unable to find class to create report '" + className + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException(
                "Unable to find appropriate constructor to create report: " + e.getMessage(), e );
        }
    }

    private static Object instantiateObject( String className, ReporterConfiguration params, ClassLoader classLoader )
        throws TestSetFailedException, ClassNotFoundException, NoSuchMethodException
    {
        Class clazz = classLoader.loadClass( className );

        Object object;
        try
        {
            if ( params != null )
            {
                Class[] paramTypes = new Class[1];
                paramTypes[0] = classLoader.loadClass(ReporterConfiguration.class.getName());

                Constructor constructor = clazz.getConstructor( paramTypes );

                object = constructor.newInstance( new Object[]{ params } );
            }
            else
            {
                object = clazz.newInstance();
            }
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        return object;
    }
}
