package org.codehaus.surefire;

import org.codehaus.surefire.battery.Battery;
import org.codehaus.surefire.battery.JUnitBattery;
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
    }

    public static ResourceBundle getResources()
    {
        return resources;
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

                    if ( battery.getTestCount() > 0 )
                    {
                        executeBattery( battery, reporterManager );

                        nbTests += battery.getTestCount();
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

                        if ( b.getTestCount() > 0 )
                        {
                            executeBattery( b, reporterManager );

                            nbTests += b.getTestCount();
                        }
                    }
                }

                if ( nbTests == 0 )
                {
                    reporterManager.writeMessage( "There are no test to run." );
                }
            }
            else
            {
                reporterManager.writeMessage( "There are no battery to run." );
            }

            reporterManager.runCompleted();
        }
        catch ( Throwable ex )
        {
            ReportEntry report = new ReportEntry( ex,
                                                  "org.codehaus.surefire.Runner",
                                                  Surefire.getResources().getString( "bigProblems" ), ex );

            reporterManager.runAborted( report );
        }

        reporterManager.resume();

        if ( reporterManager.getNbErrors() > 0 || reporterManager.getNbFailures() > 0 )
        {
            return false;
        }
        return true;
    }

    public void executeBattery( Battery battery, ReporterManager reportManager )
        throws Exception
    {
        try
        {
            String rawString = Surefire.getResources().getString( "suiteExecutionStarting" );

            ReportEntry report = new ReportEntry( this, battery.getBatteryName(), rawString );

            reportManager.batteryStarting( report );

            try
            {
                battery.execute( reportManager );

                rawString = Surefire.getResources().getString( "suiteCompletedNormally" );

                report = new ReportEntry( this, battery.getBatteryName(), rawString );

                reportManager.batteryCompleted( report );
            }
            catch ( RuntimeException e )
            {
                rawString = Surefire.getResources().getString( "executeException" );

                report = new ReportEntry( this, battery.getBatteryName(), rawString, e );

                reportManager.batteryAborted( report );
            }

            reportManager.runCompleted();

            reportManager.dispose();
        }

        catch ( Throwable ex )
        {
            ReportEntry report = new ReportEntry( ex,
                                                  "org.codehaus.surefire.Runner",
                                                  Surefire.getResources().getString( "bigProblems" ), ex );

            reportManager.runAborted( report );
        }
    }

    public static List instantiateBatteries( List batteryHolders, ClassLoader loader )
        throws Exception
    {
        List batteries = new ArrayList();

        for ( int i = 0; i < batteryHolders.size(); i++ )
        {
            Object[] holder = (Object[]) batteryHolders.get( i );

            Class testClass = null;

            try
            {
                testClass = loader.loadClass( (String) holder[0] );
            }
            catch ( Exception e )
            {
                continue;
            }

            Object battery = null;

            if ( Battery.class.isAssignableFrom( testClass ) )
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

            /*

            We will assume this is a JUnit test because you can have tests in JUnit
            that look like the following:

            public class ThrowingTest
            {
                public static Object suite() throws Exception
                {
                    TestSuite suite = new TestSuite();
                    DistributedSystemTestInfo testInfo = getTestInfo();
                    suite.addTest( new DistributedTestCase( new ScenarioInfo( "default", testInfo ) ) );
                    return suite;
                }
            }

            Which is really not identifiable as a JUnit class at all ... so that's
            why I'm making the assumption because JUnit does.

            else if ( junitTestClass != null && junitTestClass.isAssignableFrom( testClass ) )
            {
                battery = new JUnitBattery( testClass, loader );
            }
            else
            {
                throw new Exception( "Class " + testClass + " is not an implementation of junit.framework.Test" );
            }
            */
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

        for ( Iterator i = reportClassNames.iterator(); i.hasNext(); )
        {
            String reportClass = (String) i.next();

            try
            {
                Reporter report = (Reporter) classLoader.loadClass( reportClass ).newInstance();

                report.setReportsDirectory( reportsDirectory );

                reports.add( report );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        return reports;
    }

}


