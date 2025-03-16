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
package org.apache.maven.plugin.surefire.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.shared.utils.xml.PrettyPrintXMLWriter;
import org.apache.maven.surefire.shared.utils.xml.XMLWriter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType;
import static org.apache.maven.plugin.surefire.report.FileReporterUtils.stripIllegalFilenameChars;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.apache.maven.surefire.shared.utils.StringUtils.isNotBlank;

// CHECKSTYLE_OFF: LineLength
/**
 * XML format reporter writing to <code>TEST-<i>reportName</i>[-<i>suffix</i>].xml</code> file like written and read
 * by Ant's <a href="http://ant.apache.org/manual/Tasks/junit.html"><code>&lt;junit&gt;</code></a> and
 * <a href="http://ant.apache.org/manual/Tasks/junitreport.html"><code>&lt;junitreport&gt;</code></a> tasks,
 * then supported by many tools like CI servers.
 * <br>
 * <pre>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;testsuite name="<i>suite name</i>" [group="<i>group</i>"] tests="<i>0</i>" failures="<i>0</i>" errors="<i>0</i>" skipped="<i>0</i>" time="<i>{float}</i>"&gt;
 *  &lt;properties&gt;
 *    &lt;property name="<i>name</i>" value="<i>value</i>"/&gt;
 *    [...]
 *  &lt;/properties&gt;
 *  &lt;testcase time="<i>{float}</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]"/&gt;
 *  &lt;testcase time="<i>{float}</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]"&gt;
 *    &lt;<b>error</b> message="<i>message</i>" type="<i>exception class name</i>"&gt;<i>stacktrace</i>&lt;/error&gt;
 *    &lt;system-out&gt;<i>system out content (present only if not empty)</i>&lt;/system-out&gt;
 *    &lt;system-err&gt;<i>system err content (present only if not empty)</i>&lt;/system-err&gt;
 *  &lt;/testcase&gt;
 *  &lt;testcase time="<i>{float}</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]"&gt;
 *    &lt;<b>failure</b> message="<i>message</i>" type="<i>exception class name</i>"&gt;<i>stacktrace</i>&lt;/failure&gt;
 *    &lt;system-out&gt;<i>system out content (present only if not empty)</i>&lt;/system-out&gt;
 *    &lt;system-err&gt;<i>system err content (present only if not empty)</i>&lt;/system-err&gt;
 *  &lt;/testcase&gt;
 *  &lt;testcase time="<i>{float}</i>" name="<i>test name</i> [classname="<i>class name</i>"] [group="<i>group</i>"]"&gt;
 *    &lt;<b>skipped</b>/&gt;
 *  &lt;/testcase&gt;
 *  [...]</pre>
 *
 * @author Kristian Rosenvold
 * @see <a href="https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=115528872">Ant's format enhancement proposal</a>
 *      (not yet implemented by Ant 1.8.2)
 * @see <a href="https://github.com/apache/ant/blob/6f68fbdefad521741b37304cfdfc6d8646cf1839/src/main/org/apache/tools/ant/taskdefs/optional/junit/XMLJUnitResultFormatter.java">Ant's <code>XMLJUnitResultFormatter</code></a>
 */
@SuppressWarnings({"javadoc", "checkstyle:javadoctype"})
// TODO this is no more stateless due to existence of testClassMethodRunHistoryMap since of 2.19.
public class StatelessXmlReporter implements StatelessReportEventListener<WrappedReportEntry, TestSetStats> {
    private static final float ONE_SECOND = 1000.0f;

    private static final String XML_INDENT = "  ";

    private static final String XML_NL = "\n";

    private final File reportsDirectory;

    private final String reportNameSuffix;

    private final boolean trimStackTrace;

    private final int rerunFailingTestsCount;

    private final String xsdSchemaLocation;

    private final String xsdVersion;

    // Map between test class name and a map between test method name
    // and the list of runs for each test method
    private final Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistoryMap;

    private final boolean phrasedFileName;

    private final boolean phrasedSuiteName;

    private final boolean phrasedClassName;

    private final boolean phrasedMethodName;

    private final boolean enableOutErrElements;

    private final boolean enablePropertiesElement;

