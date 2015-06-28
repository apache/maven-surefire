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

import org.apache.maven.doxia.module.xhtml.XhtmlSink;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.ResourceBundle;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Prevent fom NPE if failure type and message is null however detail presents.
 */
public class Surefire597Test
{
    @Test
    public void corruptedTestCaseFailureWithMissingErrorTypeAndMessage()
        throws Exception
    {
        File basedir = new File( "." ).getCanonicalFile();
        File report = new File( basedir, "target/test-classes/surefire-597" );
        SurefireReportGenerator gen = new SurefireReportGenerator( singletonList( report ), ENGLISH, true, null );
        ResourceBundle resourceBundle = ResourceBundle.getBundle( "surefire-report", ENGLISH );
        StringWriter writer = new StringWriter();
        gen.doGenerateReport( resourceBundle, new XhtmlSink( writer ) {} );
        String xml = writer.toString();
        assertThat( xml, containsString(
            "<table border=\"1\" class=\"bodyTable\">"
                + "<tr class=\"a\">"
                + "<th>Tests</th><th>Errors </th><th>Failures</th><th>Skipped</th><th>Success Rate</th><th>Time</th>"
                + "</tr>"
                + "<tr class=\"b\">"
                + "<td>1</td><td>1</td><td>0</td><td>0</td><td>0%</td><td>0</td>"
                + "</tr>"
                + "</table>" ) );
        assertThat( xml, containsString(
            "<table border=\"1\" class=\"bodyTable\">"
                + "<tr class=\"a\">"
                + "<th>Package</th><th>Tests</th><th>Errors </th><th>Failures</th><th>Skipped</th>"
                + "<th>Success Rate</th><th>Time</th>"
                + "</tr>"
                + "<tr class=\"b\">"
                + "<td><a href=\"#surefire\">surefire</a></td><td>1</td><td>1</td><td>0</td><td>0</td>"
                + "<td>0%</td><td>0</td>"
                + "</tr>"
                + "</table>" ) );
        assertThat( xml, containsString(
            "<table border=\"1\" class=\"bodyTable\">"
                + "<tr class=\"a\">"
                + "<th></th><th>Class</th><th>Tests</th><th>Errors </th><th>Failures</th><th>Skipped</th>"
                + "<th>Success Rate</th><th>Time</th>"
                + "</tr>"
                + "<tr class=\"b\">"
                + "<td><a href=\"#surefireMyTest\"><img src=\"images/icon_error_sml.gif\" alt=\"\" /></a></td>"
                + "<td><a href=\"#surefireMyTest\">MyTest</a></td>"
                + "<td>1</td><td>1</td><td>0</td><td>0</td><td>0%</td><td>0</td>"
                + "</tr>"
                + "</table>" ) );
        assertThat( xml, containsString(
            "<table border=\"1\" class=\"bodyTable\">"
                + "<tr class=\"a\">"
                + "<td><img src=\"images/icon_error_sml.gif\" alt=\"\" /></td>"
                + "<td><a name=\"surefire.MyTest.test\"></a>test</td></tr>"
                + "<tr class=\"b\">"
                + "<td></td><td>java.lang.RuntimeException: java.lang.IndexOutOfBoundsException: msg</td>"
                + "</tr>"
                + "<tr class=\"a\">"
                + "<td></td><td><div id=\"testerror\">surefire.MyTest:13</div></td>"
                + "</tr>"
                + "</table>" ) );

    }
}
