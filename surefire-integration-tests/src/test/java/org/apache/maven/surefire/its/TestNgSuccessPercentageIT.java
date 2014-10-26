package org.apache.maven.surefire.its;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * Test that TestNG's @Test(successPercentage = n, invocationCount=n) passes so long as successPercentage tests
 * have passed.
 *
 * @author Jon Todd
 */
public class TestNgSuccessPercentageIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testPassesWhenFailuresLessThanSuccessPercentage()
    {
        OutputValidator validator = unpack("/testng-succes-percentage-pass").executeTest();
        validator.assertTestSuiteResults(4, 0, 0, 0);
    }

    @Test
    public void testFailsWhenFailuresMoreThanSuccessPercentage()
    {
        OutputValidator validator = unpack("/testng-succes-percentage-fail").executeTest();
        validator.assertTestSuiteResults(4, 0, 1, 0);
    }
}