    public StatelessXmlReporter(
            File reportsDirectory,
            String reportNameSuffix,
            boolean trimStackTrace,
            int rerunFailingTestsCount,
            Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistoryMap,
            String xsdSchemaLocation,
            String xsdVersion,
            boolean phrasedFileName,
            boolean phrasedSuiteName,
            boolean phrasedClassName,
            boolean phrasedMethodName,
            boolean enableOutErrElements,
            boolean enablePropertiesElement) {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
        this.trimStackTrace = trimStackTrace;
        this.rerunFailingTestsCount = rerunFailingTestsCount;
        this.testClassMethodRunHistoryMap = testClassMethodRunHistoryMap;
        this.xsdSchemaLocation = xsdSchemaLocation;
        this.xsdVersion = xsdVersion;
        this.phrasedFileName = phrasedFileName;
        this.phrasedSuiteName = phrasedSuiteName;
        this.phrasedClassName = phrasedClassName;
        this.phrasedMethodName = phrasedMethodName;
        this.enableOutErrElements = enableOutErrElements;
        this.enablePropertiesElement = enablePropertiesElement;
    }

    @Override
    public void testSetCompleted(WrappedReportEntry testSetReportEntry, TestSetStats testSetStats) {
        Map<String, Map<String, List<WrappedReportEntry>>> classMethodStatistics =
                arrangeMethodStatistics(testSetReportEntry, testSetStats);

        // The Java Language Spec:
        // "Note that the close methods of resources are called in the opposite order of their creation."
        try (OutputStream outputStream = getOutputStream(testSetReportEntry);
                OutputStreamWriter fw = getWriter(outputStream)) {
            XMLWriter ppw = new PrettyPrintXMLWriter(new PrintWriter(fw), XML_INDENT, XML_NL, UTF_8.name(), null);

            createTestSuiteElement(ppw, testSetReportEntry, testSetStats); // TestSuite

            if (enablePropertiesElement) {
                showProperties(ppw, testSetReportEntry.getSystemProperties());
            } else {
                boolean hasNonSuccess = false;
                for (Map<String, List<WrappedReportEntry>> statistics : classMethodStatistics.values()) {
                    for (List<WrappedReportEntry> thisMethodRuns : statistics.values()) {
                        if (thisMethodRuns.stream()
                                .anyMatch(entry -> entry.getReportEntryType() != ReportEntryType.SUCCESS)) {
                            hasNonSuccess = true;
                            break;
                        }
                    }
                    if (hasNonSuccess) {
                        break;
                    }
                }

                if (hasNonSuccess) {
                    showProperties(ppw, testSetReportEntry.getSystemProperties());
                }
            }

            for (Entry<String, Map<String, List<WrappedReportEntry>>> statistics : classMethodStatistics.entrySet()) {
                for (Entry<String, List<WrappedReportEntry>> thisMethodRuns :
                        statistics.getValue().entrySet()) {
                    serializeTestClass(outputStream, fw, ppw, thisMethodRuns.getValue());
                }
            }

            ppw.endElement(); // TestSuite
        } catch (IOException e) {
            // It's not a test error.
            // This method must be sail-safe and errors are in a dump log.
            // The control flow must not be broken in TestSetRunListener#testSetCompleted.
            InPluginProcessDumpSingleton.getSingleton().dumpException(e, e.getLocalizedMessage(), reportsDirectory);
        }
    }

    private Map<String, Map<String, List<WrappedReportEntry>>> arrangeMethodStatistics(
            WrappedReportEntry testSetReportEntry, TestSetStats testSetStats) {
        Map<String, Map<String, List<WrappedReportEntry>>> classMethodStatistics = new LinkedHashMap<>();
        for (WrappedReportEntry methodEntry : aggregateCacheFromMultipleReruns(testSetReportEntry, testSetStats)) {
            String testClassName = methodEntry.getSourceName();
            Map<String, List<WrappedReportEntry>> stats = classMethodStatistics.get(testClassName);
            if (stats == null) {
                stats = new LinkedHashMap<>();
                classMethodStatistics.put(testClassName, stats);
            }
            String methodName = methodEntry.getName();
            List<WrappedReportEntry> methodRuns = stats.get(methodName);
            if (methodRuns == null) {
                methodRuns = new ArrayList<>();
                stats.put(methodName, methodRuns);
            }
            methodRuns.add(methodEntry);
        }
        return classMethodStatistics;
    }

    private Deque<WrappedReportEntry> aggregateCacheFromMultipleReruns(
            WrappedReportEntry testSetReportEntry, TestSetStats testSetStats) {
        String suiteClassName = testSetReportEntry.getSourceName();
        Deque<WrappedReportEntry> methodRunHistory = getAddMethodRunHistoryMap(suiteClassName);
        methodRunHistory.addAll(testSetStats.getReportEntries());
        return methodRunHistory;
    }

