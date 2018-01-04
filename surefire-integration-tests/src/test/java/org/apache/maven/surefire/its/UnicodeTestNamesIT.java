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
 * under the LicenseUni.
 */

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Verifies unicode filenames pass through correctly.
 */
public class UnicodeTestNamesIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final String TXT_REPORT = "junit.twoTestCases.\u800C\u7D22\u5176\u60C5Test.txt";
    private static final String XML_REPORT = "TEST-junit.twoTestCases.\u800C\u7D22\u5176\u60C5Test.xml";

    @Test
    public void checkFileNamesWithUnicode()
    {
        SurefireLauncher unpacked = unpack( "unicode-testnames" );
        File basedir = unpacked.getUnpackedAt();

        unpacked.execute( "clean" );

        File xxyz = new File( basedir, "src/test/java/junit/twoTestCases/XXYZTest.java" );
        File dest = new File( basedir, "src/test/java/junit/twoTestCases/\u800C\u7D22\u5176\u60C5Test.java" );

        //noinspection ResultOfMethodCallIgnored
        dest.delete();
        assertTrue( xxyz.renameTo( dest ) );

        assertTrue( dest.exists() );
        assumeFalse( new File( basedir, "src/test/java/junit/twoTestCases/????Test.java" ).exists() );

        OutputValidator outputValidator =
                unpacked.executeTest()
                        .assertTestSuiteResults( 2, 0, 0, 0 );

        TestFile surefireReportFile = outputValidator.getSurefireReportsFile( TXT_REPORT );
        assertTrue( surefireReportFile.exists() );
        surefireReportFile.assertContainsText( "junit.twoTestCases.????Test" );

        TestFile surefireXmlReportFile = outputValidator.getSurefireReportsXmlFile( XML_REPORT );
        assertTrue( surefireXmlReportFile.exists() );
        assertFalse( surefireXmlReportFile.readFileToString().isEmpty() );
        surefireXmlReportFile.assertContainsText( "junit.twoTestCases.\u800C\u7D22\u5176\u60C5Test" );
    }

}
