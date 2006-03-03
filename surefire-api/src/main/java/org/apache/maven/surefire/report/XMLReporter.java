package org.apache.maven.surefire.report;

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

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;


/**
 * XML format reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class XMLReporter
    extends AbstractReporter
{
    private PrintWriter writer;

    private Xpp3Dom testSuite;

    private Xpp3Dom testCase;

    private File reportsDirectory;

    public XMLReporter( File reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public void setTestCase( Xpp3Dom testCase )
    {
        this.testCase = testCase;
    }

    public Xpp3Dom getTestCase()
    {
        return testCase;
    }

    public void writeMessage( String message )
    {
    }

    public void testSetStarting( ReportEntry report )
        throws IOException, UnsupportedEncodingException
    {
        super.testSetStarting( report );

        File reportFile = new File( reportsDirectory, "TEST-" + report.getName() + ".xml" );

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();

        writer = new PrintWriter(
            new BufferedWriter( new OutputStreamWriter( new FileOutputStream( reportFile ), "UTF-8" ) ) );

        writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" );

        testSuite = createTestElement( new Xpp3Dom( "testsuite" ), report.getName(), report );
        showProperties();
    }

    public void testSetCompleted( ReportEntry report )
    {
        super.testSetCompleted( report );

        testSuite.setAttribute( "tests", String.valueOf( this.getNbTests() ) );

        testSuite.setAttribute( "errors", String.valueOf( this.getNumErrors() ) );

        testSuite.setAttribute( "failures", String.valueOf( this.getNumFailures() ) );

        long runTime = System.currentTimeMillis() - testSetStartTime;

        testSuite.setAttribute( "time", elapsedTimeAsString( runTime ) );

        try
        {
            Xpp3DomWriter.write( writer, testSuite );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public void testStarting( ReportEntry report )
    {
        super.testStarting( report );

        String reportName;

        if ( report.getName().indexOf( "(" ) > 0 )
        {
            reportName = report.getName().substring( 0, report.getName().indexOf( "(" ) );
        }
        else
        {
            reportName = report.getName();
        }

        this.testCase = createTestElement( createElement( testSuite, "testcase" ), reportName, report );
    }

    private Xpp3Dom createTestElement( Xpp3Dom element, String reportName, ReportEntry report )
    {
        element.setAttribute( "name", reportName );
        if ( report.getGroup() != null )
        {
            element.setAttribute( "group", report.getGroup() );
        }
        return element;
    }

    public void testSucceeded( ReportEntry report )
    {
        super.testSucceeded( report );

        long runTime = this.endTime - this.startTime;

        testCase.setAttribute( "time", elapsedTimeAsString( runTime ) );
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError( report, stdOut, stdErr );

        writeTestProblems( report, stdOut, stdErr, "error" );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        writeTestProblems( report, stdOut, stdErr, "failure" );
    }

    private void writeTestProblems( ReportEntry report, String stdOut, String stdErr, String name )
    {
        Xpp3Dom element = createElement( testCase, name );

        String stackTrace = getStackTrace( report );

        Throwable t = report.getThrowable();

        if ( t != null )
        {

            String message = t.getMessage();

            if ( message != null && message.trim().length() > 0 )
            {
                element.setAttribute( "message", message );

                element.setAttribute( "type", stackTrace.substring( 0, stackTrace.indexOf( ":" ) ) );
            }
            else
            {
                element.setAttribute( "type", new StringTokenizer( stackTrace ).nextToken() );
            }
        }

        element.setValue( stackTrace );

        addOutputStreamElement( stdOut, "system-out" );

        addOutputStreamElement( stdErr, "system-err" );

        long runTime = endTime - startTime;

        testCase.setAttribute( "time", elapsedTimeAsString( runTime ) );
    }

    private void addOutputStreamElement( String stdOut, String name )
    {
        if ( stdOut != null && stdOut.trim().length() > 0 )
        {
            createElement( testCase, name ).setValue( stdOut );
        }
    }

    private Xpp3Dom createElement( Xpp3Dom element, String name )
    {
        Xpp3Dom component = new Xpp3Dom( name );

        element.addChild( component );

        return component;
    }

    /**
     * Adds system properties to the XML report.
     */
    private void showProperties()
    {
        Xpp3Dom properties = createElement( testSuite, "properties" );

        Properties systemProperties = System.getProperties();

        if ( systemProperties != null )
        {
            Enumeration propertyKeys = systemProperties.propertyNames();

            while ( propertyKeys.hasMoreElements() )
            {
                String key = (String) propertyKeys.nextElement();

                String value = systemProperties.getProperty( key );

                if ( value == null )
                {
                    value = "null";
                }

                Xpp3Dom property = createElement( properties, "property" );

                property.setAttribute( "name", key );

                property.setAttribute( "value", value );

            }
        }
    }

}