    private void serializeTestClass(
            OutputStream outputStream, OutputStreamWriter fw, XMLWriter ppw, List<WrappedReportEntry> methodEntries)
            throws IOException {
        if (rerunFailingTestsCount > 0) {
            serializeTestClassWithRerun(outputStream, fw, ppw, methodEntries);
        } else {
            // rerunFailingTestsCount is smaller than 1, but for some reasons a test could be run
            // for more than once
            serializeTestClassWithoutRerun(outputStream, fw, ppw, methodEntries);
        }
    }

    private void serializeTestClassWithoutRerun(
            OutputStream outputStream, OutputStreamWriter fw, XMLWriter ppw, List<WrappedReportEntry> methodEntries)
            throws IOException {
        for (WrappedReportEntry methodEntry : methodEntries) {
            startTestElement(ppw, methodEntry);
            if (methodEntry.getReportEntryType() != SUCCESS) {
                getTestProblems(
                        fw,
                        ppw,
                        methodEntry,
                        trimStackTrace,
                        outputStream,
                        methodEntry.getReportEntryType().getXmlTag(),
                        false);
            }
            if (methodEntry.getReportEntryType() != SUCCESS || enableOutErrElements) {
                createOutErrElements(fw, ppw, methodEntry, outputStream);
            }
            ppw.endElement();
        }
    }

    private void serializeTestClassWithRerun(
            OutputStream outputStream, OutputStreamWriter fw, XMLWriter ppw, List<WrappedReportEntry> methodEntries)
            throws IOException {
        WrappedReportEntry firstMethodEntry = methodEntries.get(0);
        switch (getTestResultType(methodEntries)) {
            case SUCCESS:
                for (WrappedReportEntry methodEntry : methodEntries) {
                    if (methodEntry.getReportEntryType() == SUCCESS) {
                        startTestElement(ppw, methodEntry);
                        ppw.endElement();
                    }
                }
                break;
            case ERROR:
            case FAILURE:
                // When rerunFailingTestsCount is set to larger than 0
                startTestElement(ppw, firstMethodEntry);
                boolean firstRun = true;
                for (WrappedReportEntry singleRunEntry : methodEntries) {
                    if (firstRun) {
                        firstRun = false;
                        getTestProblems(
                                fw,
                                ppw,
                                singleRunEntry,
                                trimStackTrace,
                                outputStream,
                                singleRunEntry.getReportEntryType().getXmlTag(),
                                false);
                        createOutErrElements(fw, ppw, singleRunEntry, outputStream);
                    } else if (singleRunEntry.getReportEntryType() == SKIPPED) {
                        // The version 3.1.0 should produce a new XSD schema with version 3.1.0, see SUREFIRE-1986,
                        // and the XSD schema should add a new element "rerunSkipped"
                        // then ReportEntryType should update the enum to SKIPPED( "skipped", "", "rerunSkipped" ).
                        // The teams should be notified - Jenkins reports.
                        addCommentElementTestCase("a skipped test execution in re-run phase", fw, ppw, outputStream);
                    } else {
                        getTestProblems(
                                fw,
                                ppw,
                                singleRunEntry,
                                trimStackTrace,
                                outputStream,
                                singleRunEntry.getReportEntryType().getRerunXmlTag(),
                                true);
                    }
                }
                ppw.endElement();
                break;
            case FLAKE:
                WrappedReportEntry successful = null;
                // Get the run time of the first successful run
                for (WrappedReportEntry singleRunEntry : methodEntries) {
                    if (singleRunEntry.getReportEntryType() == SUCCESS) {
                        successful = singleRunEntry;
                        break;
                    }
                }
                WrappedReportEntry firstOrSuccessful = successful == null ? methodEntries.get(0) : successful;
                startTestElement(ppw, firstOrSuccessful);
                for (WrappedReportEntry singleRunEntry : methodEntries) {
                    if (singleRunEntry.getReportEntryType() != SUCCESS) {
                        getTestProblems(
                                fw,
                                ppw,
                                singleRunEntry,
                                trimStackTrace,
                                outputStream,
                                singleRunEntry.getReportEntryType().getFlakyXmlTag(),
                                true);
                    }
                }
                ppw.endElement();
                break;
            case SKIPPED:
                startTestElement(ppw, firstMethodEntry);
                getTestProblems(
                        fw,
                        ppw,
                        firstMethodEntry,
                        trimStackTrace,
                        outputStream,
                        firstMethodEntry.getReportEntryType().getXmlTag(),
                        false);
                ppw.endElement();
                break;
            default:
                throw new IllegalStateException("Get unknown test result type");
        }
    }

