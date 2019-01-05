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
    public void test_noParallel()
        throws Exception
    {
        doTest( "none" );
    }

    @Test
    public void test_parallelBoth()
        throws Exception
    {
        doTest( "both" );
    }

    private void doTest( String parallelMode )
        throws Exception
    {
        OutputValidator validator =
            unpack( "surefire-943-report-content" ).maven()
            .sysProp( "parallel", parallelMode )
            .sysProp( "threadCount", 4 )
            .withFailure().executeTest();

        validator.assertTestSuiteResults( 10, 1, 3, 3 );

        validate( validator, "org.sample.module.My1Test", 1 );
        validate( validator, "org.sample.module.My2Test", 1 );
        validate( validator, "org.sample.module.My3Test", 0 );
        validateSkipped( validator, "org.sample.module.My4Test" );
        validateFailInBeforeClass( validator, "org.sample.module.My5Test" );
    }

    private void validateFailInBeforeClass( OutputValidator validator, String className )
        throws FileNotFoundException
    {
        Xpp3Dom[] children = readTests( validator, className );

        Assert.assertEquals( 1, children.length );

        Xpp3Dom child = children[0];

        Assert.assertEquals( className, child.getAttribute( "classname" ) );
        Assert.assertEquals( "", child.getAttribute( "name" ) );

        Assert.assertEquals( "Expected error tag for failed BeforeClass method for " + className, 1,
                             child.getChildren( "error" ).length );

        Assert.assertTrue( "time for test failure in BeforeClass is expected to be positive",
                           Double.compare( Double.parseDouble( child.getAttribute( "time" ) ), 0.0d ) >= 0 );

        Assert.assertTrue( "time for test failure in BeforeClass is expected to be resonably low",
                           Double.compare( Double.parseDouble( child.getAttribute( "time" ) ), 2.0d ) <= 0 );
    }

    private void validateSkipped( OutputValidator validator, String className )
        throws FileNotFoundException
    {
        Xpp3Dom[] children = readTests( validator, className );

        Assert.assertEquals( 1, children.length );

        Xpp3Dom child = children[0];

        Assert.assertEquals( className, child.getAttribute( "classname" ) );
        Assert.assertEquals( "", child.getAttribute( "name" ) );

        Assert.assertEquals( "Expected skipped tag for ignored method for " + className, 1,
                             child.getChildren( "skipped" ).length );

        Assert.assertTrue( "time for ignored test is expected to be zero",
                           Double.compare( Double.parseDouble( child.getAttribute( "time" ) ), 0.0d ) == 0 );
    }

    private void validate( OutputValidator validator, String className, int ignored )
        throws FileNotFoundException
    {
        Xpp3Dom[] children = readTests( validator, className );

        Assert.assertEquals( 2 + ignored, children.length );

        for ( Xpp3Dom child : children )
        {
            Assert.assertEquals( className, child.getAttribute( "classname" ) );

            if ( "alwaysSuccessful".equals( child.getAttribute( "name" ) ) )
            {
                Assert.assertEquals( "Expected no failures for method alwaysSuccessful for " + className, 0,
                                     child.getChildCount() );

                Assert.assertTrue( "time for successful test is expected to be positive",
                                   Double.compare( Double.parseDouble( child.getAttribute( "time" ) ), 0.0d ) > 0 );
            }
            else if ( child.getAttribute( "name" ).contains( "Ignored" ) )
            {
                Assert.assertEquals( "Expected skipped-tag for ignored method for " + className, 1,
                                     child.getChildren( "skipped" ).length );

                Assert.assertTrue( "time for ignored test is expected to be zero",
                                   Double.compare( Double.parseDouble( child.getAttribute( "time" ) ), 0.0d ) == 0 );

            }
            else
            {
                Assert.assertEquals( "Expected methods \"alwaysSuccessful\", \"*Ignored\" and \"fails\" in "
                    + className, "fails", child.getAttribute( "name" ) );
                Assert.assertEquals( "Expected failure description for method \"fails\" in " + className, 1,
                                     child.getChildren( "failure" ).length );
                Assert.assertTrue( "time for failed test is expected to be positive",
                                   Double.compare( Double.parseDouble( child.getAttribute( "time" ) ), 0.0d ) > 0 );
            }
        }
    }

    private Xpp3Dom[] readTests( OutputValidator validator, String className )
        throws FileNotFoundException
    {
        Xpp3Dom testResult =
            Xpp3DomBuilder.build( validator.getSurefireReportsXmlFile( "TEST-" + className + ".xml" ).getFileInputStream(),
                                  "UTF-8" );
        Xpp3Dom[] children = testResult.getChildren( "testcase" );
        return children;
    }

}
