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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    private long batteryStartTime;

    public void setTestCase( Xpp3Dom testCase )
    {
        this.testCase = testCase;
    }

    public Xpp3Dom getTestCase()
    {
        return testCase;
    }

    public void batteryStarting( ReportEntry report )
        throws FileNotFoundException, UnsupportedEncodingException
    {
        batteryStartTime = System.currentTimeMillis();

        File reportFile = new File( getReportsDirectory(), "TEST-" + report.getName() + ".xml" );

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();

        writer = new PrintWriter(
            new BufferedWriter( new OutputStreamWriter( new FileOutputStream( reportFile ), "UTF-8" ) ) );

        writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" );

        testSuite = new Xpp3Dom( "testsuite" );

        testSuite.setAttribute( "name", report.getName() );
        if ( report.getGroup() != null )
        {
            testSuite.setAttribute( "group", report.getGroup() );
        }
        showProperties();
    }

    public void batteryCompleted( ReportEntry report )
    {
        testSuite.setAttribute( "tests", String.valueOf( this.getNbTests() ) );

        testSuite.setAttribute( "errors", String.valueOf( this.getNbErrors() ) );

        testSuite.setAttribute( "failures", String.valueOf( this.getNbFailures() ) );

        long runTime = System.currentTimeMillis() - this.batteryStartTime;

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

        testCase = createElement( testSuite, "testcase" );

        testCase.setAttribute( "name", reportName );
        if ( report.getGroup() != null )
        {
            testCase.setAttribute( "group", report.getGroup() );
        }
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

        Xpp3Dom element = createElement( testCase, "error" );

        writeTestProblems( report, stdOut, stdErr, element );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        Xpp3Dom element = createElement( testCase, "failure" );

        writeTestProblems( report, stdOut, stdErr, element );
    }

    private void writeTestProblems( ReportEntry report, String stdOut, String stdErr, Xpp3Dom element )
    {

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

    public void dispose()
    {
        errors = 0;

        failures = 0;

        completedCount = 0;
    }

    private Xpp3Dom createElement( Xpp3Dom element, String name )
    {
        Xpp3Dom component = new Xpp3Dom( name );

        element.addChild( component );

        return component;
    }

    /**
     * Returns stacktrace as String.
     *
     * @param report ReportEntry object.
     * @return stacktrace as string.
     */
    private String getStackTrace( ReportEntry report )
    {
        StringWriter writer = new StringWriter();

        report.getThrowable().printStackTrace( new PrintWriter( writer ) );

        writer.flush();

        return writer.toString();
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
