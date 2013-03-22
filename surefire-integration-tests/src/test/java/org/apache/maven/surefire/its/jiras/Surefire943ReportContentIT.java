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


import java.io.FileNotFoundException;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.shared.utils.xml.Xpp3DomBuilder;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;

import org.junit.Assert;
import org.junit.Test;

public class Surefire943ReportContentIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void test()
        throws Exception
    {
        OutputValidator validator = unpack( "surefire-943-report-content" ).maven().withFailure().executeTest();
        validator.assertTestSuiteResults( 6, 0, 3, 0 );

        validate( validator, "org.sample.module.My1Test" );
        validate( validator, "org.sample.module.My2Test" );
        validate( validator, "org.sample.module.My3Test" );
    }

    private void validate( OutputValidator validator, String className )
        throws FileNotFoundException
    {
        Xpp3Dom testResult =
            Xpp3DomBuilder.build( validator.getSurefireReportsXmlFile( "TEST-" + className + ".xml" ).getFileInputStream(),
                                  "UTF-8" );
        Xpp3Dom[] children = testResult.getChildren( "testcase" );

        Assert.assertEquals( 2, children.length );

        for ( Xpp3Dom child : children )
        {
            Assert.assertEquals( className, child.getAttribute( "classname" ) );

            if ( "alwaysSuccessful".equals( child.getAttribute( "name" ) ) )
            {
                Assert.assertEquals( "Expected no failures for method alwaysSuccessful for " + className, 0,
                                     child.getChildCount() );
            }
            else
            {
                Assert.assertEquals( "Expected methods \"alwaysSuccessful\" and \"fails\" in " + className, "fails",
                                     child.getAttribute( "name" ) );
                Assert.assertEquals( "Expected failure description for method \"fails\" in " + className, 1,
                                     child.getChildren( "failure" ).length );
            }
        }
    }

}
