package org.apache.maven.surefire;

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

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;

/**
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @version $Id$
 */
public class SurefireBooter
{
    protected static final String EOL = System.getProperty( "line.separator" );

    protected static final String PS = System.getProperty( "path.separator" );

    private static String RUNNER = "org.apache.maven.surefire.SurefireBooter";

    private static String BATTERY_EXECUTOR = "org.apache.maven.surefire.Surefire";

    private static String SINGLE_TEST_BATTERY = "org.apache.maven.surefire.battery.SingleTestBattery";

    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();

    private String reportsDirectory;

    private String forkMode;

    private String basedir;

    private String jvm;

    private Properties systemProperties;

    private String argLine;

    private boolean childDelegation;

    private boolean debug;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static final String FORK_ONCE = "once";

    public static final String FORK_PERTEST = "pertest";

    public static final String FORK_NONE = "none";

    public static final String SUREFIRE_PROPERTIES = "surefire.properties";

    public static final String SYSTEM_PROPERTIES = "surefire-system.properties";

    public static final String CLASSLOADER_PROPERTIES = "surefire-classloader.properties";

    static int TESTS_SUCCEEDED = 0;

    static int TESTS_FAILED = 255;

    static int ILLEGAL_ARGUMENT_EXCEPTION = 100;

    static int OTHER_EXCEPTION = 200;

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public void setReportsDirectory( String reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public String getReportsDirectory()
    {
        return this.reportsDirectory;
    }

    public void addBattery( String battery, Object[] params )
    {
        batteries.add( new Object[]{battery, params} );
    }

    public void addBattery( String battery )
    {
        batteries.add( new Object[]{battery, null} );
    }

    public void addReport( String report )
    {
        reports.add( report );
    }

    public void addClassPathUrl( String path )
    {
        if ( !classpathUrls.contains( path ) )
        {
            classpathUrls.add( path );
        }
    }

    public void setClassPathUrls( List classpathUrls )
    {
        this.classpathUrls = classpathUrls;
    }

    // ----------------------------------------------------------------------
    // Forking options
    // ----------------------------------------------------------------------

    public void setForkMode( String forkMode )
    {
        this.forkMode = forkMode;
    }

    public void setJvm( String jvm )
    {
        this.jvm = jvm;
    }

    public void setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
    }

    public void setArgLine( String argLine )
    {
        this.argLine = argLine;
    }

    public void setBasedir( String basedir )
    {
        this.basedir = basedir;
    }

    public void setChildDelegation( boolean childDelegation )
    {
        this.childDelegation = childDelegation;
    }

    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public boolean run()
        throws Exception
    {
        boolean result = false;

        if ( forkMode.equals( FORK_NONE ) )
        {
            result = runTestsInProcess();
        }
        else if ( forkMode.equals( FORK_ONCE ) )
        {
            result = runTestsForkOnce();
        }
        else if ( forkMode.equals( FORK_PERTEST ) )
        {
            result = runTestsForkEach();
        }

        return result;
    }

    private IsolatedClassLoader createClassLoader()
        throws Exception
    {
        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader( ClassLoader.getSystemClassLoader(), childDelegation );

        for ( Iterator i = classpathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url == null )
            {
                continue;
            }

            File f = new File( url );

            surefireClassLoader.addURL( f.toURL() );
        }

