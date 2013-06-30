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

import java.io.File;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Verifies unicode filenames pass through correctly.
 * <p/>
 * If the underlying file system turns out not to support unicode, we just fail an assumption.s
 */
public class UnicodeTestNamesIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void checkFileNamesWithUnicode()
    {
        SurefireLauncher unpacked = unpack( "unicode-testnames" );
        File xxyz = new File( unpacked.getUnpackedAt(), "src/test/java/junit/twoTestCases/XXYZTest.java" );

        File dest = new File( unpacked.getUnpackedAt(),
                              "src/test/java/junit/twoTestCases/\u800C\u7D22\u5176\u60C5Test.java" );

        Assume.assumeTrue( xxyz.renameTo( dest ) );
        Assume.assumeTrue( dest.exists() );
        Assume.assumeTrue(
            !new File( unpacked.getUnpackedAt(), "src/test/java/junit/twoTestCases/????Test.java" ).exists() );

        OutputValidator outputValidator = unpacked.executeTest().assertTestSuiteResults( 2, 0, 0, 0 );
        TestFile surefireReportsFile = outputValidator.getSurefireReportsFile( "junit.twoTestCases.而索其情Test.txt" );
        Assert.assertTrue( surefireReportsFile.exists() );
        //surefireReportsFile .assertContainsText( "junit.twoTestCases.\u800C\u7D22\u5176\u60C5Test.txt" );
    }

}
