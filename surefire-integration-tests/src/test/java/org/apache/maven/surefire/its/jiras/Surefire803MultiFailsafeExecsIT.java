package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

public class Surefire803MultiFailsafeExecsIT
    extends SurefireIntegrationTestCase
{

    public void testSecondExecutionRunsAfterFirstExecutionFails()
    {
        unpack(
            "/surefire-803-multiFailsafeExec-failureInFirst" ).executeVerifyWithFailure().assertIntegrationTestSuiteResults(
            4, 0, 2, 0 );
    }

    public void testOneExecutionRunInTwoBuilds()
    {
        SurefireLauncher launcher = unpack( "/surefire-803-multiFailsafeExec-rebuildOverwrites" );
        launcher.addD( "success", "false" ).executeVerifyWithFailure().assertIntegrationTestSuiteResults( 1, 0, 1, 0 );
        launcher.reset();
        launcher.addD( "success", "true" ).executeVerify().assertIntegrationTestSuiteResults( 1, 0, 0, 0 );
    }

}
