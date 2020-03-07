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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.maven.doxia.markup.HtmlMarkup;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.reporting.MavenReportException;

import static org.apache.maven.doxia.markup.HtmlMarkup.A;
import static org.apache.maven.doxia.sink.Sink.JUSTIFY_LEFT;
import static org.apache.maven.doxia.sink.SinkEventAttributes.CLASS;
import static org.apache.maven.doxia.sink.SinkEventAttributes.HREF;
import static org.apache.maven.doxia.sink.SinkEventAttributes.ID;
import static org.apache.maven.doxia.sink.SinkEventAttributes.NAME;
import static org.apache.maven.doxia.sink.SinkEventAttributes.STYLE;
import static org.apache.maven.doxia.sink.SinkEventAttributes.TYPE;

/**
 * This generator creates HTML Report from Surefire and Failsafe XML Report.
 */
public final class SurefireReportGenerator
{
    private static final int LEFT = JUSTIFY_LEFT;
    private static final Object[] TAG_TYPE_START = { HtmlMarkup.TAG_TYPE_START };
    private static final Object[] TAG_TYPE_END = { HtmlMarkup.TAG_TYPE_END };

    private final SurefireReportParser report;
    private final boolean showSuccess;
    private final String xrefLocation;
    private List<ReportTestSuite> testSuites;

    public SurefireReportGenerator( List<File> reportsDirectories, Locale locale, boolean showSuccess,
                                    String xrefLocation, ConsoleLogger consoleLogger )
    {
        report = new SurefireReportParser( reportsDirectories, locale, consoleLogger );
        this.showSuccess = showSuccess;
        this.xrefLocation = xrefLocation;
    }

    public void doGenerateReport( LocalizedProperties bundle, Sink sink )
        throws MavenReportException
    {
        testSuites = report.parseXMLReportFiles();

        sink.head();

        sink.title();
        sink.text( bundle.getReportHeader() );
        sink.title_();

        sink.head_();

        sink.body();

        SinkEventAttributeSet atts = new SinkEventAttributeSet();
        atts.addAttribute( TYPE, "application/javascript" );
        sink.unknown( "script", new Object[]{ HtmlMarkup.TAG_TYPE_START }, atts );
        sink.unknown( "cdata", new Object[]{ HtmlMarkup.CDATA_TYPE, javascriptToggleDisplayCode() }, null );
        sink.unknown( "script", new Object[]{ HtmlMarkup.TAG_TYPE_END }, null );

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getReportHeader() );
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

    private void constructSummarySection( LocalizedProperties bundle, Sink sink )
    {
        Map<String, String> summary = report.getSummary( testSuites );

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getReportLabelSummary() );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Summary" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRows( new int[]{ LEFT, LEFT, LEFT, LEFT, LEFT, LEFT }, true );

        sink.tableRow();

        sinkHeader( sink, bundle.getReportLabelTests() );

        sinkHeader( sink, bundle.getReportLabelErrors() );

        sinkHeader( sink, bundle.getReportLabelFailures() );

        sinkHeader( sink, bundle.getReportLabelSkipped() );

        sinkHeader( sink, bundle.getReportLabelSuccessRate() );

