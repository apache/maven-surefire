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

/**
 * Test reported runtime
 *
 * @author Kristian Rosenvold
 */
public class XmlReporterRunTimeIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testForkModeAlways()
        throws Exception
    {
        OutputValidator outputValidator = unpack( "/runorder-parallel" ).parallelMethods().executeTest();

        List<ReportTestSuite> reports = HelperAssertions.extractReports( new File[]{ outputValidator.getBaseDir() } );
        for ( ReportTestSuite report : reports )
        {
            if ( "runorder.parallel.Test1".equals( report.getFullClassName() ) )
            {
                assertTrue( "runorder.parallel.Test1 report.getTimeElapsed found:" + report.getTimeElapsed(),
                            report.getTimeElapsed() >= 1.2f );
            }
            else if ( "runorder.parallel.Test2".equals( report.getFullClassName() ) )
            {
                assertTrue( "runorder.parallel.Test2 report.getTimeElapsed found:" + report.getTimeElapsed(),
                            report.getTimeElapsed() >= 0.9f );
            }
            else
            {
                System.out.println( "report = " + report );
            }
        }

    }
}
