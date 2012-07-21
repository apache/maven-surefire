package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

public class Surefire847AdditionalFailureIT
    extends SurefireIntegrationTestCase
{
    public void testJUnitRunCategoryAB()
    {
        unpack( "surefire-847-testngfail" ).setTestToRun( "org.codehaus.SomePassedTest")
            .executeTest().verifyErrorFreeLog();
    }
}
