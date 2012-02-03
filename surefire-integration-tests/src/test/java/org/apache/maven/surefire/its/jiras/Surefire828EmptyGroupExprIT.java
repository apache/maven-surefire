package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

public class Surefire828EmptyGroupExprIT
    extends SurefireIntegrationTestCase
{
    // !CategoryC
    public void testJUnitRunEmptyGroups()
    {
        OutputValidator validator = unpackJUnit().addD( "profile", "emptyGroups" ).executeTest();
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

    // CategoryA && CategoryB
    public void testJUnitRunEmptyExcludeGroups()
    {
        OutputValidator validator = unpackJUnit().addD( "profile", "emptyExcludedGroups" ).executeTest();
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

    // CategoryA && CategoryB
    public void testTestNGRunEmptyExcludeGroups()
    {
        OutputValidator validator = unpackTestNG().addD( "profile", "emptyExcludedGroups" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyTextInLog( "BasicTest.testInCategoriesAB()" );
        validator.verifyTextInLog( "CategoryCTest.testInCategoriesAB()" );
    }

    // !CategoryC
    public void testTestNGRunEmptyGroups()
    {
        OutputValidator validator = unpackTestNG().addD( "profile", "emptyGroups" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 8, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 2" );
        validator.verifyTextInLog( "catB: 2" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 1" );
        validator.verifyTextInLog( "mA: 2" );
        validator.verifyTextInLog( "mB: 2" );
        validator.verifyTextInLog( "mC: 0" );
        validator.verifyTextInLog( "NoCategoryTest.CatNone: 1" );
    }

    private SurefireLauncher unpackJUnit()
    {
        return unpack( "surefire-828-emptyGroupExpr-junit48" );
    }

    private SurefireLauncher unpackTestNG()
    {
        return unpack( "surefire-828-emptyGroupExpr-testng" );
    }

}
