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

import junit.framework.TestCase;
import org.apache.maven.doxia.module.xhtml5.Xhtml5Sink;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;

import java.io.File;
import java.io.StringWriter;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.apache.maven.plugins.surefire.report.Utils.toSystemNewLine;

/**
 * Prevent fom NPE if failure type and message is null however detail presents.
 */
public class Surefire597Test
        extends TestCase
{
    @SuppressWarnings( "checkstyle:linelength" )
    public void testCorruptedTestCaseFailureWithMissingErrorTypeAndMessage()
        throws Exception
    {
        File basedir = new File( "." ).getCanonicalFile();
        File report = new File( basedir, "target/test-classes/surefire-597" );
        ConsoleLogger log = new NullConsoleLogger();
        SurefireReportGenerator gen = new SurefireReportGenerator( singletonList( report ), ENGLISH, true, null, log );
        StringWriter writer = new StringWriter();
        Sink sink = new Xhtml5Sink( writer )
                        {  };
        gen.doGenerateReport( new SurefireReportMojo().getBundle( ENGLISH ), sink );
        String xml = writer.toString();
        assertThat( xml, containsString( toSystemNewLine(
            "<table border=\"1\" class=\"bodyTable\">\n"
                + "<tr class=\"a\">\n"
                + "<th>Tests</th>\n"
                + "<th>Errors</th>\n"
                + "<th>Failures</th>\n"
                + "<th>Skipped</th>\n"
                + "<th>Success Rate</th>\n"
                + "<th>Time</th></tr>\n"
                + "<tr class=\"b\">\n"
                + "<td align=\"left\">1</td>\n"
                + "<td align=\"left\">1</td>\n"
                + "<td align=\"left\">0</td>\n"
                + "<td align=\"left\">0</td>\n"
                + "<td align=\"left\">0%</td>\n"
                + "<td align=\"left\">0</td>"
                + "</tr>"
                + "</table>" ) ) );
        assertThat( xml, containsString( toSystemNewLine(
            "<table border=\"1\" class=\"bodyTable\">\n"
                + "<tr class=\"a\">\n"
                + "<th>Package</th>\n"
                + "<th>Tests</th>\n"
                + "<th>Errors</th>\n"
                + "<th>Failures</th>\n"
                + "<th>Skipped</th>\n"
                + "<th>Success Rate</th>\n"
                + "<th>Time</th></tr>\n"
                + "<tr class=\"b\">\n"
                + "<td align=\"left\"><a href=\"#surefire\">surefire</a></td>\n"
                + "<td align=\"left\">1</td>\n"
                + "<td align=\"left\">1</td>\n"
                + "<td align=\"left\">0</td>\n"
                + "<td align=\"left\">0</td>\n"
                + "<td align=\"left\">0%</td>\n"
                + "<td align=\"left\">0</td></tr></table>" ) ) );
        assertThat( xml, containsString( toSystemNewLine(
            "<table border=\"1\" class=\"bodyTable\">\n"
                + "<tr class=\"a\">\n"
                + "<th></th>\n"
                + "<th>Class</th>\n"
                + "<th>Tests</th>\n"
                + "<th>Errors</th>\n"
                + "<th>Failures</th>\n"
                + "<th>Skipped</th>\n"
                + "<th>Success Rate</th>\n"
                + "<th>Time</th></tr>\n"
                + "<tr class=\"b\">\n"
                + "<td align=\"left\"><a href=\"#surefire.MyTest\"><figure><img src=\"images/icon_error_sml.gif\" alt=\"\" /></figure></a></td>\n"
                + "<td align=\"left\"><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                + "<td align=\"left\">1</td>\n"
                + "<td align=\"left\">1</td>\n"
                + "<td align=\"left\">0</td>\n"
                + "<td align=\"left\">0</td>\n"
                + "<td align=\"left\">0%</td>\n"
                + "<td align=\"left\">0</td></tr></table>" ) ) );
        assertThat( xml, containsString( toSystemNewLine(
            "<table border=\"1\" class=\"bodyTable\">\n"
                + "<tr class=\"a\">\n"
                + "<td align=\"left\"><figure><img src=\"images/icon_error_sml.gif\" alt=\"\" /></figure></td>\n"
                + "<td align=\"left\"><a name=\"surefire.MyTest.test\"></a>test</td></tr>\n"
                + "<tr class=\"b\">\n"
                + "<td align=\"left\"></td>\n"
                + "<td align=\"left\">java.lang.RuntimeException: java.lang.IndexOutOfBoundsException: msg</td></tr>\n"
                + "<tr class=\"a\">\n"
                + "<td align=\"left\"></td>\n"
                + "<td align=\"left\">\n"
                + "<div id=\"test-error\">surefire.MyTest:13</div></td></tr></table>" ) ) );
    }
}
