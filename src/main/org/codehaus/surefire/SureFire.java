package org.codehaus.surefire;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.lang.reflect.Method;

public class SureFire
{
    private static ResourceBundle resources = ResourceBundle.getBundle( "org.codehaus.surefire.surefire" );

    private List classPathUrls = new ArrayList();

    private List reportClassNames = new ArrayList();

    private List batteries = new ArrayList();

    public SureFire()
    {
    }

    public static ResourceBundle getResources()
    {
        return resources;
    }

    public void addBattery( String battery, Object[] params )
    {
        batteries.add( new Object[]{ battery, params } );
    }

    public void addBattery( String battery )
    {
        batteries.add( new Object[]{ battery, null } );
    }

    // ----------------------------------------------------------------------
    // ClassPathUrls
    // ----------------------------------------------------------------------

    public void addClassPathUrl( String path )
    {
        if ( !classPathUrls.contains( path ) )
        {
            classPathUrls.add( path );
        }
    }

    public void clearClassPathUrls()
    {
        classPathUrls.clear();
    }

    // ----------------------------------------------------------------------
    // Reports
    // ----------------------------------------------------------------------

    public void setReportClassNames( List reportClassNames )
    {
        this.reportClassNames = reportClassNames;
    }

    public void addReportClassName( String reportClassName )
    {
        getReportClassNames().add( reportClassName );
    }

    public List getReportClassNames()
    {
        if ( reportClassNames == null )
        {
            reportClassNames = new ArrayList();
        }

        return reportClassNames;
    }

    public void clearReportClassNames()
    {
        getReportClassNames().clear();
    }

    public void run()
        throws Exception
    {
        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader();

        for ( Iterator i = classPathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            surefireClassLoader.addURL( new File( url ).toURL() );
        }

        // Now we will instantiate the battery executor

        Class batteryExecutorClass = surefireClassLoader.loadClass( "org.codehaus.surefire.battery.BatteryExecutor" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[] { List.class, List.class, ClassLoader.class } );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        run.invoke( batteryExecutor, new Object[]{ reportClassNames, batteries, surefireClassLoader } );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );
    }

    public void reset()
    {
        batteries.clear();

        clearClassPathUrls();

        clearReportClassNames();
    }

    // ----------------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------------

    public static void main( String[] args )
        throws Exception
    {
        String basedir = args[0];

        String mavenRepoLocal = args[1];

        File dependenciesFile = new File( args[2] );

        List dependencies = new ArrayList();

        BufferedReader buf = new BufferedReader( new FileReader( dependenciesFile ) );

        String line;

        while ( ( line = buf.readLine() ) != null )
        {
            dependencies.add( line );
        }

        buf.close();

        File includesFile = new File( args[3] );

        List includes = new ArrayList();

        buf = new BufferedReader( new FileReader( includesFile ) );

        line = buf.readLine();

        String includesStr = line.substring( line.indexOf( "@" ) + 1 );

        StringTokenizer st = new StringTokenizer( includesStr, "," );

        while ( st.hasMoreTokens() )
        {
            String inc = st.nextToken().trim();

            includes.add( inc );
        }

        buf.close();

        File excludesFile = new File( args[4] );

        List excludes = new ArrayList();

        buf = new BufferedReader( new FileReader( excludesFile ) );

        line = buf.readLine();

        String excludesStr = line.substring( line.indexOf( "@" ) + 1 );

        st = new StringTokenizer( excludesStr, "," );

        while ( st.hasMoreTokens() )
        {
            excludes.add( st.nextToken().trim() );
        }

        buf.close();

        SureFire sureFire = new SureFire();

        sureFire.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{ basedir, includes, excludes } );

        sureFire.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        sureFire.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-battery-0.5.jar" ).getPath() );

        sureFire.addClassPathUrl( new File( basedir, "target/classes/" ).getPath() );

        sureFire.addClassPathUrl( new File( basedir, "target/test-classes/" ).getPath() );

        processDependencies( dependencies, sureFire );

        sureFire.addReportClassName( "org.codehaus.surefire.report.ConsoleReport" );

        sureFire.run();
    }

    private static void processDependencies( List dependencies, SureFire sureFire )
        throws Exception
    {
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            String dep = (String) i.next();

            sureFire.addClassPathUrl( new File( dep ).getPath() );
        }
    }
}
