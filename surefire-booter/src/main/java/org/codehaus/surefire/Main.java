package org.codehaus.surefire;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;

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

/**
 */
public class Main
{
    private Main()
    {

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

        String forkMode = null;

        if (args.length == 6)
        {
            forkMode = args[5];
        }
        else
        {
            forkMode = "once";
        }

        SurefireBooter surefireBooter = new SurefireBooter();

        surefireBooter.addBattery( "org.codehaus.surefire.battery.DirectoryBattery", new Object[]{ testClassesDirectory, includes, excludes } );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "junit/jars/junit-3.8.1.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( mavenRepoLocal, "surefire/jars/surefire-1.3-SNAPSHOT.jar" ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory, "target/classes/" ).getPath() );

        surefireBooter.addClassPathUrl( new File( testClassesDirectory, "target/test-classes/" ).getPath() );

        processDependencies( dependencies, surefireBooter );

        surefireBooter.addReport( "org.codehaus.surefire.report.ConsoleReport" );

        surefireBooter.setForkMode(forkMode);

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
