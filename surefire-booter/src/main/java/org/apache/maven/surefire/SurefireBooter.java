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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mail-to:emmanuel@venisse.net">Emmanuel Venisse</a>
 * @version $Id$
 */
public class SurefireBooter
{
    private static String RUNNER = "org.codehaus.surefire.ForkedSurefireRunner";

    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();

    private String reportsDirectory;

    private String forkMode;

    private String basedir;

    public SurefireBooter()
    {
    }

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

    public void setForkMode( String forkMode )
    {
        this.forkMode = forkMode;
    }

    public boolean run()
        throws Exception
    {
        boolean result = false;

        if ( "once".equals( forkMode ) )
        {
            result = runTestsForkOnce();
        }
        else if ( "none".equals( forkMode ) )
        {
            result = runTestsInProcess();
        }
        else if ( "pertest".equals( forkMode ) )
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

        Class batteryExecutorClass = surefireClassLoader.loadClass( "org.codehaus.surefire.Surefire" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[]{List.class, List.class, ClassLoader.class, String.class} );

        ClassLoader oldContextClassLoader = Thread.currentThread() .getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        Boolean result = (Boolean) run.invoke( batteryExecutor, new Object[]{reports, batteries, surefireClassLoader, reportsDirectory} );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

    protected static final String EOL = System.getProperty( "line.separator" );

    protected static final String PS = System.getProperty( "path.separator" );

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
        String executable = "java";

        File workingDirectory = new File( "." );

        Commandline cli = new Commandline();

        basedir = workingDirectory.getAbsolutePath();

        cli.setWorkingDirectory( basedir );

        cli.setExecutable( executable );

        cli.addArguments( args );

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

    private String[] getForkOnceArgs()
        throws Exception
    {
        // List reports
        // List batteryHolders
        // List classpathUrls
        // String reportsDirectory
        // String forkMode

        String pathSeparator = System.getProperty( "path.separator" );

        String classpathEntries = getListOfStringsAsString( classpathUrls, pathSeparator );

        String reportClassNames = getListOfStringsAsString( reports, "," );

        String[] batteryConfig = getStringArrayFromBatteries();

        String[] argArray =
            {
                "-classpath",
                classpathEntries,
                RUNNER,
                "reportClassNames=" + reportClassNames,
                "reportsDirectory=" + reportsDirectory,
                "batteryExecutorName=" + "org.codehaus.surefire.Surefire",
                "forkMode=" + forkMode,
                "batteryConfig=" + batteryConfig[0]
            };

        return argArray;
    }

    private String[] getForkPerTestArgs( String testClass )
        throws Exception
    {
        // List reports
        // List batteryHolders
        // List classpathUrls
        // String reportsDirectory
        // String forkMode

        String pathSeparator = System.getProperty( "path.separator" );

        String classpathEntries = getListOfStringsAsString( classpathUrls, pathSeparator );

        String reportClassNames = getListOfStringsAsString( reports, "," );

        String batteryConfig = "org.codehaus.surefire.battery.SingleTestBattery|" + testClass;

        String[] argArray =
            {
                "-classpath",
                classpathEntries,
                RUNNER,
                "reportClassNames=" + reportClassNames,
                "reportsDirectory=" + reportsDirectory,
                "batteryExecutorName=" + "org.codehaus.surefire.Surefire",
                "forkMode=" + forkMode,
                "batteryConfig=" + batteryConfig
            };

        return argArray;
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    private String getListOfStringsAsString( List listOfStrings,
                                             String delimiterParm )
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

        String delimiter = "";

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