        sinkHeader( sink, bundle.getReportLabelTime() );

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
        sink.text( bundle.getReportTextNode1() );
        sink.paragraph_();

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructPackagesSection( LocalizedProperties bundle, Sink sink,
                                           Map<String, List<ReportTestSuite>> suitePackages )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getReportLabelPackageList() );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Package_List" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRows( new int[]{ LEFT, LEFT, LEFT, LEFT, LEFT, LEFT, LEFT }, true );

        sink.tableRow();

        sinkHeader( sink, bundle.getReportLabelPackage() );

        sinkHeader( sink, bundle.getReportLabelTests() );

        sinkHeader( sink, bundle.getReportLabelErrors() );

        sinkHeader( sink, bundle.getReportLabelFailures() );

        sinkHeader( sink, bundle.getReportLabelSkipped() );

        sinkHeader( sink, bundle.getReportLabelSuccessRate() );

        sinkHeader( sink, bundle.getReportLabelTime() );

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
        sink.text( bundle.getReportTextNode2() );
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

                sinkHeader( sink, bundle.getReportLabelClass() );

                sinkHeader( sink, bundle.getReportLabelTests() );

                sinkHeader( sink, bundle.getReportLabelErrors() );

                sinkHeader( sink, bundle.getReportLabelFailures() );

                sinkHeader( sink, bundle.getReportLabelSkipped() );

                sinkHeader( sink, bundle.getReportLabelSuccessRate() );

                sinkHeader( sink, bundle.getReportLabelTime() );

                sink.tableRow_();

                for ( ReportTestSuite suite : testSuiteList )
                {
                    if ( showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0 )
                    {
                        constructTestSuiteSection( sink, numberFormat, suite );
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

    private void constructTestSuiteSection( Sink sink, NumberFormat numberFormat, ReportTestSuite suite )
    {
        sink.tableRow();

        sink.tableCell();

        sink.link( "#" + suite.getPackageName() + '.' + suite.getName() );

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

        sinkCellLink( sink, suite.getName(), "#" + suite.getPackageName() + '.' + suite.getName() );

        sinkCell( sink, Integer.toString( suite.getNumberOfTests() ) );

        sinkCell( sink, Integer.toString( suite.getNumberOfErrors() ) );

        sinkCell( sink, Integer.toString( suite.getNumberOfFailures() ) );

        sinkCell( sink, Integer.toString( suite.getNumberOfSkipped() ) );

        String percentage = report.computePercentage( suite.getNumberOfTests(), suite.getNumberOfErrors(),
                                                      suite.getNumberOfFailures(), suite.getNumberOfSkipped() );
        sinkCell( sink, percentage + "%" );

        sinkCell( sink, numberFormat.format( suite.getTimeElapsed() ) );

        sink.tableRow_();
    }

    private void constructTestCasesSection( LocalizedProperties bundle, Sink sink )
    {
        NumberFormat numberFormat = report.getNumberFormat();

        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getReportLabelTestCases() );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Test_Cases" );

        constructHotLinks( sink, bundle );

        for ( ReportTestSuite suite : testSuites )
        {
            List<ReportTestCase> testCases = suite.getTestCases();

            if ( !testCases.isEmpty() )
            {
                sink.section2();
                sink.sectionTitle2();
                sink.text( suite.getName() );
                sink.sectionTitle2_();

                sinkAnchor( sink, suite.getPackageName() + '.' + suite.getName() );

                boolean showTable = false;

                for ( ReportTestCase testCase : testCases )
                {
                    if ( !testCase.isSuccessful() || showSuccess )
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
                        if ( !testCase.isSuccessful() || showSuccess )
                        {
                            constructTestCaseSection( sink, numberFormat, testCase );
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

    private static void constructTestCaseSection( Sink sink, NumberFormat numberFormat, ReportTestCase testCase )
    {
        sink.tableRow();

        sink.tableCell();

        if ( testCase.getFailureType() != null )
        {
            sink.link( "#" + toHtmlId( testCase.getFullName() ) );

            sinkIcon( testCase.getFailureType(), sink );

            sink.link_();
        }
        else
        {
            sinkIcon( "success", sink );
        }

        sink.tableCell_();

        if ( !testCase.isSuccessful() )
        {
            sink.tableCell();

            sinkAnchor( sink, "TC_" + toHtmlId( testCase.getFullName() ) );

            sinkLink( sink, testCase.getName(), "#" + toHtmlId( testCase.getFullName() ) );

            SinkEventAttributeSet atts = new SinkEventAttributeSet();
            atts.addAttribute( CLASS, "detailToggle" );
            atts.addAttribute( STYLE, "display:inline" );
            sink.unknown( "div", TAG_TYPE_START, atts );

            sinkLink( sink, "javascript:toggleDisplay('" + toHtmlId( testCase.getFullName() ) + "');" );

            atts = new SinkEventAttributeSet();
            atts.addAttribute( STYLE, "display:inline;" );
            atts.addAttribute( ID, toHtmlId( testCase.getFullName() ) + "-off" );
            sink.unknown( "span", TAG_TYPE_START, atts );
            sink.text( " + " );
            sink.unknown( "span", TAG_TYPE_END, null );

            atts = new SinkEventAttributeSet();
            atts.addAttribute( STYLE, "display:none;" );
            atts.addAttribute( ID, toHtmlId( testCase.getFullName() ) + "-on" );
            sink.unknown( "span", TAG_TYPE_START, atts );
            sink.text( " - " );
            sink.unknown( "span", TAG_TYPE_END, null );

            sink.text( "[ Detail ]" );
            sinkLink_( sink );

            sink.unknown( "div", TAG_TYPE_END, null );

            sink.tableCell_();
        }
        else
        {
            sinkCellAnchor( sink, testCase.getName(), "TC_" + toHtmlId( testCase.getFullName() ) );
        }

        sinkCell( sink, numberFormat.format( testCase.getTime() ) );

        sink.tableRow_();

        if ( !testCase.isSuccessful() )
        {
            sink.tableRow();

            sinkCell( sink, "" );
            sinkCell( sink, testCase.getFailureMessage() );
            sinkCell( sink, "" );
            sink.tableRow_();

            String detail = testCase.getFailureDetail();
            if ( detail != null )
            {
                sink.tableRow();
                sinkCell( sink, "" );

                sink.tableCell();
                SinkEventAttributeSet atts = new SinkEventAttributeSet();
                atts.addAttribute( ID, toHtmlId( testCase.getFullName() ) + toHtmlIdFailure( testCase ) );
                atts.addAttribute( STYLE, "display:none;" );
                sink.unknown( "div", TAG_TYPE_START, atts );

                sink.verbatim( null );
                sink.text( detail );
                sink.verbatim_();

                sink.unknown( "div", TAG_TYPE_END, null );
                sink.tableCell_();

                sinkCell( sink, "" );

                sink.tableRow_();
            }
        }
    }

    private static String toHtmlId( String id )
    {
        return DoxiaUtils.isValidId( id ) ? id : DoxiaUtils.encodeId( id, true );
    }

    private void constructFailureDetails( Sink sink, LocalizedProperties bundle, List<ReportTestCase> failures )
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getReportLabelFailureDetails() );
        sink.sectionTitle1_();

        sinkAnchor( sink, "Failure_Details" );

        constructHotLinks( sink, bundle );

        sinkLineBreak( sink );

        sink.table();

        sink.tableRows( new int[]{ LEFT, LEFT }, true );

        for ( ReportTestCase tCase : failures )
        {
            sink.tableRow();

            sink.tableCell();

            String type = tCase.getFailureType();

            sinkIcon( type, sink );

            sink.tableCell_();

            sinkCellAnchor( sink, tCase.getName(), toHtmlId( tCase.getFullName() ) );

            sink.tableRow_();

            String message = tCase.getFailureMessage();

            sink.tableRow();

            sinkCell( sink, "" );

            sinkCell( sink, message == null ? type : type + ": " + message );

            sink.tableRow_();

            String detail = tCase.getFailureDetail();
            if ( detail != null )
            {
                sink.tableRow();

                sinkCell( sink, "" );

                sink.tableCell();
                SinkEventAttributeSet atts = new SinkEventAttributeSet();
                atts.addAttribute( ID, tCase.getName() + toHtmlIdFailure( tCase ) );
                sink.unknown( "div", TAG_TYPE_START, atts );

                String fullClassName = tCase.getFullClassName();
                String errorLineNumber = tCase.getFailureErrorLine();
                if ( xrefLocation != null )
                {
                    String path = fullClassName.replace( '.', '/' );
                    sink.link( xrefLocation + "/" + path + ".html#" + errorLineNumber );
                }
                sink.text( fullClassName + ":" + errorLineNumber );

                if ( xrefLocation != null )
                {
                    sink.link_();
                }
                sink.unknown( "div", TAG_TYPE_END, null );

                sink.tableCell_();

                sink.tableRow_();
            }
        }

        sink.tableRows_();

        sink.table_();

        sinkLineBreak( sink );

        sink.section1_();
    }

    private void constructHotLinks( Sink sink, LocalizedProperties bundle )
    {
        if ( !testSuites.isEmpty() )
        {
            sink.paragraph();

            sink.text( "[" );
            sinkLink( sink, bundle.getReportLabelSummary(), "#Summary" );
            sink.text( "]" );

            sink.text( " [" );
            sinkLink( sink, bundle.getReportLabelPackageList(), "#Package_List" );
            sink.text( "]" );

            sink.text( " [" );
            sinkLink( sink, bundle.getReportLabelTestCases(), "#Test_Cases" );
            sink.text( "]" );

            sink.paragraph_();
        }
    }

    private static String toHtmlIdFailure( ReportTestCase tCase )
    {
        return tCase.hasError() ? "-error" : "-failure";
    }

    private static void sinkLineBreak( Sink sink )
    {
        sink.lineBreak();
    }

    private static void sinkIcon( String type, Sink sink )
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

    private static void sinkHeader( Sink sink, String header )
    {
        sink.tableHeaderCell();
        sink.text( header );
        sink.tableHeaderCell_();
    }

    private static void sinkCell( Sink sink, String text )
    {
        sink.tableCell();
        sink.text( text );
        sink.tableCell_();
    }

    private static void sinkLink( Sink sink, String text, String link )
    {
        sink.link( link );
        sink.text( text );
        sink.link_();
    }

    private static void sinkCellLink( Sink sink, String text, String link )
    {
        sink.tableCell();
        sinkLink( sink, text, link );
        sink.tableCell_();
    }

    private static void sinkCellAnchor( Sink sink, String text, String anchor )
    {
        sink.tableCell();
        sinkAnchor( sink, anchor );
        sink.text( text );
        sink.tableCell_();
    }

    private static void sinkAnchor( Sink sink, String anchor )
    {
        // Dollar '$' for nested classes is not valid character in sink.anchor() and therefore it is ignored
        // https://issues.apache.org/jira/browse/SUREFIRE-1443
        sink.unknown( A.toString(), TAG_TYPE_START, new SinkEventAttributeSet( NAME, anchor ) );
        sink.unknown( A.toString(), TAG_TYPE_END, null );
    }

    private static void sinkLink( Sink sink, String href )
    {
        // The "'" argument in this JavaScript function would be escaped to "&apos;"
        // sink.link( "javascript:toggleDisplay('" + toHtmlId( testCase.getFullName() ) + "');" );
        sink.unknown( A.toString(), TAG_TYPE_START, new SinkEventAttributeSet( HREF, href ) );
    }

    @SuppressWarnings( "checkstyle:methodname" )
    private static void sinkLink_( Sink sink )
    {
        sink.unknown( A.toString(), TAG_TYPE_END, null );
    }

    private static String javascriptToggleDisplayCode()
    {

        // the javascript code is emitted within a commented CDATA section
        // so we have to start with a newline and comment the CDATA closing in the end

        return "\n" + "function toggleDisplay(elementId) {\n"
                + " var elm = document.getElementById(elementId + '-error');\n"
                + " if (elm == null) {\n"
                + "  elm = document.getElementById(elementId + '-failure');\n"
                + " }\n"
                + " if (elm && typeof elm.style != \"undefined\") {\n"
                + "  if (elm.style.display == \"none\") {\n"
                + "   elm.style.display = \"\";\n"
                + "   document.getElementById(elementId + '-off').style.display = \"none\";\n"
                + "   document.getElementById(elementId + '-on').style.display = \"inline\";\n"
                + "  } else if (elm.style.display == \"\") {"
                + "   elm.style.display = \"none\";\n"
                + "   document.getElementById(elementId + '-off').style.display = \"inline\";\n"
                + "   document.getElementById(elementId + '-on').style.display = \"none\";\n"
                + "  } \n"
                + " } \n"
                + " }\n"
                + "//";
    }
}