    /**
     * Clean testClassMethodRunHistoryMap
     */
    public void cleanTestHistoryMap() {
        testClassMethodRunHistoryMap.clear();
    }

    /**
     * Get the result of a test from a list of its runs in WrappedReportEntry
     *
     * @param methodEntryList the list of runs for a given test
     * @return the TestResultType for the given test
     */
    private TestResultType getTestResultType(List<WrappedReportEntry> methodEntryList) {
        List<ReportEntryType> testResultTypeList = new ArrayList<>();
        for (WrappedReportEntry singleRunEntry : methodEntryList) {
            testResultTypeList.add(singleRunEntry.getReportEntryType());
        }

        return DefaultReporterFactory.getTestResultType(testResultTypeList, rerunFailingTestsCount);
    }

    private Deque<WrappedReportEntry> getAddMethodRunHistoryMap(String testClassName) {
        Deque<WrappedReportEntry> methodRunHistory = testClassMethodRunHistoryMap.get(testClassName);
        if (methodRunHistory == null) {
            methodRunHistory = new ConcurrentLinkedDeque<>();
            testClassMethodRunHistoryMap.put(testClassName == null ? "null" : testClassName, methodRunHistory);
        }
        return methodRunHistory;
    }

    private OutputStream getOutputStream(WrappedReportEntry testSetReportEntry) throws IOException {
        File reportFile = getReportFile(testSetReportEntry);
        File reportDir = reportFile.getParentFile();
        //noinspection ResultOfMethodCallIgnored
        reportFile.delete();
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        return new BufferedOutputStream(Files.newOutputStream(reportFile.toPath()), 64 * 1024);
    }

    private static OutputStreamWriter getWriter(OutputStream fos) {
        return new OutputStreamWriter(fos, UTF_8);
    }

    private File getReportFile(WrappedReportEntry report) {
        String reportName = "TEST-" + (phrasedFileName ? report.getReportSourceName() : report.getSourceName());
        String customizedReportName = isBlank(reportNameSuffix) ? reportName : reportName + "-" + reportNameSuffix;
        return new File(reportsDirectory, stripIllegalFilenameChars(customizedReportName + ".xml"));
    }

    private void startTestElement(XMLWriter ppw, WrappedReportEntry report) throws IOException {
        ppw.startElement("testcase");
        String name = phrasedMethodName ? report.getReportName() : report.getName();
        ppw.addAttribute("name", name == null ? "" : extraEscapeAttribute(name));

        if (report.getGroup() != null) {
            ppw.addAttribute("group", report.getGroup());
        }

        String className = phrasedClassName
                ? report.getReportSourceName(reportNameSuffix)
                : report.getSourceName(reportNameSuffix);
        if (className != null) {
            ppw.addAttribute("classname", extraEscapeAttribute(className));
        }

        if (report.getElapsed() != null) {
            ppw.addAttribute("time", String.valueOf(report.getElapsed() / ONE_SECOND));
        }
    }

    private void createTestSuiteElement(XMLWriter ppw, WrappedReportEntry report, TestSetStats testSetStats)
            throws IOException {
        ppw.startElement("testsuite");

        ppw.addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        ppw.addAttribute("xsi:noNamespaceSchemaLocation", xsdSchemaLocation);
        ppw.addAttribute("version", xsdVersion);

        String reportName = phrasedSuiteName
                ? report.getReportSourceName(reportNameSuffix)
                : report.getSourceName(reportNameSuffix);
        ppw.addAttribute("name", reportName == null ? "" : extraEscapeAttribute(reportName));

        if (report.getGroup() != null) {
            ppw.addAttribute("group", report.getGroup());
        }

        if (report.getElapsed() != null) {
            ppw.addAttribute("time", String.valueOf(report.getElapsed() / ONE_SECOND));
        }

        ppw.addAttribute("tests", String.valueOf(testSetStats.getCompletedCount()));

        ppw.addAttribute("errors", String.valueOf(testSetStats.getErrors()));

        ppw.addAttribute("skipped", String.valueOf(testSetStats.getSkipped()));

        ppw.addAttribute("failures", String.valueOf(testSetStats.getFailures()));
    }

