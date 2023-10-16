/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.surefire.report;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.markup.HtmlMarkup;
import org.apache.maven.doxia.markup.Markup;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.impl.SinkEventAttributeSet;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

import static org.apache.maven.doxia.markup.HtmlMarkup.A;
import static org.apache.maven.doxia.sink.SinkEventAttributes.CLASS;
import static org.apache.maven.doxia.sink.SinkEventAttributes.HREF;
import static org.apache.maven.doxia.sink.SinkEventAttributes.ID;
import static org.apache.maven.doxia.sink.SinkEventAttributes.STYLE;

/**
 * This generator creates HTML Report from Surefire and Failsafe XML Report.
 */
public class SurefireReportRenderer extends AbstractMavenReportRenderer {
    private static final Object[] TAG_TYPE_START = {HtmlMarkup.TAG_TYPE_START};
    private static final Object[] TAG_TYPE_END = {HtmlMarkup.TAG_TYPE_END};

    private final I18N i18n;
    private final String i18nSection;
    private final Locale locale;

    private final SurefireReportParser parser;
    private final boolean showSuccess;
    private final String xrefLocation;
    private final List<ReportTestSuite> testSuites;

    public SurefireReportRenderer(
            Sink sink,
            I18N i18n,
            String i18nSection,
            Locale locale,
            ConsoleLogger consoleLogger,
            boolean showSuccess,
            List<File> reportsDirectories,
            String xrefLocation) {
        super(sink);
        this.i18n = i18n;
        this.i18nSection = i18nSection;
        this.locale = locale;
        parser = new SurefireReportParser(reportsDirectories, consoleLogger);
        testSuites = parser.parseXMLReportFiles();
        this.showSuccess = showSuccess;
        this.xrefLocation = xrefLocation;
    }

    @Override
    public String getTitle() {
        return getI18nString("title");
    }

    /**
     * @param key The key.
     * @return The translated string.
     */
    private String getI18nString(String key) {
        return getI18nString(getI18nSection(), key);
    }

    private String getI18nSection() {
        return i18nSection;
    }

    /**
     * @param section The section.
     * @param key The key to translate.
     * @return the translated key.
     */
    private String getI18nString(String section, String key) {
        return i18n.getString("surefire-report", locale, "report." + section + '.' + key);
    }

    /**
     * @param section The section.
     * @param key The key to translate.
     * @param args The args to pass to translated string.
     * @return the translated key.
     */
    private String formatI18nString(String section, String key, Object... args) {
        return i18n.format("surefire-report", locale, "report." + section + '.' + key, args);
    }

    public void renderBody() {
        javaScript(javascriptToggleDisplayCode());

        sink.section1();
        sink.sectionTitle1();
        sink.text(getTitle());
        sink.sectionTitle1_();
        sink.section1_();

        renderSectionSummary();

        renderSectionPackages();

        renderSectionTestCases();

        renderSectionFailureDetails();
    }

    private void renderSectionSummary() {
        Map<String, Object> summary = parser.getSummary(testSuites);

        sink.section1();
        sinkAnchor("Summary");
        sink.sectionTitle1();
        sink.text(getI18nString("surefire", "label.summary"));
        sink.sectionTitle1_();

        constructHotLinks();

        sink.lineBreak();

        startTable();

        tableHeader(new String[] {
            getI18nString("surefire", "label.tests"),
            getI18nString("surefire", "label.errors"),
            getI18nString("surefire", "label.failures"),
            getI18nString("surefire", "label.skipped"),
            getI18nString("surefire", "label.successrate"),
            getI18nString("surefire", "label.time")
        });

        tableRow(new String[] {
            String.valueOf(summary.get("totalTests")),
            String.valueOf(summary.get("totalErrors")),
            String.valueOf(summary.get("totalFailures")),
            String.valueOf(summary.get("totalSkipped")),
            formatI18nString("surefire", "value.successrate", summary.get("totalPercentage")),
            formatI18nString("surefire", "value.time", summary.get("totalElapsedTime"))
        });

        endTable();

        sink.lineBreak();

        paragraph(getI18nString("surefire", "text.note1"));

        sink.lineBreak();

        sink.section1_();
    }

