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
package org.apache.maven.surefire.junitcore;


import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.RunListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Writes out a specific {@link org.junit.runner.notification.Failure} for
 * surefire as a stacktrace.
 *
 * @author Karl M. Davis
 * @author Kristian Rosenvold (junit core adaption)
 */

class JUnitCoreTestSet
{
    private final Class testClass;

    private static final String className = "org.jdogma.junit.ConfigurableParallelComputer";

    public String getName()
    {
        return testClass.getName();
    }

    Class getTestClass()
    {
        return testClass;
    }

    /**
     * Constructor.
     *
     * @param testClasses the classes to be run as a test
     */
    protected JUnitCoreTestSet( Class testClasses )
    {
        this.testClass = testClasses;
    }

    /**
     * Actually runs the test and adds the tests results to the <code>reportManager</code>.
     *
     * @param reportManager       The report manager
     * @param JUnitCoreParameters The parameters for this test
     * @throws TestSetFailedException If something fails
     * @see org.apache.maven.surefire.testset.SurefireTestSet#execute(org.apache.maven.surefire.report.ReporterManager,java.lang.ClassLoader)
     */
    public void execute( ReporterManagerFactory reportManager, JUnitCoreParameters JUnitCoreParameters )
        throws TestSetFailedException
    {

        Class[] classes = new Class[1];
        classes[0] = testClass;
        execute( classes, reportManager, JUnitCoreParameters );
    }

    public static void execute( Class[] classes, ReporterManagerFactory reporterManagerFactory,
                                JUnitCoreParameters jUnitCoreParameters )
        throws TestSetFailedException
    {
        final ConcurrentReportingRunListener listener =
            ConcurrentReportingRunListener.createInstance( reporterManagerFactory, jUnitCoreParameters.isParallelClasses(),
                                                      jUnitCoreParameters.isParallelBoth() );
        Computer computer = getComputer( jUnitCoreParameters );
        try
        {
            runJunitCore( classes, computer, listener );
        }
        finally
        {
            closeIfConfigurable( computer );
        }
    }

    private static void closeIfConfigurable( Computer computer )
        throws TestSetFailedException
    {
        if ( computer.getClass().getName().startsWith( className ) )
        {
            try
            {
                Class<?> cpcClass = Class.forName( className );
                Method method = cpcClass.getMethod( "close" );
                method.invoke( computer );
            }
            catch ( ClassNotFoundException e )
            {
                throw new TestSetFailedException( e );
            }
            catch ( NoSuchMethodException e )
            {
                throw new TestSetFailedException( e );
            }
            catch ( InvocationTargetException e )
            {
                throw new TestSetFailedException( e );
            }
            catch ( IllegalAccessException e )
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
        return jUnitCoreParameters.isConfigurableParallelComputerPresent() ? getConfigurableParallelComputer(
            jUnitCoreParameters ) : getParallelComputer( jUnitCoreParameters );
    }

    private static Computer getParallelComputer( JUnitCoreParameters JUnitCoreParameters )
    {
        if ( JUnitCoreParameters.isUseUnlimitedThreads() )
        {
            return new ParallelComputer( true, true );
        }
        else
        {
            return new ParallelComputer( JUnitCoreParameters.isParallelClasses(),
                                         JUnitCoreParameters.isParallelMethod() );
        }
    }

    private static Computer getConfigurableParallelComputer( JUnitCoreParameters JUnitCoreParameters )
        throws TestSetFailedException
    {

        try
        {
            Class<?> cpcClass = Class.forName( className );
            if ( JUnitCoreParameters.isUseUnlimitedThreads() )
            {
                Constructor<?> constructor = cpcClass.getConstructor();
                return (Computer) constructor.newInstance();
            }
            else
            {
                Constructor<?> constructor =
                    cpcClass.getConstructor( boolean.class, boolean.class, Integer.class, boolean.class );
                return (Computer) constructor.newInstance( JUnitCoreParameters.isParallelClasses(),
                                                           JUnitCoreParameters.isParallelMethod(),
                                                           JUnitCoreParameters.getThreadCount(),
                                                           JUnitCoreParameters.isPerCoreThreadCount() );
            }
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( e );
        }
    }

    private static void runJunitCore( Class[] classes, Computer computer, RunListener real )
        throws TestSetFailedException
    {
        JUnitCore junitCore = new JUnitCore();
        junitCore.addListener( real );
        try
        {
            junitCore.run( computer, classes );
        }
        finally
        {
            junitCore.removeListener( real );
        }
    }


}
