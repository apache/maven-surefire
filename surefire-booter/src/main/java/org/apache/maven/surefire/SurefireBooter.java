package org.apache.maven.surefire;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.util.jdk13.NestedCheckedException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @version $Id$
 */
public class SurefireBooter
{
    protected static final String EOL = System.getProperty( "line.separator" );

    protected static final String PS = System.getProperty( "path.separator" );

    private static final String RUNNER = "org.apache.maven.surefire.SurefireBooter";

    private static final String BATTERY_EXECUTOR = "org.apache.maven.surefire.Surefire";

    private static final String SINGLE_TEST_BATTERY = "org.apache.maven.surefire.battery.JUnitBattery";

    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();

    private String reportsDirectory;

    private String forkMode;

    private String basedir;

    private String jvm;

    private Properties systemProperties;

    private String argLine;

    private Map environmentVariables;

    private File workingDirectory;

    private boolean childDelegation;

    private boolean debug;

    private String surefireBooterJar;

    private String plexusUtilsJar;

    private String jdk13UtilsJar;

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
        if ( forkMode.equals( FORK_NONE ) || forkMode.equals( FORK_ONCE ) || forkMode.equals( FORK_PERTEST ) )
        {
            this.forkMode = forkMode;
        }
        else
        {
            throw new IllegalArgumentException( "Fork mode " + forkMode + " is not a legal value" );
        }
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

    public void setEnvironmentVariables( Map environmentVariables )
    {
        this.environmentVariables = environmentVariables;
    }

    public void setBasedir( String basedir )
    {
        this.basedir = basedir;
    }

