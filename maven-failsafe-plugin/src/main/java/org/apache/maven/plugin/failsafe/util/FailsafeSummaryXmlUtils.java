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
package org.apache.maven.plugin.failsafe.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.apache.maven.surefire.api.suite.RunResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.shared.lang3.StringEscapeUtils.escapeXml10;
import static org.apache.maven.surefire.shared.lang3.StringEscapeUtils.unescapeXml;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isBlank;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class FailsafeSummaryXmlUtils {
    private static final String FAILSAFE_SUMMARY_XML_SCHEMA_LOCATION =
            "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/failsafe-summary.xsd";

    private static final String MESSAGE_NIL_ELEMENT =
            "<failureMessage xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";

    private static final String MESSAGE_ELEMENT = "<failureMessage>%s</failureMessage>";

    private static final String FAILSAFE_SUMMARY_XML_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<failsafe-summary xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:noNamespaceSchemaLocation=\"" + FAILSAFE_SUMMARY_XML_SCHEMA_LOCATION + "\""
            + " result=\"%s\" timeout=\"%s\">\n"
            + "    <completed>%d</completed>\n"
            + "    <errors>%d</errors>\n"
            + "    <failures>%d</failures>\n"
            + "    <skipped>%d</skipped>\n"
            + "    <flakes>%d</flakes>\n"
            + "    %s\n"
            + "</failsafe-summary>";

    private FailsafeSummaryXmlUtils() {
        throw new IllegalStateException("No instantiable constructor.");
    }

    public static RunResult toRunResult(File failsafeSummaryXml) throws IOException {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        try (Reader reader = new InputStreamReader(new FileInputStream(failsafeSummaryXml), UTF_8)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
            setAttribute(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Document document = factory.newDocumentBuilder().parse(new InputSource(reader));
            Node root = document.getDocumentElement();

            String completed = xpath.evaluate("/failsafe-summary/completed", root);
            String errors = xpath.evaluate("/failsafe-summary/errors", root);
            String failures = xpath.evaluate("/failsafe-summary/failures", root);
            String skipped = xpath.evaluate("/failsafe-summary/skipped", root);
            String failureMessage = xpath.evaluate("/failsafe-summary/failureMessage", root);
            String timeout = xpath.evaluate("/failsafe-summary/@timeout", root);
            String flakes = xpath.evaluate("/failsafe-summary/flakes", root);

            return new RunResult(
                    parseInt(completed),
                    parseInt(errors),
                    parseInt(failures),
                    parseInt(skipped),
                    // FIXME Backwards compatibility: to be replaced with parseInt in a future release
                    // synchronize with maven-surefire-plugin/src/site/resources/xsd/failsafe-summary.xsd
                    isBlank(flakes) ? 0 : parseInt(flakes),
                    isBlank(failureMessage) ? null : unescapeXml(failureMessage),
                    parseBoolean(timeout));
        } catch (XPathExpressionException | NumberFormatException e) {
            throw new IOException("Could not parse " + failsafeSummaryXml.getPath(), e);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Could not parse " + failsafeSummaryXml.getPath(), e);
        }
    }

    private static void setFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            // Optional hardening feature unsupported by this parser; secure processing is still enabled.
        }
    }

    private static void setAttribute(DocumentBuilderFactory factory, String attribute, String value) {
        try {
            factory.setAttribute(attribute, value);
        } catch (IllegalArgumentException e) {
            // Optional access restriction unsupported by this parser.
        }
    }

    public static void fromRunResultToFile(RunResult fromRunResult, File toFailsafeSummaryXml) throws IOException {
        String failure = fromRunResult.getFailure();
        String msg = isBlank(failure) ? MESSAGE_NIL_ELEMENT : format(MESSAGE_ELEMENT, escapeXml10(failure));
        String xml = format(
                Locale.ROOT,
                FAILSAFE_SUMMARY_XML_TEMPLATE,
                fromRunResult.getFailsafeCode(),
                fromRunResult.isTimeout(),
                fromRunResult.getCompletedCount(),
                fromRunResult.getErrors(),
                fromRunResult.getFailures(),
                fromRunResult.getSkipped(),
                fromRunResult.getFlakes(),
                msg);

        Files.write(
                toFailsafeSummaryXml.toPath(),
                xml.getBytes(UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    public static void writeSummary(RunResult mergedSummary, File mergedSummaryFile, boolean inProgress)
            throws Exception {
        if (!mergedSummaryFile.getParentFile().isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            mergedSummaryFile.getParentFile().mkdirs();
        }

        if (mergedSummaryFile.exists() && inProgress) {
            RunResult runResult = toRunResult(mergedSummaryFile);
            mergedSummary = mergedSummary.aggregate(runResult);
        }

        fromRunResultToFile(mergedSummary, mergedSummaryFile);
    }
}