    private static void getTestProblems(
            OutputStreamWriter outputStreamWriter,
            XMLWriter ppw,
            WrappedReportEntry report,
            boolean trimStackTrace,
            OutputStream fw,
            String testErrorType,
            boolean createNestedOutErrElements)
            throws IOException {
        ppw.startElement(testErrorType);

        String stackTrace = report.getStackTrace(trimStackTrace);

        if (report.getMessage() != null && !report.getMessage().isEmpty()) {
            ppw.addAttribute("message", extraEscapeAttribute(report.getMessage()));
        }

        if (report.getStackTraceWriter() != null) {
            //noinspection ThrowableResultOfMethodCallIgnored
            SafeThrowable t = report.getStackTraceWriter().getThrowable();
            if (t != null) {
                if (t.getMessage() != null) {
                    int delimiter = stackTrace.indexOf(":");
                    String type = delimiter == -1 ? stackTrace : stackTrace.substring(0, delimiter);
                    ppw.addAttribute("type", type);
                } else {
                    if (isNotBlank(stackTrace)) {
                        ppw.addAttribute("type", new StringTokenizer(stackTrace).nextToken());
                    }
                }
            }
        }

        /* This structure is inconsistent due to bad legacy design choices for the XML schema.
         * Ideally, all elements would be complex and strackTrace would have its own element.
         * See SUREFIRE-2230 for details to how improve and unify the schema in the future.
         */
        if (createNestedOutErrElements) {
            ppw.startElement("stackTrace");
            if (stackTrace != null) {
                extraEscapeElementValue(stackTrace, outputStreamWriter, ppw, fw);
            }
            ppw.endElement();

            createOutErrElements(outputStreamWriter, ppw, report, fw);
        } else if (stackTrace != null) {
            extraEscapeElementValue(stackTrace, outputStreamWriter, ppw, fw);
        }

        ppw.endElement(); // entry type
    }

    // Create system-out and system-err elements
    private static void createOutErrElements(
            OutputStreamWriter outputStreamWriter, XMLWriter ppw, WrappedReportEntry report, OutputStream fw)
            throws IOException {
        EncodingOutputStream eos = new EncodingOutputStream(fw);
        addOutputStreamElement(outputStreamWriter, eos, ppw, report.getStdout(), "system-out");
        addOutputStreamElement(outputStreamWriter, eos, ppw, report.getStdErr(), "system-err");
    }

    private static void addOutputStreamElement(
            OutputStreamWriter outputStreamWriter,
            EncodingOutputStream eos,
            XMLWriter xmlWriter,
            Utf8RecodingDeferredFileOutputStream utf8RecodingDeferredFileOutputStream,
            String name)
            throws IOException {
        if (utf8RecodingDeferredFileOutputStream != null && utf8RecodingDeferredFileOutputStream.getByteCount() > 0) {
            xmlWriter.startElement(name);
            xmlWriter.writeText(""); // Cheat sax to emit element
            outputStreamWriter.flush();
            eos.getUnderlying().write(ByteConstantsHolder.CDATA_START_BYTES); // emit cdata
            utf8RecodingDeferredFileOutputStream.writeTo(eos);
            utf8RecodingDeferredFileOutputStream.free();
            eos.getUnderlying().write(ByteConstantsHolder.CDATA_END_BYTES);
            eos.flush();
            xmlWriter.endElement();
        }
    }