    private void renderSectionPackages() {
        Map<String, List<ReportTestSuite>> suitePackages = parser.getSuitesGroupByPackage(testSuites);
        if (suitePackages.isEmpty()) {
            return;
        }

        sink.section1();
        sinkAnchor("Package_List");
        sink.sectionTitle1();
        sink.text(getI18nString("surefire", "label.packagelist"));
        sink.sectionTitle1_();

        constructHotLinks();

        sink.lineBreak();

        startTable();

        tableHeader(new String[] {
            getI18nString("surefire", "label.package"),
            getI18nString("surefire", "label.tests"),
            getI18nString("surefire", "label.errors"),
            getI18nString("surefire", "label.failures"),
            getI18nString("surefire", "label.skipped"),
            getI18nString("surefire", "label.successrate"),
            getI18nString("surefire", "label.time")
        });

        for (Map.Entry<String, List<ReportTestSuite>> entry : suitePackages.entrySet()) {
            String packageName = entry.getKey();

            List<ReportTestSuite> testSuiteList = entry.getValue();

            Map<String, Object> packageSummary = parser.getSummary(testSuiteList);

            tableRow(new String[] {
                createLinkPatternedText(packageName, '#' + packageName),
                String.valueOf(packageSummary.get("totalTests")),
                String.valueOf(packageSummary.get("totalErrors")),
                String.valueOf(packageSummary.get("totalFailures")),
                String.valueOf(packageSummary.get("totalSkipped")),
                formatI18nString("surefire", "value.successrate", packageSummary.get("totalPercentage")),
                formatI18nString("surefire", "value.time", packageSummary.get("totalElapsedTime"))
            });
        }

        endTable();
        sink.lineBreak();

        paragraph(getI18nString("surefire", "text.note2"));

        for (Map.Entry<String, List<ReportTestSuite>> entry : suitePackages.entrySet()) {
            String packageName = entry.getKey();

            List<ReportTestSuite> testSuiteList = entry.getValue();

            sink.section2();
            sinkAnchor(packageName);
            sink.sectionTitle2();
            sink.text(packageName);
            sink.sectionTitle2_();

            boolean showTable = false;

            for (ReportTestSuite suite : testSuiteList) {
                if (showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0) {
                    showTable = true;

                    break;
                }
            }

            if (showTable) {
                startTable();

                tableHeader(new String[] {
                    "",
                    getI18nString("surefire", "label.class"),
                    getI18nString("surefire", "label.tests"),
                    getI18nString("surefire", "label.errors"),
                    getI18nString("surefire", "label.failures"),
                    getI18nString("surefire", "label.skipped"),
                    getI18nString("surefire", "label.successrate"),
                    getI18nString("surefire", "label.time")
                });

                for (ReportTestSuite suite : testSuiteList) {
                    if (showSuccess || suite.getNumberOfErrors() != 0 || suite.getNumberOfFailures() != 0) {
                        renderSectionTestSuite(suite);
                    }
                }

                endTable();
            }

            sink.section2_();
        }

        sink.lineBreak();

        sink.section1_();
    }

    private void renderSectionTestSuite(ReportTestSuite suite) {
        sink.tableRow();

        sink.tableCell();

        sink.link("#" + suite.getPackageName() + '.' + suite.getName());

        if (suite.getNumberOfErrors() > 0) {
            sinkIcon("error");
        } else if (suite.getNumberOfFailures() > 0) {
            sinkIcon("junit.framework");
        } else if (suite.getNumberOfSkipped() > 0) {
            sinkIcon("skipped");
        } else {
            sinkIcon("success");
        }

        sink.link_();

        sink.tableCell_();

        tableCell(createLinkPatternedText(suite.getName(), '#' + suite.getPackageName() + '.' + suite.getName()));

        tableCell(Integer.toString(suite.getNumberOfTests()));

        tableCell(Integer.toString(suite.getNumberOfErrors()));

        tableCell(Integer.toString(suite.getNumberOfFailures()));

        tableCell(Integer.toString(suite.getNumberOfSkipped()));

        float percentage = parser.computePercentage(
                suite.getNumberOfTests(), suite.getNumberOfErrors(),
                suite.getNumberOfFailures(), suite.getNumberOfSkipped());
        tableCell(formatI18nString("surefire", "value.successrate", percentage));

        tableCell(formatI18nString("surefire", "value.time", suite.getTimeElapsed()));

        sink.tableRow_();
    }

