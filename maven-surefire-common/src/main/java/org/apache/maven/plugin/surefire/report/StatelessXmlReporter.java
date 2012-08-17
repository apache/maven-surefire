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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.SafeThrowable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

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
 * @author Kristian Rosenvold
 * @see <a href="http://wiki.apache.org/ant/Proposals/EnhancedTestReports">Ant's format enhancement proposal</a>
 *      (not yet implemented by Ant 1.8.2)
 */
public class StatelessXmlReporter
{
    private static final String LS = System.getProperty( "line.separator" );

    private final File reportsDirectory;

    private final String reportNameSuffix;

    private final boolean trimStackTrace;

    public StatelessXmlReporter( File reportsDirectory, String reportNameSuffix, boolean trimStackTrace )
    {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
        this.trimStackTrace = trimStackTrace;
    }

    public void testSetCompleted( WrappedReportEntry testSetReportEntry, TestSetStats testSetStats )
        throws ReporterException
    {

        Xpp3Dom testSuite = createTestSuiteElement( testSetReportEntry, testSetStats, reportNameSuffix );

        showProperties( testSuite );

        testSuite.setAttribute( "tests", String.valueOf( testSetStats.getCompletedCount() ) );

        testSuite.setAttribute( "errors", String.valueOf( testSetStats.getErrors() ) );

        testSuite.setAttribute( "skipped", String.valueOf( testSetStats.getSkipped() ) );

        testSuite.setAttribute( "failures", String.valueOf( testSetStats.getFailures() ) );

        for ( WrappedReportEntry entry : testSetStats.getReportEntries() )
        {
            if ( ReportEntryType.success.equals( entry.getReportEntryType() ) )
            {
                testSuite.addChild( createTestElement( entry, reportNameSuffix ) );
            }
            else
            {
                testSuite.addChild( getTestProblems( entry, trimStackTrace, reportNameSuffix ) );
            }

        }

        File reportFile = getReportFile( testSetReportEntry, reportsDirectory, reportNameSuffix );

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

    private File getReportFile( ReportEntry report, File reportsDirectory, String reportNameSuffix )
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

    private static Xpp3Dom createTestElement( WrappedReportEntry report, String reportNameSuffix )
    {
        Xpp3Dom testCase = new Xpp3Dom( "testcase" );
        testCase.setAttribute( "name", report.getReportName() );
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
        testCase.setAttribute( "time", report.elapsedTimeAsString() );
        return testCase;
    }

    private static Xpp3Dom createTestSuiteElement( WrappedReportEntry report, TestSetStats testSetStats,
                                                   String reportNameSuffix1 )
    {
        Xpp3Dom testCase = new Xpp3Dom( "testsuite" );

        testCase.setAttribute( "name", report.getReportName( reportNameSuffix1 ) );

        if ( report.getGroup() != null )
        {
            testCase.setAttribute( "group", report.getGroup() );
        }
        testCase.setAttribute( "time", testSetStats.getElapsedForTestSet() );
        return testCase;
    }


    private Xpp3Dom getTestProblems( WrappedReportEntry report, boolean trimStackTrace, String reportNameSuffix )
    {

        Xpp3Dom testCase = createTestElement( report, reportNameSuffix );

        Xpp3Dom element = createElement( testCase, report.getReportEntryType().name() );

        String stackTrace = report.getStackTrace( trimStackTrace );

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

        addOutputStreamElement( report.getStdout(), "system-out", testCase );

        addOutputStreamElement( report.getStdErr(), "system-err", testCase );

        return testCase;
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
}
