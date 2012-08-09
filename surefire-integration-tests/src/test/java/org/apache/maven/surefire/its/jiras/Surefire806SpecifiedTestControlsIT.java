package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.Not2xCompatible;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( Not2xCompatible.class)
public class Surefire806SpecifiedTestControlsIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void singleTestInOneExecutionOfMultiExecutionProject()
    {
        unpack( "/surefire-806-specifiedTests-multi" ).setTestToRun( "FirstTest" ).failIfNoSpecifiedTests(
            false ).executeTest().verifyErrorFree( 1 );
    }

    @Test
    public void twoSpecifiedTestExecutionsInCorrectExecutionBlocks()
    {
        unpack( "/surefire-806-specifiedTests-multi" ).setTestToRun(
            "FirstTest,SecondTest" ).executeTest().verifyErrorFree( 2 );
    }

    @Test
    public void singleTestInSingleExecutionProject()
    {
        unpack( "/surefire-806-specifiedTests-single" ).setTestToRun( "ThirdTest" ).failIfNoSpecifiedTests(
            false ).executeTest().verifyErrorFree( 1 );
    }
}
