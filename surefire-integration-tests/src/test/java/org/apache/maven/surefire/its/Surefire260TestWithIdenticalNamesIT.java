package org.apache.maven.surefire.its;
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
import java.net.URI;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Test Surefire-570 Multiple report directories
 *
 * @author Kristian Rosenvold
 */
public class Surefire260TestWithIdenticalNamesIT
    extends SurefireVerifierTestClass
{

    public Surefire260TestWithIdenticalNamesIT()
    {
        super( "/surefire-260-testWithIdenticalNames" );
    }

    public void testWithIdenticalNames()
        throws Exception
    {

        failNever();
        executeTest();
        reset();
        execute("surefire-report:report" );

        File siteFile = getSiteFile( "surefire-report.html" );
        final URI uri = siteFile.toURI();

        final WebClient webClient = new WebClient();
        webClient.setJavaScriptEnabled(true);
        final HtmlPage page = webClient.getPage(uri.toURL());

        final HtmlAnchor a = (HtmlAnchor) page.getByXPath("//a[contains(@href, 'surefire260_TestB_testDup')]").get(0);
        final HtmlDivision content = (HtmlDivision) page.getElementById("surefire260_TestB_testDuperror");
        assertTrue(content.getAttribute("style").contains("none"));
        a.click();
        assertFalse(content.getAttribute("style").contains("none"));
        webClient.closeAllWindows();
    }
}
