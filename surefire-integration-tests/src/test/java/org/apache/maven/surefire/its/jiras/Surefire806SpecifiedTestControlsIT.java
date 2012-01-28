package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;

public class Surefire806SpecifiedTestControlsIT
    extends SurefireIntegrationTestCase
{

    public void testSingleTestInOneExecutionOfMultiExecutionProject()
    {
        unpack( "/surefire-806-specifiedTests-multi" ).setTestToRun( "FirstTest" ).failIfNoSpecifiedTests(
            false ).executeTest().verifyErrorFree( 1 );
    }

    public void testTwoSpecifiedTestExecutionsInCorrectExecutionBlocks()
    {
        unpack( "/surefire-806-specifiedTests-multi" ).setTestToRun(
            "FirstTest,SecondTest" ).executeTest().verifyErrorFree( 2 );
    }

    public void testSingleTestInSingleExecutionProject()
    {
        unpack( "/surefire-806-specifiedTests-single" ).setTestToRun( "ThirdTest" ).failIfNoSpecifiedTests(
            false ).executeTest().verifyErrorFree( 1 );
    }

}