    private void renderSectionTestCases() {
        if (testSuites.isEmpty()) {
            return;
        }

        sink.section1();
        sinkAnchor("Test_Cases");
        sink.sectionTitle1();
        sink.text(getI18nString("surefire", "label.testcases"));
        sink.sectionTitle1_();

        constructHotLinks();

        for (ReportTestSuite suite : testSuites) {
            List<ReportTestCase> testCases = suite.getTestCases();

            if (!testCases.isEmpty()) {
                sink.section2();
                sinkAnchor(suite.getPackageName() + '.' + suite.getName());
                sink.sectionTitle2();
                sink.text(suite.getName());
                sink.sectionTitle2_();

                boolean showTable = false;

                for (ReportTestCase testCase : testCases) {
                    if (!testCase.isSuccessful() || showSuccess) {
                        showTable = true;

                        break;
                    }
                }

                if (showTable) {
                    startTable();

                    for (ReportTestCase testCase : testCases) {
                        if (!testCase.isSuccessful() || showSuccess) {
                            constructTestCaseSection(testCase);
                        }
                    }

                    endTable();
                }

                sink.section2_();
            }
        }

        sink.lineBreak();

        sink.section1_();
    }

    private void constructTestCaseSection(ReportTestCase testCase) {
        sink.tableRow();

        sink.tableCell();

        if (testCase.getFailureType() != null) {
            sink.link("#" + toHtmlId(testCase.getFullName()));

            sinkIcon(testCase.getFailureType());

            sink.link_();
        } else {
            sinkIcon("success");
        }

        sink.tableCell_();

        if (!testCase.isSuccessful()) {
            sink.tableCell();
            sinkAnchor("TC_" + toHtmlId(testCase.getFullName()));

            link("#" + toHtmlId(testCase.getFullName()), testCase.getName());

            SinkEventAttributeSet atts = new SinkEventAttributeSet();
            atts.addAttribute(CLASS, "detailToggle");
            atts.addAttribute(STYLE, "display:inline");
            sink.unknown("div", TAG_TYPE_START, atts);

            sinkLink("javascript:toggleDisplay('" + toHtmlId(testCase.getFullName()) + "');");

            atts = new SinkEventAttributeSet();
            atts.addAttribute(STYLE, "display:inline;");
            atts.addAttribute(ID, toHtmlId(testCase.getFullName()) + "-off");
            sink.unknown("span", TAG_TYPE_START, atts);
            sink.text(" + ");
            sink.unknown("span", TAG_TYPE_END, null);

            atts = new SinkEventAttributeSet();
            atts.addAttribute(STYLE, "display:none;");
            atts.addAttribute(ID, toHtmlId(testCase.getFullName()) + "-on");
            sink.unknown("span", TAG_TYPE_START, atts);
            sink.text(" - ");
            sink.unknown("span", TAG_TYPE_END, null);

            sink.text("[ Detail ]");
            sinkLink_();

            sink.unknown("div", TAG_TYPE_END, null);

            sink.tableCell_();
        } else {
            sinkCellAnchor(testCase.getName(), "TC_" + toHtmlId(testCase.getFullName()));
        }

        tableCell(formatI18nString("surefire", "value.time", testCase.getTime()));

        sink.tableRow_();

        if (!testCase.isSuccessful()) {
            String message = testCase.getFailureMessage();
            if (message != null) {
                sink.tableRow();

                tableCell("");

                sink.tableCell();

                // This shall not be subject to #linkPatternedText()
                text(message);

                sink.tableCell_();

                tableCell("");

                sink.tableRow_();
            }

            String detail = testCase.getFailureDetail();
            if (detail != null) {
                SinkEventAttributeSet atts = new SinkEventAttributeSet();
                atts.addAttribute(ID, toHtmlId(testCase.getFullName()) + toHtmlIdFailure(testCase));
                atts.addAttribute(STYLE, "display:none;");
                sink.tableRow(atts);

                tableCell("");

                sink.tableCell();

                verbatimText(detail);

                sink.tableCell_();

                tableCell("");

                sink.tableRow_();
            }
        }
    }

    private String toHtmlId(String id) {
        return DoxiaUtils.isValidId(id) ? id : DoxiaUtils.encodeId(id, true);
    }

