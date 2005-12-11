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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Arrays;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mail-to:emmanuel@venisse.net">Emmanuel Venisse</a>
 * @version $Id$
 */
public class SurefireBooter
{
    protected static final String EOL = System.getProperty( "line.separator" );

    protected static final String PS = System.getProperty( "path.separator" );

    private static String RUNNER = "org.apache.maven.surefire.ForkedSurefireRunner";

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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public boolean run()
        throws Exception
    {
        boolean result = false;

        System.out.println( ">>>>>>>>> forkMode = " + forkMode );

        if ( forkMode.equals( ForkedSurefireRunner.FORK_NONE ) )
        {
            result = runTestsInProcess();
        }
        else if ( forkMode.equals( ForkedSurefireRunner.FORK_ONCE ) )
        {
            result = runTestsForkOnce();
        }
        else if ( forkMode.equals( ForkedSurefireRunner.FORK_PERTEST ) )
        {
            result = runTestsForkEach();
        }

        return result;
    }

    private IsolatedClassLoader createClassLoader()
        throws Exception
    {
        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader( ClassLoader.getSystemClassLoader() );

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
        return fork( getForkOnceArgs() );
    }

    private boolean runTestsForkEach()
        throws Exception
    {
        boolean noFailures = true;

        List testClasses = getTestClasses();

        for ( Iterator i = testClasses.iterator(); i.hasNext(); )
        {
            String testClass = (String) i.next();

            boolean result = fork( getForkPerTestArgs( testClass ) );

            if ( !result )
            {
                noFailures = false;
            }
        }

        return noFailures;
    }

    private boolean fork( String[] args )
        throws Exception
    {
        File workingDirectory = new File( "." );

        Commandline cli = new Commandline();

        basedir = workingDirectory.getAbsolutePath();

        cli.setWorkingDirectory( basedir );

        cli.setExecutable( jvm );

        cli.addArguments( args );

        System.out.println( Commandline.toString( cli.getCommandline() ) );

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
            e.printStackTrace();

            throw new Exception( "Error while executing forked tests.", e );
        }
        catch ( Exception e )
        {
            e.printStackTrace();

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

    private String[] getForkOnceArgs()
        throws Exception
    {
        return getForkArgs( getStringArrayFromBatteries()[0] );
    }

    private String[] getForkPerTestArgs( String testClass )
        throws Exception
    {
        return getForkArgs( SINGLE_TEST_BATTERY + "|" + testClass );
    }

    private String[] getForkArgs( String batteryConfig )
        throws Exception
    {
        String reportClassNames = getListOfStringsAsString( reports, "," );

        String classpathEntries = makeClasspath( classpathUrls );

        List args = new ArrayList();

        args.add( "-classpath" );

        args.add( classpathEntries );

        // ----------------------------------------------------------------------
        // Add some system propeties
        // ----------------------------------------------------------------------

        if ( systemProperties != null )
        {
            Enumeration propertyKeys = systemProperties.propertyNames();

            while ( propertyKeys.hasMoreElements() )
            {
                String key = (String) propertyKeys.nextElement();

                args.add( "-D" + key + "=" + systemProperties.getProperty( key ) );
            }
        }

        args.add( RUNNER );

        args.add( "reportClassNames=" + reportClassNames );

        args.add( "reportsDirectory=" + reportsDirectory );

        args.add( "batteryExecutorName=" + BATTERY_EXECUTOR );

        args.add( "forkMode=" + forkMode );

        args.add( "batteryConfig=" + batteryConfig );

        String[] s = new String[args.size()];

        for ( int i = 0; i < s.length; i++ )
        {
            s[i] = (String) args.get( i );
        }

        return s;
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    private String makeClasspath( List list )
    {
        StringBuffer files = new StringBuffer();

        for ( Iterator i = list.iterator(); i.hasNext(); )
        {
            String classpathElement = (String) i.next();

            files.append( classpathElement );

            files.append( PS );
        }

        return files.toString();
    }

    private String quotedPathArgument( String value )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            return "'" + value.replace( '\\', '/' ) + "'";
        }

        return value;
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
}

