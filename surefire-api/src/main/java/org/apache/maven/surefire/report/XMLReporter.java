package org.apache.maven.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.util.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
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
    private static final String LS = System.getProperty( "line.separator" );

    private File reportsDirectory;

    private List results = Collections.synchronizedList( new ArrayList() );

    public XMLReporter( File reportsDirectory, Boolean trimStackTrace )
    {
        super( trimStackTrace );

        this.reportsDirectory = reportsDirectory;
    }

    public void writeMessage( String message )
    {
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        super.testSetCompleted( report );

        long runTime = System.currentTimeMillis() - testSetStartTime;

        Xpp3Dom testSuite = createTestElement( "testsuite", report, runTime );

        showProperties( testSuite );

        testSuite.setAttribute( "tests", String.valueOf( this.getNumTests() ) );

        testSuite.setAttribute( "errors", String.valueOf( this.getNumErrors() ) );

        testSuite.setAttribute( "skipped", String.valueOf( this.getNumSkipped() ) );

        testSuite.setAttribute( "failures", String.valueOf( this.getNumFailures() ) );

        for ( Iterator i = results.iterator(); i.hasNext(); )
        {
            Xpp3Dom testcase = (Xpp3Dom) i.next();
            testSuite.addChild( testcase );
        }

        File reportFile = new File( reportsDirectory, "TEST-" + report.getName() + ".xml" );

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();

        PrintWriter writer = null;

        try
        {
            writer = new PrintWriter(
                new BufferedWriter( new OutputStreamWriter( new FileOutputStream( reportFile ), "UTF-8" ) ) );

            writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + LS );

            Xpp3DomWriter.write( new PrettyPrintXMLWriter( writer ), testSuite );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new ReporterException( "Unable to use UTF-8 encoding", e );
        }
        catch ( FileNotFoundException e )
        {
            throw new ReporterException( "Unable to create file: " + e.getMessage(), e );
        }

        finally
        {
            IOUtil.close( writer );
        }
    }

    private String getReportName( ReportEntry report )
    {
        String reportName;

        if ( report.getName().indexOf( "(" ) > 0 )
        {
            reportName = report.getName().substring( 0, report.getName().indexOf( "(" ) );
        }
        else
        {
            reportName = report.getName();
        }
        return reportName;
    }

    public void testSucceeded( ReportEntry report )
    {
        super.testSucceeded( report );

        long runTime = this.endTime - this.startTime;

        Xpp3Dom testCase = createTestElement( "testcase", report, runTime );

        results.add( testCase );
    }

    private Xpp3Dom createTestElement( String name, ReportEntry report, long runTime )
    {
        Xpp3Dom testCase = new Xpp3Dom( name );
        testCase.setAttribute( "name", getReportName( report ) );
        if ( report.getGroup() != null )
        {
            testCase.setAttribute( "group", report.getGroup() );
        }
        testCase.setAttribute( "time", elapsedTimeAsString( runTime ) );
        return testCase;
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
        long runTime = endTime - startTime;

        Xpp3Dom testCase = createTestElement( "testcase", report, runTime );

        Xpp3Dom element = createElement( testCase, name );

        String stackTrace = getStackTrace( report );

        Throwable t = report.getStackTraceWriter().getThrowable();

        if ( t != null )
        {

            String message = t.getMessage();

            if ( message != null && message.trim().length() > 0 )
            {
                element.setAttribute( "message", escapeAttribute( message ) );

                element.setAttribute( "type", stackTrace.substring( 0, stackTrace.indexOf( ":" ) ) );
            }
            else
            {
                element.setAttribute( "type", new StringTokenizer( stackTrace ).nextToken() );
            }
        }

        element.setValue( stackTrace );

        addOutputStreamElement( stdOut, "system-out", testCase );

        addOutputStreamElement( stdErr, "system-err", testCase );

        results.add( testCase );
    }

    private void addOutputStreamElement( String stdOut, String name, Xpp3Dom testCase )
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
     *
     * @param testSuite
     */
    private void showProperties( Xpp3Dom testSuite )
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

                property.setAttribute( "value", escapeAttribute( value ) );

            }
        }
    }

    private static String escapeAttribute( String attribute )
    {
        // Shouldn't Xpp3Dom do this itself?
        String s = StringUtils.replace( attribute, "<", "&lt;" );
        s = StringUtils.replace( s, ">", "&gt;" );
        return s;

    }

    public Iterator getResults()
    {
        return results.iterator();
    }

    public void reset()
    {
        results.clear();
        super.reset();
    }
}
