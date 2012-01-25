package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

public class Surefire809GroupExpressionsIT
    extends SurefireIntegrationTestCase
{
    public void testJUnitRunCategoryAB()
    {
        OutputValidator validator = unpackJUnit().groups( "junit4.CategoryA&&junit4.CategoryB" ).executeTest();
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

    public void testJUnitRunCategoryNotC()
    {
        OutputValidator validator = unpackJUnit().groups( "!junit4.CategoryC" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 5, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 2" );
        validator.verifyTextInLog( "catB: 2" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 1" );
        validator.verifyTextInLog( "NoCategoryTest.CatNone: 1" );
    }

    public void testTestNGRunCategoryAB()
    {
        OutputValidator validator = unpackTestNG().groups( "CategoryA&&CategoryB" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyTextInLog( "BasicTest.testInCategoriesAB()" );
        validator.verifyTextInLog( "CategoryCTest.testInCategoriesAB()" );
    }

    public void testTestNGRunCategoryNotC()
    {
        OutputValidator validator = unpackTestNG().groups( "!CategoryC" ).executeTest();
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
        return unpack( "surefire-809-groupExpr-junit48" );
    }

    private SurefireLauncher unpackTestNG()
    {
        return unpack( "surefire-809-groupExpr-testng" );
    }

}