        return surefireClassLoader;
    }

    private boolean runTestsInProcess()
        throws Exception
    {
        IsolatedClassLoader surefireClassLoader = createClassLoader();

        Class batteryExecutorClass = surefireClassLoader.loadClass( BATTERY_EXECUTOR );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[]{List.class, List.class, ClassLoader.class, String.class} );

        ClassLoader oldContextClassLoader = Thread.currentThread() .getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        Boolean result = (Boolean) run.invoke( batteryExecutor, new Object[]{reports, batteries, surefireClassLoader, reportsDirectory} );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

    private boolean runTestsForkOnce()
        throws Exception
    {
        getForkOnceArgs();

        return fork();
    }

    private boolean runTestsForkEach()
        throws Exception
    {
        boolean noFailures = true;

        List testClasses = getTestClasses();

        for ( Iterator i = testClasses.iterator(); i.hasNext(); )
        {
            String testClass = (String) i.next();

            getForkPerTestArgs( testClass );

            boolean result = fork();

            if ( !result )
            {
                noFailures = false;
            }
        }

        return noFailures;
    }

    private boolean fork()
        throws Exception
    {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory( basedir );

        cli.setExecutable( jvm );

        if ( argLine != null )
        {
            cli.addArguments( StringUtils.split( argLine, " " ) );
        }

        cli.createArgument().setValue( "-classpath" );

        cli.createArgument().setValue( surefireBooterJar + PS + plexusUtilsJar );

        cli.createArgument().setValue( RUNNER );

        cli.createArgument().setValue( basedir );

        if ( debug )
        {
            System.out.println( Commandline.toString( cli.getCommandline() ) );
        }

        Writer stringWriter = new StringWriter();

        StreamConsumer out = new WriterStreamConsumer( stringWriter );

        StreamConsumer err = new WriterStreamConsumer( stringWriter );

        int returnCode;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );
        }
        catch ( CommandLineException e )
        {
            throw new Exception( "Error while executing forked tests.", e );
        }
        catch ( Exception e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e );
        }

        String string = stringWriter.toString();

        if ( string != null && string.length() > 0 )
        {
            StringReader sr = new StringReader( string );

            BufferedReader br = new BufferedReader( sr );

            while ( ( string = br.readLine() ) != null )
            {
                System.out.println( string );
            }
        }

        if ( returnCode != 0 )
        {
            return false;
        }

        return true;
    }

    private List getTestClasses()
        throws Exception
    {

        IsolatedClassLoader classLoader = createClassLoader();

        List instantiatedBatteries = Surefire.instantiateBatteries( batteries, classLoader );

        List testClasses = new ArrayList();

        for ( Iterator i = instantiatedBatteries.iterator(); i.hasNext(); )
        {
            Object o = i.next();

            Method m = o.getClass().getMethod( "getSubBatteryClassNames", new Class[]{} );

            List tests = (List) m.invoke( o, new Object[]{} );

            // This class comes from a different classloader then the isolated classloader.
            // This is the battery class that is from a different loader.

            testClasses.addAll( tests );
        }

        return testClasses;
    }

    private void getForkOnceArgs()
        throws Exception
    {
        getForkArgs( getStringArrayFromBatteries()[0] );
    }

    private void getForkPerTestArgs( String testClass )
        throws Exception
    {
        getForkArgs( SINGLE_TEST_BATTERY + "|" + testClass );
    }

    private String surefireBooterJar;

    private String plexusUtilsJar;

    private void getForkArgs( String batteryConfig )
        throws Exception
    {
        String reportClassNames = getListOfStringsAsString( reports, "," );

        Properties p = new Properties();

        for ( int i = 0; i < classpathUrls.size(); i++ )
        {
            String entry = (String) classpathUrls.get( i );

            // Exclude the surefire booter
            if ( entry.indexOf( "surefire-booter" ) > 0 )
            {
                surefireBooterJar = entry;
            }
            else if ( entry.indexOf( "plexus-utils" ) > 0 )
            {
                plexusUtilsJar = entry;
            }
            else
            {
                p.setProperty( Integer.toString( i ), entry );
            }

        }

        FileOutputStream fos = new FileOutputStream( new File( basedir, CLASSLOADER_PROPERTIES ) );

        p.store( fos, "classpath entries" );

        fos.close();

        if ( systemProperties != null )
        {
            File f = new File( basedir, SYSTEM_PROPERTIES );

            fos = new FileOutputStream( f );

            systemProperties.store( fos, "system properties" );

            fos.close();
        }

        p = new Properties();

        p.setProperty( "reportClassNames",  reportClassNames );

        p.setProperty( "reportsDirectory",  reportsDirectory );

        p.setProperty( "batteryExecutorName",  BATTERY_EXECUTOR );

        p.setProperty( "forkMode",  forkMode );

        p.setProperty( "batteryConfig", batteryConfig );

        fos = new FileOutputStream( new File( basedir, SUREFIRE_PROPERTIES ) );

        p.store( fos, "surefire properties" );

        fos.close();

    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    private String getListOfStringsAsString ( List listOfStrings, String delimiterParm )
    {
        StringBuffer stringBuffer = new StringBuffer();

        Iterator listOfStringsIterator = listOfStrings.iterator();

        String delimiter = "";

        while ( listOfStringsIterator.hasNext() )
        {
            String string = (String) listOfStringsIterator.next();

            stringBuffer.append( delimiter );

            stringBuffer.append( string );

            delimiter = delimiterParm;
        }

        return new String( stringBuffer );
    }

    private String[] getStringArrayFromBatteries()
    {
        String[] batteryConfig = new String[batteries.size()];

        StringBuffer batteryBuffer = new StringBuffer();

        int batteryCounter = 0;

        for ( Iterator j = batteries.iterator(); j.hasNext(); )
        {
            Object[] batteryArray = (Object[]) j.next();

            batteryBuffer.append( (String) batteryArray[0] );

            if ( batteryArray[1] != null )
            {
                Object[] batteryParms = (Object[]) batteryArray[1];

                for ( int i = 0; i < 3; i++ )
                {
                    batteryBuffer.append( "|" );

                    batteryBuffer.append( batteryParms[i] );
                }
            }

            batteryConfig[batteryCounter++] = new String( batteryBuffer );
        }

        return batteryConfig;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private static Properties loadProperties( String basedir, String file )
        throws Exception
    {
        File f = new File( basedir, file );

        Properties p = new Properties();

        if ( !f.exists() )
        {
            return p;
        }

        //f.deleteOnExit();

        p.load( new FileInputStream( f ) );

        return p;
    }

    private static Properties getSurefireProperties( String basedir )
        throws Exception
    {
        return loadProperties( basedir, SUREFIRE_PROPERTIES );
    }

    private static void  setSystemProperties( String basedir )
        throws Exception
    {
        Properties p = loadProperties( basedir, SYSTEM_PROPERTIES );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, p.getProperty( key ) );
        }
    }

    private static ClassLoader createForkingClassLoader( String basedir )
        throws Exception
    {
        Properties p = loadProperties( basedir, CLASSLOADER_PROPERTIES );

        IsolatedClassLoader classLoader = new IsolatedClassLoader( ClassLoader.getSystemClassLoader() );

        for ( Iterator i = p.values().iterator(); i.hasNext(); )
        {
            String entry = (String) i.next();

            classLoader.addURL( new File( entry ).toURL() );
        }

        return classLoader;
    }

    /**
     * This method is invoked when Surefire is forked - this method parses and
     * organizes the arguments passed to it and then calls the Surefire class'
     * run method.
     *
     * @param args
     * @throws Exception
     */
    public static void main( String[] args )
        throws Exception
    {
        String basedir = args[0];

        ClassLoader classLoader = createForkingClassLoader( basedir );

        Thread.currentThread().setContextClassLoader( classLoader );

        setSystemProperties( basedir );

        Properties p = getSurefireProperties( basedir );

        String batteryExecutorName = p.getProperty( "batteryExecutorName" );

        Class batteryExecutorClass = classLoader.loadClass( batteryExecutorName );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        String reports = p.getProperty( "reportClassNames" );

        String[] reportClasses = reports.split( "," );

        List reportList = Arrays.asList( reportClasses );

        String batteryConfig = p.getProperty( "batteryConfig" );

        String[] batteryParts = batteryConfig.split( "\\|" );

        String batteryClassName = batteryParts[0];

        Object[] batteryParms;

        String forkMode = p.getProperty( "forkMode" );

        if ( forkMode.equals( FORK_ONCE ) )
        {
            batteryParms = new Object[batteryParts.length - 1];

            batteryParms[0] = new File( batteryParts[1] );

            String stringList = batteryParts[2];

            if ( stringList.startsWith( "[" ) && stringList.endsWith( "]" ) )
            {
                stringList = stringList.substring( 1, stringList.length() - 1 );
            }

            ArrayList includesList = new ArrayList();

            String[] stringArray = stringList.split( "," );

            for ( int i = 0; i < stringArray.length; i++ )
            {
                includesList.add( stringArray[i].trim() );
            }

            batteryParms[1] = includesList;

            stringList = batteryParts[3];

            ArrayList excludesList = new ArrayList();

            if ( stringList.startsWith( "[" ) && stringList.endsWith( "]" ) )
            {
                stringList = stringList.substring( 1, stringList.length() - 1 );
            }

            stringArray = stringList.split( "," );

            for ( int i = 0; i < stringArray.length; i++ )
            {
                excludesList.add( stringArray[i].trim() );
            }

            batteryParms[2] = excludesList;
        }
        else
        {
            batteryParms = new Object[1];

            batteryParms[0] = batteryParts[1];
        }

        List batteryHolders = new ArrayList();

        batteryHolders.add( new Object[]{batteryClassName, batteryParms} );

        String reportsDirectory = p.getProperty( "reportsDirectory" );

        Method run = batteryExecutorClass.getMethod( "run", new Class[]{List.class, List.class, String.class} );

        Object[] parms = new Object[]{reportList, batteryHolders, reportsDirectory};

        int returnCode = TESTS_FAILED;

        try
        {
            boolean result = ( (Boolean) run.invoke( batteryExecutor, parms ) ).booleanValue();

            if ( result )
            {
                returnCode = TESTS_SUCCEEDED;
            }

        }
        catch ( IllegalArgumentException e )
        {
            returnCode = ILLEGAL_ARGUMENT_EXCEPTION;
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            returnCode = OTHER_EXCEPTION;
        }

        System.exit( returnCode );
    }
}

