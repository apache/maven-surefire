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

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This class is executed when SurefireBooter forks surefire JUnit processes
 *
 * @author Jason van Zyl
 * @version $Id$
 */
public class ForkedSurefireRunner
{
    public static final String FORK_ONCE = "once";

    public static final String FORK_PERTEST = "pertest";

    public static final String FORK_NONE = "none";

    public static final String SUREFIRE_PROPERTIES = "surefire.properties";

    public static final String SYSTEM_PROPERTIES = "surefire-system.properties";

    public static final String CLASSLOADER_PROPERTIES = "surefire-classloader.properties";

    static String basedir;

    static int TESTS_SUCCEEDED = 0;

    static int TESTS_FAILED = 255;

    static int ILLEGAL_ARGUMENT_EXCEPTION = 100;

    static int OTHER_EXCEPTION = 200;

    private ForkedSurefireRunner()
    {
        super();
    }

    private static Properties getSurefireProperties()
        throws Exception
    {
        File f = new File( basedir, SUREFIRE_PROPERTIES );

        f.deleteOnExit();

        Properties p = new Properties();

        p.load( new FileInputStream( f ) );

        return p;
    }

    private static void  setSystemProperties()
        throws Exception
    {
        File f = new File( basedir, SYSTEM_PROPERTIES );

        f.deleteOnExit();

        if ( !f.exists() )
        {
            return;
        }


        Properties p = new Properties();

        p.load( new FileInputStream( f ) );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            System.setProperty( key, p.getProperty( key ) );
        }
    }

    private static ClassLoader createClassLoader()
        throws Exception
    {
        File f = new File( basedir, CLASSLOADER_PROPERTIES );

        f.deleteOnExit();

        Properties p = new Properties();

        p.load( new FileInputStream( f ) );

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
        ClassLoader classLoader = createClassLoader();

        setSystemProperties();

        Properties p = getSurefireProperties();

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
