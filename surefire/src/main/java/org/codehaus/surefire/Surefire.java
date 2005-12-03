package org.codehaus.surefire;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.surefire.battery.Battery;
import org.codehaus.surefire.battery.JUnitBattery;
import org.codehaus.surefire.battery.assertion.BatteryTestFailedException;
import org.codehaus.surefire.report.ReportEntry;
import org.codehaus.surefire.report.Reporter;
import org.codehaus.surefire.report.ReporterManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

public class Surefire
{
    private static ResourceBundle resources = ResourceBundle.getBundle( "org.codehaus.surefire.surefire" );

    private List batteryHolders;

    private List reports;

    private ReporterManager reporterManager;

    private ClassLoader classLoader;

    private String reportsDirectory;

    public Surefire()
    {
        super();
    }

    public static ResourceBundle getResources()
    {
        return resources;
    }

    public boolean run( List reports, List batteryHolders, String reportsDirectory ) 
        throws Exception
    {
        ClassLoader classLoader = this.getClass().getClassLoader();

        return run( reports, batteryHolders, classLoader, reportsDirectory );
    }

    public boolean run( List reports, List batteryHolders, ClassLoader classLoader, String reportsDirectory )
        throws Exception
    {
        if ( reports == null || batteryHolders == null || classLoader == null )
        {
            throw new NullPointerException();
        }

        if ( batteryHolders.size() == 0 )
        {
            throw new IllegalArgumentException();
        }

        this.batteryHolders = batteryHolders;

        this.reports = reports;

        this.classLoader = classLoader;

        this.reportsDirectory = reportsDirectory;

        return run();
    }

    public boolean run()
        throws Exception
    {
        List batts = instantiateBatteries( batteryHolders, classLoader );

        reporterManager = new ReporterManager( instantiateReports( reports, classLoader ), reportsDirectory );

        try
        {
            reporterManager.runStarting( 100 );

            if ( batts.size() > 0 )
            {
                int nbTests = 0;

                for ( Iterator i = batts.iterator(); i.hasNext(); )
                {
                    Battery battery = (Battery) i.next();

                    int testCount = 0;

                    try
                    {
                        testCount = battery.getTestCount();
                    }
                    catch ( BatteryTestFailedException e )
                    {
                        e.printStackTrace();

                        ReportEntry report = new ReportEntry( e, "org.codehaus.surefire.Runner", getResources().getString( "bigProblems" ), e );

                        reporterManager.batteryAborted( report );
                    }

                    if ( testCount > 0 )
                    {
                        executeBattery( battery, reporterManager );

                        nbTests += testCount;
                    }

                    List list = new ArrayList();

                    for ( Iterator j = battery.getSubBatteryClassNames().iterator(); j.hasNext(); )
                    {
                        String s = (String) j.next();

                        list.add( new Object[]{s, null} );
                    }

                    List subBatteries = instantiateBatteries( list, classLoader );

                    for ( Iterator j = subBatteries.iterator(); j.hasNext(); )
                    {
                        Battery b = (Battery) j.next();

                        testCount = 0;

                        try
                        {
                            testCount = b.getTestCount();
                        }
                        catch ( BatteryTestFailedException e )
                        {
                            e.printStackTrace();

                            ReportEntry report = new ReportEntry( e, "org.codehaus.surefire.Runner", getResources().getString( "bigProblems" ), e );

                            reporterManager.batteryAborted( report );
                        }

                        if ( testCount > 0 )
                        {
                            executeBattery( b, reporterManager );

                            nbTests += testCount;
                        }
                    }
                }

                if ( nbTests == 0 )
                {
                    reporterManager.writeMessage( "There are no tests to run." );
                }
            }
            else
            {
                reporterManager.writeMessage( "There are no batteries to run." );
            }

            reporterManager.runCompleted();
        }
        catch ( Throwable ex )
        {
            ex.printStackTrace();

            ReportEntry report = new ReportEntry( ex, "org.codehaus.surefire.Runner", getResources().getString( "bigProblems" ), ex );

            reporterManager.runAborted( report );
        }

        reporterManager.resume();

        return !( reporterManager.getNbErrors() > 0 || reporterManager
            .getNbFailures() > 0 );
    }

    /**
     * @param battery
     * @param reportManager
     * @throws Exception
     */
    public void executeBattery( Battery battery, ReporterManager reportManager )
        throws Exception
    {
        try
        {
            String rawString = getResources().getString( "suiteExecutionStarting" );

            ReportEntry report = new ReportEntry( this, battery.getBatteryName(), rawString );

            reportManager.batteryStarting( report );

            try
            {
                battery.execute( reportManager );

                rawString = getResources().getString( "suiteCompletedNormally" );

                report = new ReportEntry( this, battery.getBatteryName(), rawString );

                reportManager.batteryCompleted( report );
            }
            catch ( RuntimeException e )
            {
                e.printStackTrace();

                rawString = getResources().getString( "executeException" );

                report = new ReportEntry( this, battery.getBatteryName(), rawString, e );

                reportManager.batteryAborted( report );
            }

            reportManager.runCompleted();

            reportManager.dispose();
        }

        catch ( Throwable ex )
        {
            ReportEntry report = new ReportEntry( ex, "org.codehaus.surefire.Runner", getResources().getString( "bigProblems" ), ex );

            reportManager.runAborted( report );
        }
    }

    /**
     * @param batteryHolders
     * @param loader
     * @return
     * @throws Exception
     */
    public static List instantiateBatteries( List batteryHolders, ClassLoader loader )
        throws Exception
    {
        List batteries = new ArrayList();

        for ( int i = 0; i < batteryHolders.size(); i++ )
        {
            Object[] holder = (Object[]) batteryHolders.get( i );

            Class testClass;

            Class batteryClass;

            try
            {
                testClass = loader.loadClass( (String) holder[0] );

                batteryClass = loader.loadClass( "org.codehaus.surefire.battery.Battery" );
            }
            catch ( Exception e )
            {
                continue;
            }

            Object battery = null;

            if ( batteryClass.isAssignableFrom( testClass ) )
            {
                if ( holder[1] != null )
                {
                    Object[] params = (Object[]) holder[1];

                    Class[] paramTypes = new Class[params.length];

                    for ( int j = 0; j < params.length; j++ )
                    {
                        paramTypes[j] = params[j].getClass();
                    }

                    Constructor constructor = testClass.getConstructor( paramTypes );

                    battery = constructor.newInstance( params );
                }
                else
                {
                    battery = testClass.newInstance();
                }
            }
            else
            {
                battery = new JUnitBattery( testClass, loader );
            }

            batteries.add( battery );
        }

        return batteries;
    }

    protected List instantiateReports( List reportClassNames, ClassLoader classLoader )
        throws Exception
    {
        List reports = new ArrayList();

        boolean fail = false;

        ClassLoader reporterClassLoader = Reporter.class.getClassLoader();

        for ( Iterator i = reportClassNames.iterator(); i.hasNext(); )
        {
            String reportClassName = (String) i.next();

            try
            {
                Class reportClass = reporterClassLoader.loadClass( reportClassName );

                //assert Reporter.class.isAssignableFrom(reportClass);

                Reporter report = (Reporter) reportClass.newInstance();

                report.setReportsDirectory( reportsDirectory );

                reports.add( report );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        if ( fail )
        {
            throw new RuntimeException( "couldn't assign reports as expected" );
        }

        return reports;
    }
}


