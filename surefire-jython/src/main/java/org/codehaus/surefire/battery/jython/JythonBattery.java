package org.codehaus.surefire.battery.jython;

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

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.surefire.report.ReportEntry;
import org.codehaus.surefire.report.ReporterManager;
import org.codehaus.surefire.battery.AbstractBattery;
import org.python.core.PyList;
import org.python.core.PyStringMap;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class JythonBattery
    extends AbstractBattery
{
    private PythonInterpreter interp;

    private ArrayList testMethods;

    private DirectoryScanner scanner;

    private File directory;

    public JythonBattery()
    {
        interp = new PythonInterpreter();

        testMethods = new ArrayList();

        scanner = new DirectoryScanner();

        directory = new File( "py" );
    }

    public void execute( ReporterManager reportManager )
    {
        scanner.setBasedir( directory );

        scanner.setIncludes( new String[]{ "**/*Test.py" } );

        scanner.scan();

        String files[] = scanner.getIncludedFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            String f = files[i];

            process( f, reportManager );
        }
    }

    public void process( String pyScript, ReporterManager reportManager )
    {
        interp.execfile( ( new File( directory, pyScript ) ).getPath() );

        interp.exec( "battery = " + pyScript.substring( 0, pyScript.indexOf( "." ) ) + "()" );

        PyList l = ( (PyStringMap) interp.get( "battery" ).__findattr__( "__class__" ).__findattr__( "__dict__" ) ).keys();

        int j = l.__len__();

        for ( int i = 0; i < j; i++ )
        {
            String s = l.pop().toString();

            if ( s.startsWith( "test" ) )
            {
                testMethods.add( s );
            }
        }

        executeTestMethods( reportManager );
    }

    protected void executeTestMethods( ReporterManager ReportManager )
    {
        String testMethod;

        for ( Iterator i = testMethods.iterator(); i.hasNext(); )
        {
            testMethod = "battery." + (String) i.next() + "()";

            testMethod( ReportManager, testMethod );
        }

        testMethods.clear();
    }

    protected void testMethod( ReporterManager reportManager, String method )
    {
        ReportEntry reportEntry = new ReportEntry( this, method, "starting" );

        reportManager.testStarting( reportEntry );

        try
        {
            interp.exec( method );

            reportEntry = new ReportEntry( this, method, "succeeded" );

            reportManager.testSucceeded( reportEntry );
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            reportEntry = new ReportEntry( this, method, "failed: " + e.getMessage() );

            reportManager.testFailed( reportEntry );
        }
    }
}