    public void setWorkingDirectory( File dir )
    {
        this.workingDirectory = dir;
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

    private ClassLoader createClassLoader() throws Exception
    {
        return createClassLoader( classpathUrls, childDelegation );
    }

    static private ClassLoader createClassLoader( List classpathUrls, boolean childDelegation ) throws Exception
    {
        ArrayList urls = new ArrayList();

        for ( Iterator i = classpathUrls.iterator(); i.hasNext(); )
        {
            String url = (String) i.next();

            if ( url == null )
            {
                continue;
            }

            File f = new File( url );
            urls.add( f.toURL() );
        }

        if ( childDelegation )
        {
            IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader( ClassLoader.getSystemClassLoader(), true );
            for ( Iterator iter = urls.iterator(); iter.hasNext(); )
            {
                URL url = (URL) iter.next();
                surefireClassLoader.addURL( url );
            }
            return surefireClassLoader;
        }
        else
        {
            URL u[] = new URL[urls.size()];
            urls.toArray( u );
            return new URLClassLoader( u, ClassLoader.getSystemClassLoader() );
        }
    }

    private static ClassLoader createForkingClassLoader( String basedir )
        throws Exception
    {
        Properties p = loadProperties( basedir, CLASSLOADER_PROPERTIES );

        String cp = p.getProperty( "classpath" );

        boolean childDelegation = "true".equals( p.getProperty( "childDelegation", "false" ) );

        List urls;
        if ( cp == null )
        {
            urls = new ArrayList(0);
        }
        else
        {
            urls = Arrays.asList( split( cp, PS ) );
        }

        return createClassLoader( urls, childDelegation );
    }


    private boolean runTestsInProcess()
        throws Exception
    {
        ClassLoader surefireClassLoader = createClassLoader();

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

        return fork( true );
    }

    private boolean runTestsForkEach()
        throws Exception
    {
        boolean noFailures = true;

        List testClasses = getTestClasses();

        for ( int i = 0; i < testClasses.size(); i++ )
        {
            String testClass = (String) testClasses.get( i );

            getForkPerTestArgs( testClass );

            // Only show the heading for the first run
            boolean result = fork( i == 0 );

            if ( !result )
            {
                noFailures = false;
            }
        }

        return noFailures;
    }

    private boolean fork( boolean showHeading )
        throws Exception
    {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory( basedir );

        cli.setExecutable( jvm );

        if ( argLine != null )
        {
            cli.addArguments( StringUtils.split( argLine, " " ) );
        }

        if ( environmentVariables != null )
        {
            Iterator iter = environmentVariables.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = (String) environmentVariables.get( key );

                cli.addEnvironment( key, value );

                if ( debug )
                {
                    System.out.println( "Environment: " + key + "=" + value + " added." );
                }

            }

        }

        cli.createArgument().setValue( "-classpath" );

        cli.createArgument().setValue( surefireBooterJar + PS + plexusUtilsJar + PS + jdk13UtilsJar );

        cli.createArgument().setValue( RUNNER );

        cli.createArgument().setValue( basedir );

        if ( workingDirectory != null )
        {
            //both cli's working directory and  system property "user.dir" must have the same value
            cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

            cli.createArgument().setValue( workingDirectory.getAbsolutePath() );
        }

        if ( debug )
        {
            System.out.println( Commandline.toString( cli.getCommandline() ) );
        }

        Writer consoleWriter = new OutputStreamWriter( System.out );

        StreamConsumer out = new ForkingWriterStreamConsumer( consoleWriter, showHeading );

        StreamConsumer err = new ForkingWriterStreamConsumer( consoleWriter, showHeading );

        int returnCode;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );
        }
        catch ( CommandLineException e )
        {
            throw new NestedCheckedException( "Error while executing forked tests.", e );
        }
        catch ( Exception e )
        {
            throw new SurefireBooterForkException( "Error while executing forked tests.", e );
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
        ClassLoader classLoader = createClassLoader();

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


    private void getForkArgs( String batteryConfig )
        throws Exception
    {
        String reportClassNames = getListOfStringsAsString( reports, "," );

        Properties p = new Properties();

        String cp = "";
        for ( int i = 0; i < classpathUrls.size(); i++ )
        {
            String url = (String) classpathUrls.get( i );

            // Exclude the surefire booter
            // Exclude the surefire booter
            if ( url.indexOf( "surefire-booter" ) > 0 )
            {
                surefireBooterJar = url;
            }
            else if ( url.indexOf( "plexus-utils" ) > 0 )
            {
                plexusUtilsJar = url;
            }
            else if ( url.indexOf( "jdk13-util" ) > 0 )
            {
                jdk13UtilsJar = url;
            }
            else
            {
                if ( cp.length() == 0 )
                {
                    cp = url;
                }
                else
                {
                    cp += PS + url;
                }
            }
        }

        p.setProperty( "classpath", cp );

        p.setProperty( "childDelegation", "" + childDelegation );

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

        p.setProperty( "reportClassNames", reportClassNames );

        p.setProperty( "reportsDirectory", reportsDirectory );

        p.setProperty( "batteryExecutorName", BATTERY_EXECUTOR );

        p.setProperty( "forkMode", forkMode );

        p.setProperty( "batteryConfig", batteryConfig );

        p.setProperty( "debug", "" + debug );

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

    private String getListOfStringsAsString( List listOfStrings, String delimiterParm )
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
            throw new FileNotFoundException( f.getAbsolutePath() );
        }

        f.deleteOnExit();

        p.load( new FileInputStream( f ) );

        return p;
    }

    private static Properties getSurefireProperties( String basedir )
        throws Exception
    {
        return loadProperties( basedir, SUREFIRE_PROPERTIES );
    }

    private static void setSystemProperties( String basedir )
        throws Exception
    {
        Properties p = loadProperties( basedir, SYSTEM_PROPERTIES );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, p.getProperty( key ) );
        }
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

        String workingDirectory = null;

        if ( args.length == 2 )
        {
            workingDirectory = args[1];
        }

        ClassLoader classLoader = createForkingClassLoader( basedir );


        Thread.currentThread().setContextClassLoader( classLoader );

        setSystemProperties( basedir );

        if ( workingDirectory != null )
        {
            System.setProperty( "user.dir", workingDirectory );
        }

        Properties p = getSurefireProperties( basedir );

        boolean debug = "true".equals( p.getProperty( "debug", "false" ) );
        if ( debug )
        {
            logClassLoader( classLoader );
        }

        String batteryExecutorName = p.getProperty( "batteryExecutorName" );

        Class batteryExecutorClass = classLoader.loadClass( batteryExecutorName );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        String reports = p.getProperty( "reportClassNames" );

        String[] reportClasses = split( reports, "," );

        List reportList = Arrays.asList( reportClasses );

        String batteryConfig = p.getProperty( "batteryConfig" );

        String[] batteryParts = split( batteryConfig, "\\|" );

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

            String[] stringArray = split( stringList, "," );

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

            stringArray = split( stringList, "," );

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


    private static void logClassLoader( ClassLoader classLoader )
    {
        if ( classLoader.getParent() != null )
        {
            logClassLoader( classLoader.getParent() );
        }

        if ( classLoader instanceof URLClassLoader )
        {
            System.out.println( "ClassLoader: type" + classLoader.getClass() + ", value=" + classLoader );

            URLClassLoader ucl = (URLClassLoader) classLoader;

            URL[] u = ucl.getURLs();

            for ( int i = 0; i < u.length; i++ )
            {
                System.out.println( "           : " + u[i] );
            }
        }
        else
        {
            System.out.println( "ClassLoader: type" + classLoader.getClass() + ", value=" + classLoader );
        }
    }
    
    /**
     * Split a string in a List of Strings using a delimiter. Same as Java 1.4 String.split( String )
     * 
     * @since 1.5.4
     * @param s the string to be splitted
     * @param delimiter the delimiter to be used
     * @return an array with the Strings between the delimiters
     */
    public static String[] split( String s, String delimiter )
    {
        String delim = delimiter;
        if ( "\\|".equals( delimiter ) )
        {
            delim = "|";
        }
        if ( s.equals(delim) )
        {
            return new String[0];
        }
        List tokens = new ArrayList();
        int i = 0;
        int j = s.indexOf( delim, i );
        while ( j > -1 )
        {
            tokens.add( s.substring( i, j ) );
            i = j + delim.length();
            j = s.indexOf( delim, i );
        }
        if ( i < s.length() || ( i == 0 ) )
        {
            tokens.add( s.substring( i ) );
        }
        return (String[]) tokens.toArray( new String[0] );
    }
}

