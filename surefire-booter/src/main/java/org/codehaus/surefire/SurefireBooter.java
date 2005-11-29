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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class SurefireBooter
{
    private List batteries = new ArrayList();

    private List reports = new ArrayList();

    private List classpathUrls = new ArrayList();

    private String reportsDirectory;

    public SurefireBooter()
    {
    }

    public void setReportsDirectory( String reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public void addBattery( String battery, Object[] params )
    {
        batteries.add( new Object[]{ battery, params } );
    }

    public void addBattery( String battery )
    {
        batteries.add( new Object[]{ battery, null } );
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

    public boolean run()
        throws Exception
    {
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();

        IsolatedClassLoader surefireClassLoader = new IsolatedClassLoader( systemLoader );

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

        Class batteryExecutorClass = surefireClassLoader.loadClass( "org.codehaus.surefire.Surefire" );

        Object batteryExecutor = batteryExecutorClass.newInstance();

        Method run = batteryExecutorClass.getMethod( "run", new Class[] { List.class, List.class, ClassLoader.class, String.class } );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader( surefireClassLoader );

        Boolean result = (Boolean) run.invoke( batteryExecutor, new Object[]{ reports, batteries, surefireClassLoader, reportsDirectory } );

        Thread.currentThread().setContextClassLoader( oldContextClassLoader );

        return result.booleanValue();
    }

    public void reset()
    {
        batteries.clear();

        reports.clear();

        classpathUrls.clear();
    }

    // ----------------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------------

    public static void main( String[] args )
        throws Exception
    {
        // 0: basedir
        String basedir = args[0];

        System.setProperty( "basedir", basedir );

        // 1; testClassesDirectory
        String testClassesDirectory = args[1];

        // 2; includes

        // 3: excludes


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

        SurefireBooter surefireBooter = new SurefireBooter();

        System.out.println( "testClassesDirectory = " + testClassesDirectory );

        System.out.println( "includes = " + includes );

        System.out.println( "excludes = " + excludes );

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{ testClassesDirectory, includes, excludes } );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.3-SNAPSHOT.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory, "target/classes/" ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory, "target/test-classes/" ).getPath() );

        processDependencies( dependencies, surefireBooter );

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReport" );

        surefireBooter.run();
    }

    private static void processDependencies( List dependencies, SurefireBooter sureFire )
        throws Exception
    {
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            String dep = (String) i.next();
         
            sureFire.addClassPathUrl( new File( dep ).getPath() );
        }
    }
}
