package org.apache.maven.surefire.its.jiras;

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

import org.apache.maven.it.VerificationException;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.shared.utils.xml.Xpp3DomBuilder;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1036">SUREFIRE-1036</a>
 * @since 2.18
 */
public class Surefire1036NonFilterableJUnitRunnerWithCategoriesIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void test()
        throws VerificationException, FileNotFoundException
    {
        OutputValidator validator = unpack().maven().executeTest();
        validator.assertTestSuiteResults( 1, 0, 0, 0 );
        assertFalse( validator.getSurefireReportsXmlFile(
            "TEST-jiras.surefire1036.TestSomethingWithMockitoRunner.xml" ).exists() );
        assertFalse( validator.getSurefireReportsXmlFile( "TEST-jiras.surefire1036.TestSomeUnit.xml" ).exists() );
        TestFile reportFile =
            validator.getSurefireReportsXmlFile( "TEST-jiras.surefire1036.TestSomeIntegration.xml" );
        assertTestCount( reportFile, 1 );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1036-NonFilterableJUnitRunnerWithCategories" );
    }

    private void assertTestCount( TestFile reportFile, int tests )
        throws FileNotFoundException
    {
        assertTrue( reportFile.exists() );
        Xpp3Dom testResult = Xpp3DomBuilder.build( reportFile.getFileInputStream(), "UTF-8" );
        Xpp3Dom[] children = testResult.getChildren( "testcase" );
        assertEquals( tests, children.length );
    }
}