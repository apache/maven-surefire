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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mail-to:emmanuel@venisse.net">Emmanuel Venisse</a>
 * @author <a href="mailto:andyglick@acm.org">Andy Glick</a>
 * @version $Id$
 */
public class SurefireBooter
{
    private static final Log log = LogFactory.getLog( SurefireBooter.class );

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
        batteries.add( new Object[]{battery,
            params} );
    }

    public void addBattery( String battery )
    {
        batteries.add( new Object[]{battery,
            null} );
    }

    public void addReport( String report )
    {
        reports.add( report );
    }

    public void addClassPathUrl( String path )
    {
        if( !classpathUrls.contains( path ) )
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

        if( "once".equals( forkMode ) )
        {
            result = runTestsForkOnce();
        }
        else if( "none".equals( forkMode ) )
        {
            result = runTestsInProcess();
        }
        else if( "per_test".equals( forkMode ) )
        {
            result = runTestsForkEach();
        }
        else
        {
            // throw
        }

        return result;
    }

    private boolean runTestsInProcess() throws Exception
    {
        log.debug( "entered runTestsInProcess" );

        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader();

        for( Iterator i = classpathUrls.iterator(); i.hasNext(); )
        {
            String url = ( String ) i.next();

            if( url == null )
            {
                continue;
            }

            log.debug( "classpath filename is " + url );

            File f = new File( url );

            surefireClassLoader.addURL( f.toURL() );
        }

        Class batteryExecutorClass = surefireClassLoader
            .loadClass( "org.codehaus.surefire.Surefire" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run",
            new Class[]{List.class,
                List.class,
                ClassLoader.class,
                String.class} );

        ClassLoader oldContextClassLoader = Thread.currentThread()
            .getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        log.debug( "batteryExecutor is " + batteryExecutor );
        log.debug( "reports are " + reports );
        log.debug( "batteries/size() is " + batteries.size() );

        if( batteries.size() > 0 )
        {
            for( int i = 0; i < batteries.size(); i++ )
            {
                Object[] array = ( Object[] ) batteries.get( i );

                log.debug( "battery name is " + array[0] );

                Object[] parmArray = ( Object[] ) array[1];

                for( int j = 0; j < parmArray.length; j++ )
                {
                    log.debug(
                        "parmArray[" + j + "] value is " + parmArray[j] );
                }
            }
        }

        log.debug( "surefireClassLoader's class is " + surefireClassLoader
            .getClass().getName() );
        log.debug( "reportsDirectory is " + reportsDirectory );

        Boolean result = ( Boolean ) run.invoke( batteryExecutor,
            new Object[]{reports,
                batteries,
                surefireClassLoader,
                reportsDirectory} );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

    protected static final String EOL = System.getProperty( "line.separator" );
    protected static final String PS = System.getProperty( "path.separator" );

    private boolean runTestsForkOnce()
        throws Exception
    {
        log.debug( "entered runTestsForkOnce" );

        String executable = "java";

        String pathSeparator = System.getProperty( "path.separator" );

        File workingDirectory = new File( "." );

        Commandline cli = new Commandline();

        basedir = workingDirectory.getAbsolutePath();

        log.debug( "basedir = " + basedir );

        cli.setWorkingDirectory( basedir );

        cli.setExecutable( executable );

        String[] args = getForkArgs();

        cli.addArguments( args );

        String[] returnedArgs = cli.getShellCommandline();

        if( log.isDebugEnabled() )
        {
            for( int i = 0; i < returnedArgs.length; i ++ )
            {
                log.debug( "returned arg is " + returnedArgs[i] );
            }
        }

        Writer stringWriter = new StringWriter();

        StreamConsumer out = new WriterStreamConsumer( stringWriter );

        StreamConsumer err = new WriterStreamConsumer( stringWriter );

        int returnCode;

        List messages = new ArrayList();

        try
        {
            log.debug( "call CommandLineUtils.executeCommandLine" );

            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );

            // messages = parseModernStream( new BufferedReader( new StringReader( stringWriter.toString() ) ) );
        }
        catch( CommandLineException e )
        {
            // throw new SurefireException( "Error while executing forked tests.", e );
            throw new Exception( "Error while executing forked tests.", e );
        }
        catch( Exception e )
        {
            throw new org.codehaus.surefire.SurefireBooterForkException(
                "Error while executing forked tests.", e );
        }

        if( returnCode != 0 && messages.isEmpty() )
        {
            // TODO: exception?
            // messages.add( new SurefireError( "Failure executing forked tests,  but could not parse the error:"
            //  +  EOL + stringWriter.toString(), true ) );

            // messages.add( new Error( "Failure executing forked tests,  but could not parse the error:"
            //  +  EOL + stringWriter.toString() ) );
        }


        Iterator messageIterator = messages.iterator();

        while( messageIterator.hasNext() )
        {
            Error error = ( Error ) messageIterator.next();

            System.out.println( "error message is " + error.getMessage() );

        }

        String string = stringWriter.toString();

        if( string != null && string.length() > 0 )
        {
            StringReader sr = new StringReader( string );

            BufferedReader br = new BufferedReader( sr );

            while( ( string = br.readLine() ) != null )
            {
                System.out.println( string );
            }
        }
        else
        {
            log.debug( "string from process is null or length = 0" );
        }

        if( log.isDebugEnabled() )
        {
            if ( returnCode == 0 )
            {
                log.debug( "tests executed successsfully" );
            }
            else
            {
                log.debug( "some tests failed - returnCode value = "
                    + returnCode );
            }
        }

        return true;
    }


    private boolean runTestsForkEach()
    {
        return true;
    }

    private String[] getForkArgs() throws Exception
    {
        // List reports
        // List batteryHolders
        // List classpathUrls
        // String reportsDirectory
        // String forkMode

        String pathSeparator = System.getProperty( "path.separator" );

        String classpathEntries = getListOfStringsAsString( classpathUrls,
            pathSeparator );

        String reportClassNames = getListOfStringsAsString( reports, "," );

        String[] batteryConfig = getStringArrayFromBatteries();
        // String[] batteryConfig = getArrayOfStringsFromBatteries();

        log.info( "classpathEntries = " + classpathEntries );
        log.info( "reportClassNames = " + reportClassNames );
        log.info( "reportsDirectory = " + reportsDirectory );
        log.info( "batteryExecutorName = " + "org.codehaus.surefire.Surefire" );
        log.info( "forkMode = " + forkMode );

        // a battery is defined as the name of the class - followed by directory
        // and includes and excludes

        // battery = "<batteryClassName>|<directory name>|<includes>|<excludes>
        for( int i = 0; i < batteryConfig.length; i++ )
        {
            log.info( "batteryConfig = " + batteryConfig[i] );
        }

        String[] argArray =
            {
                "-classpath",
                classpathEntries,
                "org.codehaus.surefire.ForkedSurefireLoader",
                "reportClassNames=" + reportClassNames,
                "reportsDirectory=" + reportsDirectory,
                "batteryExecutorName=" + "org.codehaus.surefire.Surefire",
                "forkMode=" + forkMode,
                "batteryConfig=" + batteryConfig[0]
            };

        return argArray;
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    private String getClassPathAsString()
    {
        StringBuffer classpathBuffer = new StringBuffer();

        Iterator classpathUrlIterator = classpathUrls.iterator();

        String delimiter = "";

        while( classpathUrlIterator.hasNext() )
        {
            URL classpathURL = ( URL ) classpathUrlIterator.next();

            classpathBuffer.append( delimiter );
            classpathBuffer.append( classpathURL.toString() );
            delimiter = ",";
        }

        return new String( classpathBuffer );
    }

    private String getListOfStringsAsString( List listOfStrings,
        String delimiterParm )
    {
        StringBuffer stringBuffer = new StringBuffer();

        Iterator listOfStringsIterator = listOfStrings.iterator();

        String delimiter = "";

        while( listOfStringsIterator.hasNext() )
        {
            String string = ( String ) listOfStringsIterator.next();

            stringBuffer.append( delimiter );
            stringBuffer.append( string );

            delimiter = delimiterParm;
        }

        return new String( stringBuffer );
    }


    private String[] getStringArrayFromBatteries()
    {
        String[] batteryConfig = new String[batteries.size()];

        Iterator batteryIterator = batteries.iterator();

        StringBuffer batteryBuffer = new StringBuffer();

        String delimiter = "";

        int batteryCounter = 0;

        while( batteryIterator.hasNext() )
        {
            Object[] batteryArray = ( Object[] ) batteryIterator.next();

            batteryBuffer.append( ( String ) batteryArray[0] );

            if( batteryArray[1] != null )
            {
                Object[] batteryParms = ( Object[] ) batteryArray[1];
                for( int i = 0; i < 3; i++ )
                {
                    batteryBuffer.append( "|" );
                    batteryBuffer.append( batteryParms[i] );
                }
            }
            batteryConfig[batteryCounter++] = new String( batteryBuffer );
        }

        return batteryConfig;
    }

    private String[] getArrayOfStringsFromBatteries()
    {
        log.debug( "batteries.size() is " + batteries.size() );

        String[] batteryConfig = new String[batteries.size()];

        Iterator batteryIterator = batteries.iterator();

        int batteryCounter = 0;

        while( batteryIterator.hasNext() )
        {
            Object[] batteryArray = ( Object[] ) batteryIterator.next();

            batteryConfig[batteryCounter++] = getStringFromBattery(
                batteryArray );
        }

        return batteryConfig;
    }

    private String getStringFromBattery( Object[] battery )
    {
        StringBuffer batteryBuffer = new StringBuffer();
        batteryBuffer.append( ( String ) battery[0] );

        log.debug( "battery[0] should be a battery class " + battery[0] );

        if( battery[1] != null )
        {
            Object[] batteryParms = ( Object[] ) battery[1];

            batteryBuffer.append( "|" );

            log.debug(
                "batteryParms[0]'s class is " + batteryParms[0].getClass()
                    .getName() );

            String directoryName = ( ( File ) batteryParms[0] ).getAbsolutePath();

            if( directoryName == null )
            {
                directoryName = "";
            }

            batteryBuffer.append( directoryName );

            for( int i = 1; i < batteryParms.length; i++ )
            {
                batteryBuffer.append( "|" );
                batteryBuffer
                    .append( getListOfStringsAsString( ( List ) batteryParms[i],
                        "," ) );
            }
        }
        return new String( batteryBuffer );
    }
}

