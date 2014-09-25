package org.apache.maven.plugins.surefire.report;

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

import java.io.File;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.maven.doxia.markup.HtmlMarkup;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkEventAttributeSet;
import org.apache.maven.doxia.sink.SinkEventAttributes;
import org.apache.maven.reporting.MavenReportException;

/**
 *
 */
public class SurefireReportGenerator
{
    private final SurefireReportParser report;

    private List<ReportTestSuite> testSuites;

    private final boolean showSuccess;

    private final String xrefLocation;

    private static final int LEFT = Sink.JUSTIFY_LEFT;

    public SurefireReportGenerator( List<File> reportsDirectories, Locale locale, boolean showSuccess,
                                    String xrefLocation )
    {
        report = new SurefireReportParser( reportsDirectories, locale );

        this.xrefLocation = xrefLocation;

        this.showSuccess = showSuccess;
    }

    public void doGenerateReport( ResourceBundle bundle, Sink sink )
        throws MavenReportException
    {
        testSuites = report.parseXMLReportFiles();

        sink.head();

        sink.title();
        sink.text( bundle.getString( "report.surefire.header" ) );
        sink.title_();

        sink.head_();

        sink.body();

        SinkEventAttributeSet atts = new SinkEventAttributeSet();
        atts.addAttribute( SinkEventAttributes.TYPE, "text/javascript" );
        sink.unknown( "script", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );
        sink.unknown( "cdata", new Object[]{ HtmlMarkup.CDATA_TYPE, javascriptToggleDisplayCode() }, null );
        sink.unknown( "script", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.header" ) );
        sink.sectionTitle1_();
        sink.section1_();

        constructSummarySection( bundle, sink );

        Map<String, List<ReportTestSuite>> suitePackages = report.getSuitesGroupByPackage( testSuites );
        if ( !suitePackages.isEmpty() )
        {
            constructPackagesSection( bundle, sink, suitePackages );
        }

        if ( !testSuites.isEmpty() )
        {
            constructTestCasesSection( bundle, sink );
        }

        List<ReportTestCase> failureList = report.getFailureDetails( testSuites );
        if ( !failureList.isEmpty() )
        {
            constructFailureDetails( sink, bundle, failureList );
        }

        sink.body_();

        sink.flush();

        sink.close();
    }

    private void constructSummarySection( ResourceBundle bundle, Sink sink )
    {
        Map<String, String> summary = report.getSummary( testSuites );

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.label.summary" ) );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Summary" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRows( new int[]{ LEFT, LEFT, LEFT, LEFT, LEFT, LEFT }, true );

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.skipped" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

        sink.tableRow_();

        sink.tableRow();

        sinkCell( sink, summary.get( "totalTests" ) );

        sinkCell( sink, summary.get( "totalErrors" ) );

        sinkCell( sink, summary.get( "totalFailures" ) );

        sinkCell( sink, summary.get( "totalSkipped" ) );

        sinkCell( sink, summary.get( "totalPercentage" ) + "%" );

        sinkCell( sink, summary.get( "totalElapsedTime" ) );

        sink.tableRow_();

        sink.tableRows_();

        sink.table_();

        sink.lineBreak();

        sink.paragraph();
        sink.text( bundle.getString( "report.surefire.text.note1" ) );
        sink.paragraph_();

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructPackagesSection( ResourceBundle bundle, Sink sink,
                                           Map<String, List<ReportTestSuite>> suitePackages )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.label.packagelist" ) );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Package_List" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRows( new int[]{ LEFT, LEFT, LEFT, LEFT, LEFT, LEFT, LEFT }, true );

        sink.tableRow();

        sinkHeader( sink, bundle.getString( "report.surefire.label.package" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.skipped" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

        sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

        sink.tableRow_();

        for ( Map.Entry<String, List<ReportTestSuite>> entry : suitePackages.entrySet() )
        {
            sink.tableRow();

            String packageName = entry.getKey();

            List<ReportTestSuite> testSuiteList = entry.getValue();

            Map<String, String> packageSummary = report.getSummary( testSuiteList );

            sinkCellLink( sink, packageName, "#" + packageName );

            sinkCell( sink, packageSummary.get( "totalTests" ) );

            sinkCell( sink, packageSummary.get( "totalErrors" ) );

            sinkCell( sink, packageSummary.get( "totalFailures" ) );

            sinkCell( sink, packageSummary.get( "totalSkipped" ) );

            sinkCell( sink, packageSummary.get( "totalPercentage" ) + "%" );

            sinkCell( sink, packageSummary.get( "totalElapsedTime" ) );

            sink.tableRow_();
        }

        sink.tableRows_();

        sink.table_();

        sink.lineBreak();

        sink.paragraph();
        sink.text( bundle.getString( "report.surefire.text.note2" ) );
        sink.paragraph_();

        for ( Map.Entry<String, List<ReportTestSuite>> entry : suitePackages.entrySet() )
        {
            String packageName = entry.getKey();

            List<ReportTestSuite> testSuiteList = entry.getValue();

            sink.section2();
            sink.sectionTitle2();
            sink.text( packageName );
            sink.sectionTitle2_();

            sinkAnchor( sink, packageName );

            boolean showTable = false;

            for ( ReportTestSuite suite : testSuiteList )
            {
                if ( showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0 )
                {
                    showTable = true;

                    break;
                }
            }

            if ( showTable )
            {
                sink.table();

                sink.tableRows( new int[]{ LEFT, LEFT, LEFT, LEFT, LEFT, LEFT, LEFT, LEFT }, true );

                sink.tableRow();

                sinkHeader( sink, "" );

                sinkHeader( sink, bundle.getString( "report.surefire.label.class" ) );

                sinkHeader( sink, bundle.getString( "report.surefire.label.tests" ) );

                sinkHeader( sink, bundle.getString( "report.surefire.label.errors" ) );

                sinkHeader( sink, bundle.getString( "report.surefire.label.failures" ) );

                sinkHeader( sink, bundle.getString( "report.surefire.label.skipped" ) );

                sinkHeader( sink, bundle.getString( "report.surefire.label.successrate" ) );

                sinkHeader( sink, bundle.getString( "report.surefire.label.time" ) );

                sink.tableRow_();

                for ( ReportTestSuite suite : testSuiteList )
                {
                    if ( showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0 )
                    {

                        sink.tableRow();

                        sink.tableCell();

                        sink.link( "#" + suite.getPackageName() + suite.getName() );

                        if ( suite.getNumberOfErrors() > 0 )
                        {
                            sinkIcon( "error", sink );
                        }
                        else if ( suite.getNumberOfFailures() > 0 )
                        {
                            sinkIcon( "junit.framework", sink );
                        }
                        else if ( suite.getNumberOfSkipped() > 0 )
                        {
                            sinkIcon( "skipped", sink );
                        }
                        else
                        {
                            sinkIcon( "success", sink );
                        }

                        sink.link_();

                        sink.tableCell_();

                        sinkCellLink( sink, suite.getName(), "#" + suite.getPackageName() + suite.getName() );

                        sinkCell( sink, Integer.toString( suite.getNumberOfTests() ) );

                        sinkCell( sink, Integer.toString( suite.getNumberOfErrors() ) );

                        sinkCell( sink, Integer.toString( suite.getNumberOfFailures() ) );

                        sinkCell( sink, Integer.toString( suite.getNumberOfSkipped() ) );

                        String percentage =
                            report.computePercentage( suite.getNumberOfTests(), suite.getNumberOfErrors(),
                                                      suite.getNumberOfFailures(), suite.getNumberOfSkipped() );
                        sinkCell( sink, percentage + "%" );

                        sinkCell( sink, numberFormat.format( suite.getTimeElapsed() ) );

                        sink.tableRow_();
                    }
                }

                sink.tableRows_();

                sink.table_();
            }

            sink.section2_();
        }

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructTestCasesSection( ResourceBundle bundle, Sink sink )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.surefire.label.testcases" ) );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Test_Cases" );

        constructHotLinks( sink, bundle );

        for ( ReportTestSuite suite : testSuites )
        {
            List<ReportTestCase> testCases = suite.getTestCases();

            if ( testCases != null && !testCases.isEmpty() )
            {
                sink.section2();
                sink.sectionTitle2();
                sink.text( suite.getName() );
                sink.sectionTitle2_();

                sinkAnchor( sink, suite.getPackageName() + suite.getName() );

                boolean showTable = false;

                for ( ReportTestCase testCase : testCases )
                {
                    if ( testCase.getFailure() != null || showSuccess )
                    {
                        showTable = true;

                        break;
                    }
                }

                if ( showTable )
                {
                    sink.table();

                    sink.tableRows( new int[]{ LEFT, LEFT, LEFT }, true );

                    for ( ReportTestCase testCase : testCases )
                    {
                        if ( testCase.getFailure() != null || showSuccess )
                        {
                            sink.tableRow();

                            sink.tableCell();

                            Map<String, Object> failure = testCase.getFailure();

                            if ( failure != null )
                            {
                                sink.link( "#" + testCase.getFullName() );

                                sinkIcon( (String) failure.get( "type" ), sink );

                                sink.link_();
                            }
                            else
                            {
                                sinkIcon( "success", sink );
                            }

                            sink.tableCell_();

                            if ( failure != null )
                            {
                                sink.tableCell();

                                sinkLink( sink, testCase.getName(), "#" + testCase.getFullName() );

                                SinkEventAttributeSet atts = new SinkEventAttributeSet();
                                atts.addAttribute( SinkEventAttributes.CLASS, "detailToggle" );
                                atts.addAttribute( SinkEventAttributes.STYLE, "display:inline" );
                                sink.unknown( "div", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );

                                sink.link( "javascript:toggleDisplay('" + toHtmlId( testCase.getFullName() ) + "');" );

                                atts = new SinkEventAttributeSet();
                                atts.addAttribute( SinkEventAttributes.STYLE, "display:inline;" );
                                atts.addAttribute( SinkEventAttributes.ID, toHtmlId( testCase.getFullName() ) + "off" );
                                sink.unknown( "span", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );
                                sink.text( " + " );
                                sink.unknown( "span", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );

                                atts = new SinkEventAttributeSet();
                                atts.addAttribute( SinkEventAttributes.STYLE, "display:none;" );
                                atts.addAttribute( SinkEventAttributes.ID, toHtmlId( testCase.getFullName() ) + "on" );
                                sink.unknown( "span", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );
                                sink.text( " - " );
                                sink.unknown( "span", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );

                                sink.text( "[ Detail ]" );
                                sink.link_();

                                sink.unknown( "div", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );

                                sink.tableCell_();
                            }
                            else
                            {
                                sinkCell( sink, testCase.getName() );
                            }

                            sinkCell( sink, numberFormat.format( testCase.getTime() ) );

                            sink.tableRow_();

                            if ( failure != null )
                            {
                                sink.tableRow();

                                sinkCell( sink, "" );
                                sinkCell( sink, (String) failure.get( "message" ) );
                                sinkCell( sink, "" );
                                sink.tableRow_();

                                List<String> detail = (List<String>) failure.get( "detail" );
                                if ( detail != null )
                                {

                                    sink.tableRow();
                                    sinkCell( sink, "" );

                                    sink.tableCell();
                                    SinkEventAttributeSet atts = new SinkEventAttributeSet();
                                    atts.addAttribute( SinkEventAttributes.ID,
                                                       toHtmlId( testCase.getFullName() ) + "error" );
                                    atts.addAttribute( SinkEventAttributes.STYLE, "display:none;" );
                                    sink.unknown( "div", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );

                                    sink.verbatim( null );
                                    for ( String line : detail )
                                    {
                                        sink.text( line );
                                        sink.lineBreak();
                                    }
                                    sink.verbatim_();

                                    sink.unknown( "div", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );
                                    sink.tableCell_();

                                    sinkCell( sink, "" );

                                    sink.tableRow_();
                                }
                            }
                        }
                    }

                    sink.tableRows_();

                    sink.table_();
                }

                sink.section2_();
            }
        }

        sinkLineBreak( sink );

        sink.section1_();
    }


    private String toHtmlId( String id )
    {
        return id.replace( ".", "_" );
    }

    private void constructFailureDetails( Sink sink, ResourceBundle bundle, List<ReportTestCase> failureList )
    {
        Iterator<ReportTestCase> failIter = failureList.iterator();

        if ( failIter != null )
        {
            sink.section1();
            sink.sectionTitle1();
            sink.text( bundle.getString( "report.surefire.label.failuredetails" ) );
            sink.sectionTitle1_();

            sinkAnchor( sink, "Failure_Details" );

            constructHotLinks( sink, bundle );

            sinkLineBreak( sink );

            sink.table();

            sink.tableRows( new int[]{ LEFT, LEFT }, true );

            while ( failIter.hasNext() )
            {
                ReportTestCase tCase = failIter.next();

                Map<String, Object> failure = tCase.getFailure();

                sink.tableRow();

                sink.tableCell();

                String type = (String) failure.get( "type" );
                sinkIcon( type, sink );

                sink.tableCell_();

                sinkCellAnchor( sink, tCase.getName(), tCase.getFullName() );

                sink.tableRow_();

                String message = (String) failure.get( "message" );

                sink.tableRow();

                sinkCell( sink, "" );

                StringBuilder sb = new StringBuilder();
                sb.append( type );

                if ( message != null )
                {
                    sb.append( ": " );
                    sb.append( message );
                }

                sinkCell( sink, sb.toString() );

                sink.tableRow_();

                List<String> detail = (List<String>) failure.get( "detail" );
                if ( detail != null )
                {
                    boolean firstLine = true;

                    String techMessage = "";
                    for ( String line : detail )
                    {
                        techMessage = line;
                        if ( firstLine )
                        {
                            firstLine = false;
                        }
                        else
                        {
                            sink.text( "    " );
                        }
                    }

                    sink.tableRow();

                    sinkCell( sink, "" );

                    sink.tableCell();
                    SinkEventAttributeSet atts = new SinkEventAttributeSet();
                    atts.addAttribute( SinkEventAttributes.ID, tCase.getName() + "error" );
                    sink.unknown( "div", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );

                    if ( xrefLocation != null )
                    {
                        String path = tCase.getFullClassName().replace( '.', '/' );

                        sink.link( xrefLocation + "/" + path + ".html#" +
                                       getErrorLineNumber( tCase.getFullName(), techMessage ) );
                    }
                    sink.text(
                        tCase.getFullClassName() + ":" + getErrorLineNumber( tCase.getFullName(), techMessage ) );

                    if ( xrefLocation != null )
                    {
                        sink.link_();
                    }
                    sink.unknown( "div", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );

                    sink.tableCell_();

                    sink.tableRow_();
                }
            }

            sink.tableRows_();

            sink.table_();
        }

        sinkLineBreak( sink );

        sink.section1_();
    }

    private String getErrorLineNumber( String className, String source )
    {
        StringTokenizer tokenizer = new StringTokenizer( source );

        while ( tokenizer.hasMoreTokens() )
        {
            String token = tokenizer.nextToken();
            if ( token.startsWith( className ) )
            {
                int idx = token.indexOf( ":" );
                if ( idx >= 0 )
                {
                    int closeIdx = token.lastIndexOf( ")" );

                    if ( closeIdx > idx + 1 )
                    {
                        return token.substring( idx + 1, closeIdx );
                    }
                }
            }
        }
        return "";
    }

    private void constructHotLinks( Sink sink, ResourceBundle bundle )
    {
        if ( !testSuites.isEmpty() )
        {
            sink.paragraph();

            sink.text( "[" );
            sinkLink( sink, bundle.getString( "report.surefire.label.summary" ), "#Summary" );
            sink.text( "]" );

            sink.text( " [" );
            sinkLink( sink, bundle.getString( "report.surefire.label.packagelist" ), "#Package_List" );
            sink.text( "]" );

            sink.text( " [" );
            sinkLink( sink, bundle.getString( "report.surefire.label.testcases" ), "#Test_Cases" );
            sink.text( "]" );

            sink.paragraph_();
        }
    }

    private void sinkLineBreak( Sink sink )
    {
        sink.lineBreak();
    }

    private void sinkIcon( String type, Sink sink )
    {
        sink.figure();

        if ( type.startsWith( "junit.framework" ) || "skipped".equals( type ) )
        {
            sink.figureGraphics( "images/icon_warning_sml.gif" );
        }
        else if ( type.startsWith( "success" ) )
        {
            sink.figureGraphics( "images/icon_success_sml.gif" );
        }
        else
        {
            sink.figureGraphics( "images/icon_error_sml.gif" );
        }

        sink.figure_();
    }

    private void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();
        sink.text( header );
        sink.tableHeaderCell_();
    }

    private void sinkCell( Sink sink, String text )
    {
        sink.tableCell();
        sink.text( text );
        sink.tableCell_();
    }

    private void sinkLink( Sink sink, String text, String link )
    {
        sink.link( link );
        sink.text( text );
        sink.link_();
    }

    private void sinkCellLink( Sink sink, String text, String link )
    {
        sink.tableCell();
        sinkLink( sink, text, link );
        sink.tableCell_();
    }

    private void sinkCellAnchor( Sink sink, String text, String anchor )
    {
        sink.tableCell();
        sinkAnchor( sink, anchor );
        sink.text( text );
        sink.tableCell_();
    }

    private void sinkAnchor( Sink sink, String anchor )
    {
        sink.anchor( anchor );
        sink.anchor_();
    }

    private static String javascriptToggleDisplayCode()
    {
        final StringBuilder str = new StringBuilder( 64 );

        // the javascript code is emitted within a commented CDATA section
        // so we have to start with a newline and comment the CDATA closing in the end
        str.append( "\n" );
        str.append( "function toggleDisplay(elementId) {\n" );
        str.append( " var elm = document.getElementById(elementId + 'error');\n" );
        str.append( " if (elm && typeof elm.style != \"undefined\") {\n" );
        str.append( " if (elm.style.display == \"none\") {\n" );
        str.append( " elm.style.display = \"\";\n" );
        str.append( " document.getElementById(elementId + 'off').style.display = \"none\";\n" );
        str.append( " document.getElementById(elementId + 'on').style.display = \"inline\";\n" );
        str.append( " }" );
        str.append( " else if (elm.style.display == \"\") {" );
        str.append( " elm.style.display = \"none\";\n" );
        str.append( " document.getElementById(elementId + 'off').style.display = \"inline\";\n" );
        str.append( " document.getElementById(elementId + 'on').style.display = \"none\";\n" );
        str.append( " } \n" );
        str.append( " } \n" );
        str.append( " }\n" );
        str.append( "//" );

        return str.toString();
    }
}