    private void renderSectionFailureDetails() {
        List<ReportTestCase> failures = parser.getFailureDetails(testSuites);
        if (failures.isEmpty()) {
            return;
        }

        sink.section1();
        sinkAnchor("Failure_Details");
        sink.sectionTitle1();
        sink.text(getI18nString("surefire", "label.failuredetails"));
        sink.sectionTitle1_();

        constructHotLinks();

        sink.lineBreak();

        startTable();

        for (ReportTestCase testCase : failures) {
            sink.tableRow();

            sink.tableCell();

            String type = testCase.getFailureType();

            sinkIcon(type);

            sink.tableCell_();

            sinkCellAnchor(testCase.getName(), toHtmlId(testCase.getFullName()));

            sink.tableRow_();

            String message = testCase.getFailureMessage();

            sink.tableRow();

            tableCell("");

            sink.tableCell();

            // This shall not be subject to #linkPatternedText()
            text(message == null ? type : type + ": " + message);

            sink.tableCell_();

            sink.tableRow_();

            String detail = testCase.getFailureDetail();
            if (detail != null) {
                sink.tableRow();

                tableCell("");

                sink.tableCell();
                SinkEventAttributeSet atts = new SinkEventAttributeSet();
                atts.addAttribute(ID, testCase.getName() + toHtmlIdFailure(testCase));
                sink.unknown("div", TAG_TYPE_START, atts);

                String fullClassName = testCase.getFullClassName();
                String errorLineNumber = testCase.getFailureErrorLine();
                if (xrefLocation != null) {
                    String path = fullClassName.replace('.', '/');
                    sink.link(xrefLocation + "/" + path + ".html#L" + errorLineNumber);
                }
                sink.text(fullClassName + ":" + errorLineNumber);

                if (xrefLocation != null) {
                    sink.link_();
                }
                sink.unknown("div", TAG_TYPE_END, null);

                sink.tableCell_();

                sink.tableRow_();
            }
        }

        endTable();

        sink.lineBreak();

        sink.section1_();
    }

    private void constructHotLinks() {
        if (!testSuites.isEmpty()) {
            sink.paragraph();

            sink.text("[");
            link("#Summary", getI18nString("surefire", "label.summary"));
            sink.text("]");

            sink.text(" [");
            link("#Package_List", getI18nString("surefire", "label.packagelist"));
            sink.text("]");

            sink.text(" [");
            link("#Test_Cases", getI18nString("surefire", "label.testcases"));
            sink.text("]");

            sink.paragraph_();
        }
    }

    private String toHtmlIdFailure(ReportTestCase testCase) {
        return testCase.hasError() ? "-error" : "-failure";
    }

    private void sinkIcon(String type) {
        if (type.startsWith("junit.framework") || "skipped".equals(type)) {
            sink.figureGraphics("images/icon_warning_sml.gif");
        } else if (type.startsWith("success")) {
            sink.figureGraphics("images/icon_success_sml.gif");
        } else {
            sink.figureGraphics("images/icon_error_sml.gif");
        }
    }

    private void sinkCellAnchor(String text, String anchor) {
        sink.tableCell();
        sinkAnchor(anchor);
        sink.text(text);
        sink.tableCell_();
    }

    private void sinkAnchor(String anchor) {
        // Dollar '$' for nested classes is not valid character in sink.anchor() and therefore it is ignored
        // https://issues.apache.org/jira/browse/SUREFIRE-1443
        sink.unknown(A.toString(), TAG_TYPE_START, new SinkEventAttributeSet(ID, anchor));
        sink.unknown(A.toString(), TAG_TYPE_END, null);
    }

    private void sinkLink(String href) {
        // The "'" argument in this JavaScript function would be escaped to "&apos;"
        // sink.link( "javascript:toggleDisplay('" + toHtmlId( testCase.getFullName() ) + "');" );
        sink.unknown(A.toString(), TAG_TYPE_START, new SinkEventAttributeSet(HREF, href));
    }

    @SuppressWarnings("checkstyle:methodname")
    private void sinkLink_() {
        sink.unknown(A.toString(), TAG_TYPE_END, null);
    }

    private String javascriptToggleDisplayCode() {
        return "function toggleDisplay(elementId) {" + Markup.EOL
                + " var elm = document.getElementById(elementId + '-error');" + Markup.EOL
                + " if (elm == null) {" + Markup.EOL
                + "  elm = document.getElementById(elementId + '-failure');" + Markup.EOL
                + " }" + Markup.EOL
                + " if (elm && typeof elm.style != \"undefined\") {" + Markup.EOL
                + "  if (elm.style.display == \"none\") {" + Markup.EOL
                + "   elm.style.display = \"\";" + Markup.EOL
                + "   document.getElementById(elementId + '-off').style.display = \"none\";" + Markup.EOL
                + "   document.getElementById(elementId + '-on').style.display = \"inline\";" + Markup.EOL
                + "  } else if (elm.style.display == \"\") {"
                + "   elm.style.display = \"none\";" + Markup.EOL
                + "   document.getElementById(elementId + '-off').style.display = \"inline\";" + Markup.EOL
                + "   document.getElementById(elementId + '-on').style.display = \"none\";" + Markup.EOL
                + "  }" + Markup.EOL
                + " }" + Markup.EOL
                + " }";
    }
}
