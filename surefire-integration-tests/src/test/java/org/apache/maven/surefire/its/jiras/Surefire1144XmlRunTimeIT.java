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

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.surefire.its.fixture.HelperAssertions;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test that runtime reported on console matches runtime in XML
 *
 * @author <a href="mailto:eloussi2@illinois.edu">Lamyaa Eloussi</a>
 */
public class Surefire1144XmlRunTimeIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testXmlRunTime()
        throws Exception
    {
        OutputValidator outputValidator = unpack( "/surefire-1144-xml-runtime" ).forkOnce().executeTest();

        List<ReportTestSuite> reports = HelperAssertions.extractReports( new File[]{ outputValidator.getBaseDir() } );
        assertEquals( 1, reports.size() ); //only one report

        ReportTestSuite report = reports.get( 0 );
        float xmlTime = report.getTimeElapsed();
        assertTrue( "surefire1144.Test1 report.getTimeElapsed found:" + xmlTime,
                     xmlTime >= 1.6f ); //include beforeClass and afterClass

        outputValidator.verifyTextInLog( Float.toString( xmlTime ) ); //same time in console
    }

}