    /**
     * Adds system properties to the XML report.
     * <br>
     *
     * @param xmlWriter The test suite to report to
     */
    private static void showProperties(XMLWriter xmlWriter, Map<String, String> systemProperties) throws IOException {
        xmlWriter.startElement("properties");
        for (final Entry<String, String> entry : systemProperties.entrySet()) {
            final String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                value = "null";
            }

            xmlWriter.startElement("property");

            xmlWriter.addAttribute("name", key);

            xmlWriter.addAttribute("value", extraEscapeAttribute(value));

            xmlWriter.endElement();
        }
        xmlWriter.endElement();
    }

    /**
     * Handle stuff that may pop up in java that is not legal in xml.
     *
     * @param message   The string
     * @return The escaped string or returns itself if all characters are legal
     */
    private static String extraEscapeAttribute(String message) {
        // Someday convert to xml 1.1 which handles everything but 0 inside string
        return containsEscapesIllegalXml10(message) ? escapeXml(message, true) : message;
    }

    /**
     * Writes escaped string or the message within CDATA if all characters are legal.
     *
     * @param message   The string
     */
    private static void extraEscapeElementValue(
            String message, OutputStreamWriter outputStreamWriter, XMLWriter xmlWriter, OutputStream fw)
            throws IOException {
        // Someday convert to xml 1.1 which handles everything but 0 inside string
        if (containsEscapesIllegalXml10(message)) {
            xmlWriter.writeText(escapeXml(message, false));
        } else {
            EncodingOutputStream eos = new EncodingOutputStream(fw);
            xmlWriter.writeText(""); // Cheat sax to emit element
            outputStreamWriter.flush();
            eos.getUnderlying().write(ByteConstantsHolder.CDATA_START_BYTES);
            eos.write(message.getBytes(UTF_8));
            eos.getUnderlying().write(ByteConstantsHolder.CDATA_END_BYTES);
            eos.flush();
        }
    }

    // todo: SUREFIRE-1986
    private static void addCommentElementTestCase(
            String comment, OutputStreamWriter outputStreamWriter, XMLWriter xmlWriter, OutputStream fw)
            throws IOException {
        xmlWriter.writeText(""); // Cheat sax to emit element
        outputStreamWriter.flush();
        fw.write(XML_NL.getBytes(UTF_8));
        fw.write(XML_INDENT.getBytes(UTF_8));
        fw.write(XML_INDENT.getBytes(UTF_8));
        fw.write(ByteConstantsHolder.COMMENT_START);
        fw.write(comment.getBytes(UTF_8));
        fw.write(ByteConstantsHolder.COMMENT_END);
        fw.write(XML_NL.getBytes(UTF_8));
        fw.write(XML_INDENT.getBytes(UTF_8));
        fw.flush();
    }

    private static final class EncodingOutputStream extends FilterOutputStream {
        private int c1;

        private int c2;

        EncodingOutputStream(OutputStream out) {
            super(out);
        }

        OutputStream getUnderlying() {
            return out;
        }

        private boolean isCdataEndBlock(int c) {
            return c1 == ']' && c2 == ']' && c == '>';
        }

        @Override
        public void write(int b) throws IOException {
            if (isCdataEndBlock(b)) {
                out.write(ByteConstantsHolder.CDATA_ESCAPE_STRING_BYTES);
            } else if (isIllegalEscape(b)) {
                // uh-oh!  This character is illegal in XML 1.0!
                // http://www.w3.org/TR/1998/REC-xml-19980210#charsets
                // we're going to deliberately doubly-XML escape it...
                // there's nothing better we can do! :-(
                // SUREFIRE-456
                out.write(ByteConstantsHolder.AMP_BYTES);
                out.write(String.valueOf(b).getBytes(UTF_8));
                out.write(';'); // & Will be encoded to amp inside xml encodingSHO
            } else {
                out.write(b);
            }
            c1 = c2;
            c2 = b;
        }
    }

    private static boolean containsEscapesIllegalXml10(String message) {
        int size = message.length();
        for (int i = 0; i < size; i++) {
            if (isIllegalEscape(message.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIllegalEscape(char c) {
        return isIllegalEscape((int) c);
    }

    private static boolean isIllegalEscape(int c) {
        return c >= 0 && c < 32 && c != '\n' && c != '\r' && c != '\t';
    }

    /**
     * escape for XML 1.0
     *
     * @param text      The string
     * @param attribute true if the escaped value is inside an attribute
     * @return The escaped string
     */
    private static String escapeXml(String text, boolean attribute) {
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isIllegalEscape(c)) {
                // uh-oh!  This character is illegal in XML 1.0!
                // http://www.w3.org/TR/1998/REC-xml-19980210#charsets
                // we're going to deliberately doubly-XML escape it...
                // there's nothing better we can do! :-(
                // SUREFIRE-456
                sb.append(attribute ? "&#" : "&amp#")
                        .append((int) c)
                        .append(';'); // & Will be encoded to amp inside xml encodingSHO
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class ByteConstantsHolder {
        private static final byte[] CDATA_START_BYTES = "<![CDATA[".getBytes(UTF_8);

        private static final byte[] CDATA_END_BYTES = "]]>".getBytes(UTF_8);

        private static final byte[] CDATA_ESCAPE_STRING_BYTES = "]]><![CDATA[>".getBytes(UTF_8);

        private static final byte[] AMP_BYTES = "&amp#".getBytes(UTF_8);

        private static final byte[] COMMENT_START = "<!-- ".getBytes(UTF_8);

        private static final byte[] COMMENT_END = " --> ".getBytes(UTF_8);
    }
}
