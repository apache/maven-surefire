package org.apache.maven.plugin.failsafe.util;

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

import org.apache.commons.io.IOUtils;
import org.apache.maven.surefire.suite.RunResult;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeXml;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class FailsafeSummaryXmlUtils
{
    private static final String FAILSAFE_SUMMARY_XML_SCHEMA_LOCATION =
            "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/failsafe-summary.xsd";

    private static final String MESSAGE_NIL_ELEMENT =
            "<failureMessage xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";

    private static final String MESSAGE_ELEMENT =
            "<failureMessage>%s</failureMessage>";

    private static final String FAILSAFE_SUMMARY_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<failsafe-summary xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:noNamespaceSchemaLocation=\"" + FAILSAFE_SUMMARY_XML_SCHEMA_LOCATION + "\""
                    + " result=\"%s\" timeout=\"%s\">\n"
                    + "    <completed>%d</completed>\n"
                    + "    <errors>%d</errors>\n"
                    + "    <failures>%d</failures>\n"
                    + "    <skipped>%d</skipped>\n"
                    + "    %s\n"
                    + "</failsafe-summary>";

    private FailsafeSummaryXmlUtils()
    {
        throw new IllegalStateException( "No instantiable constructor." );
    }

    public static RunResult toRunResult( File failsafeSummaryXml ) throws Exception
    {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        try ( FileInputStream is = new FileInputStream( failsafeSummaryXml ) )
        {
            Node root = ( Node ) xpath.evaluate( "/", new InputSource( is ), XPathConstants.NODE );

            String completed = xpath.evaluate( "/failsafe-summary/completed", root );
            String errors = xpath.evaluate( "/failsafe-summary/errors", root );
            String failures = xpath.evaluate( "/failsafe-summary/failures", root );
            String skipped = xpath.evaluate( "/failsafe-summary/skipped", root );
            String failureMessage = xpath.evaluate( "/failsafe-summary/failureMessage", root );
            String timeout = xpath.evaluate( "/failsafe-summary/@timeout", root );

            return new RunResult( parseInt( completed ), parseInt( errors ), parseInt( failures ), parseInt( skipped ),
                    isBlank( failureMessage ) ? null : unescapeXml( failureMessage ),
                    parseBoolean( timeout )
            );
        }
    }

    public static void fromRunResultToFile( RunResult fromRunResult, File toFailsafeSummaryXml )
            throws IOException
    {
        String failure = fromRunResult.getFailure();
        String msg = isBlank( failure ) ? MESSAGE_NIL_ELEMENT : format( MESSAGE_ELEMENT, escapeXml10( failure ) );
        String xml = format( Locale.ROOT, FAILSAFE_SUMMARY_XML_TEMPLATE,
                fromRunResult.getFailsafeCode(),
                String.valueOf( fromRunResult.isTimeout() ),
                fromRunResult.getCompletedCount(),
                fromRunResult.getErrors(),
                fromRunResult.getFailures(),
                fromRunResult.getSkipped(),
                msg );

        try ( FileOutputStream os = new FileOutputStream( toFailsafeSummaryXml ) )
        {
            IOUtils.write( xml, os, UTF_8 );
        }
    }

    public static void writeSummary( RunResult mergedSummary, File mergedSummaryFile, boolean inProgress )
            throws Exception
    {
        if ( !mergedSummaryFile.getParentFile().isDirectory() )
        {
            //noinspection ResultOfMethodCallIgnored
            mergedSummaryFile.getParentFile().mkdirs();
        }

        if ( mergedSummaryFile.exists() && inProgress )
        {
            RunResult runResult = toRunResult( mergedSummaryFile );
            mergedSummary = mergedSummary.aggregate( runResult );
        }

        fromRunResultToFile( mergedSummary, mergedSummaryFile );
    }
}
