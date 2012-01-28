package org.apache.maven.plugin.surefire.report;

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
import org.apache.maven.surefire.report.DescriptionDecoder;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.SafeThrowable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

/**
 * XML format reporter writing to <code>TEST-<i>reportName</i>[-<i>suffix</i>].xml</code> file like written and read
 * by Ant's <a href="http://ant.apache.org/manual/Tasks/junit.html"><code>&lt;junit&gt;</code></a> and
 * <a href="http://ant.apache.org/manual/Tasks/junitreport.html"><code>&lt;junitreport&gt;</code></a> tasks,
 * then supported by many tools like CI servers.
 * <p/>
 * <pre>&lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;testsuite name="<i>suite name</i>" [group="<i>group</i>"] tests="<i>0</i>" failures="<i>0</i>" errors="<i>0</i>" skipped="<i>0</i>" time="<i>0,###.###</i>">
 *  &lt;properties>
 *    &lt;property name="<i>name</i>" value="<i>value</i>"/>
 *    [...]
 *  &lt;/properties>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]"/>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]">
 *    &lt;<b>error</b> message="<i>message</i>" type="<i>exception class name</i>"><i>stacktrace</i>&lt;/error>
 *    &lt;system-out><i>system out content (present only if not empty)</i>&lt;/system-out>
 *    &lt;system-err><i>system err content (present only if not empty)</i>&lt;/system-err>
 *  &lt;/testcase>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]">
 *    &lt;<b>failure</b> message="<i>message</i>" type="<i>exception class name</i>"><i>stacktrace</i>&lt;/failure>
 *    &lt;system-out><i>system out content (present only if not empty)</i>&lt;/system-out>
 *    &lt;system-err><i>system err content (present only if not empty)</i>&lt;/system-err>
 *  &lt;/testcase>
 *  &lt;testcase time="<i>0,###.###</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]">
 *    &lt;<b>skipped</b>/>
 *  &lt;/testcase>
 *  [...]</pre>
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 * @see <a href="http://wiki.apache.org/ant/Proposals/EnhancedTestReports">Ant's format enhancement proposal</a>
 *      (not yet implemented by Ant 1.8.2)
 */
