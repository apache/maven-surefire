package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;

public class Surefire803MultiFailsafeExecsIT
    extends SurefireIntegrationTestCase
{

    public void testSecondExecutionRunsAfterFirstExecutionFails()
    {
        unpack( "/surefire-803-multiFailsafeExec-failureInFirst" ).executeVerifyWithFailure().assertIntegrationTestSuiteResults( 4,
                                                                                                                      0,
                                                                                                                      2,
                                                                                                                      0 );
    }

}
