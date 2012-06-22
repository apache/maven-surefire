package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

public class Surefire832ProviderSelectionIT
    extends SurefireIntegrationTestCase
{
    public void testJUnitRunCategoryAB()
    {
        OutputValidator validator = unpackJUnit().groups( "junit4.CategoryA AND junit4.CategoryB" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 1" );
        validator.verifyTextInLog( "catB: 1" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 0" );
        validator.verifyTextInLog( "mA: 1" );
        validator.verifyTextInLog( "mB: 1" );
        validator.verifyTextInLog( "mC: 0" );
    }

    private SurefireLauncher unpackJUnit()
    {
        return unpack( "surefire-832-provider-selection" );
    }

}