public class XMLReporter
    extends AbstractReporter
{
    private static final String LS = System.getProperty( "line.separator" );

    private final File reportsDirectory;

    private final boolean deleteOnStarting;

    private final String reportNameSuffix;

    private final List<Xpp3Dom> results = Collections.synchronizedList( new ArrayList<Xpp3Dom>() );

    private static final DescriptionDecoder decoder = new DescriptionDecoder();

    private int elapsed = 0;

    public XMLReporter( boolean trimStackTrace, File reportsDirectory )
    {
        this( trimStackTrace, reportsDirectory, null );
    }

    public XMLReporter( boolean trimStackTrace, File reportsDirectory, String reportNameSuffix )
    {
        super( trimStackTrace );
        this.reportsDirectory = reportsDirectory;
        this.deleteOnStarting = false;
        this.reportNameSuffix = reportNameSuffix;
    }


    public void writeMessage( String message )
    {
    }

    public void writeMessage( byte[] b, int off, int len )
    {
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        super.testSetStarting( report );

        if ( deleteOnStarting )
        {
            final File reportFile = getReportFile( report );
            deleteIfExisting( reportFile );
        }
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        super.testSetCompleted( report );

        long runTime = elapsed > 0 ? elapsed : ( System.currentTimeMillis() - testSetStartTime );

        Xpp3Dom testSuite = createTestSuiteElement( report, runTime );

        showProperties( testSuite );

        testSuite.setAttribute( "tests", String.valueOf( this.getNumTests() ) );

        testSuite.setAttribute( "errors", String.valueOf( this.getNumErrors() ) );

        testSuite.setAttribute( "skipped", String.valueOf( this.getNumSkipped() ) );

        testSuite.setAttribute( "failures", String.valueOf( this.getNumFailures() ) );

        for ( Object result : results )
        {
            Xpp3Dom testcase = (Xpp3Dom) result;
            testSuite.addChild( testcase );
        }

        File reportFile = getReportFile( report );

        File reportDir = reportFile.getParentFile();

        //noinspection ResultOfMethodCallIgnored
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

    private File getReportFile( ReportEntry report )
    {
        File reportFile;

        if ( reportNameSuffix != null && reportNameSuffix.length() > 0 )
        {
            reportFile = new File( reportsDirectory, "TEST-" + report.getName() + "-" + reportNameSuffix + ".xml" );
        }
        else
        {
            reportFile = new File( reportsDirectory, "TEST-" + report.getName() + ".xml" );
        }

        return reportFile;
    }

    public void testSucceeded( ReportEntry report )
    {
        super.testSucceeded( report );

        long runTime = getActualRunTime( report );

        Xpp3Dom testCase = createTestElement( report, runTime );

        results.add( testCase );
    }

    private Xpp3Dom createTestElement( ReportEntry report, long runTime )
    {
        elapsed += report.getElapsed();
        Xpp3Dom testCase = new Xpp3Dom( "testcase" );
        testCase.setAttribute( "name", decoder.getReportName( report ) );
        if ( report.getGroup() != null )
        {
            testCase.setAttribute( "group", report.getGroup() );
        }
        if ( report.getSourceName() != null )
        {
            if ( reportNameSuffix != null && reportNameSuffix.length() > 0 )
            {
                testCase.setAttribute( "classname", report.getSourceName() + "(" + reportNameSuffix + ")" );
            }
            else
            {
                testCase.setAttribute( "classname", report.getSourceName() );
            }
        }
        testCase.setAttribute( "time", elapsedTimeAsString( runTime ) );
        return testCase;
    }

    private Xpp3Dom createTestSuiteElement( ReportEntry report, long runTime )
    {
        Xpp3Dom testCase = new Xpp3Dom( "testsuite" );

        if ( reportNameSuffix != null && reportNameSuffix.length() > 0 )
        {
            testCase.setAttribute( "name", decoder.getReportName( report ) + "(" + reportNameSuffix + ")" );
        }
        else
        {
            testCase.setAttribute( "name", decoder.getReportName( report ) );
        }
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

    public void testSkipped( ReportEntry report )
    {
        super.testSkipped( report );
        writeTestProblems( report, null, null, "skipped" );
    }

    private void writeTestProblems( ReportEntry report, String stdOut, String stdErr, String name )
    {
        long runTime = getActualRunTime( report );

        Xpp3Dom testCase = createTestElement( report, runTime );

        Xpp3Dom element = createElement( testCase, name );

        String stackTrace = getStackTrace( report );

        if ( report.getMessage() != null && report.getMessage().length() > 0 )
        {
            element.setAttribute( "message", report.getMessage() );
        }

        if ( report.getStackTraceWriter() != null )
        {
            //noinspection ThrowableResultOfMethodCallIgnored
            SafeThrowable t = report.getStackTraceWriter().getThrowable();
            if ( t != null )
            {
                if ( t.getMessage() != null )
                {
                    element.setAttribute( "type", ( stackTrace.contains( ":" )
                        ? stackTrace.substring( 0, stackTrace.indexOf( ":" ) )
                        : stackTrace ) );
                }
                else
                {
                    element.setAttribute( "type", new StringTokenizer( stackTrace ).nextToken() );
                }
            }
        }

        if ( stackTrace != null )
        {
            element.setValue( stackTrace );
        }

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
     * <p/>
     *
     * @param testSuite The test suite to report to
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

                property.setAttribute( "value", value );

            }
        }
    }

    public Iterator getResults()
    {
        return results.iterator();
    }

    public void reset()
    {
        results.clear();
        elapsed = 0;
        super.reset();
    }
}
