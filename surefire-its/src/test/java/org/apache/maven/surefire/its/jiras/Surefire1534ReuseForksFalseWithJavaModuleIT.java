package org.apache.maven.surefire.its.jiras;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;

public class Surefire1534ReuseForksFalseWithJavaModuleIT
        extends SurefireJUnit4IntegrationTestCase
{

    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    @Test
    public void testExecuteWithReuseForksFalseWithJavaModule()
            throws Exception
    {
        OutputValidator validator = unpack()
                .reuseForks( false )
                .forkCount( 1 )
                .executeTest();

        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyErrorFreeLog();

        TestFile report = validator.getSurefireReportsFile( "TEST-MainTest.xml", UTF8 );
        assertTrue( report.exists() );
        report.assertContainsText( "<property name=\"sun.java.command\" value=\"org.apache.maven.surefire.booter.ForkedBooter");
        report.assertContainsText( "<property name=\"reuseForks\" value=\"false\"/>" );
        report.assertContainsText( "<property name=\"forkCount\" value=\"1\"/>" );
        report.assertContainsText( "<testcase name=\"test1\"" );
        report.assertContainsText( "<testcase name=\"test2\"" );
    }

    private SurefireLauncher unpack()
    {
        return unpack("/surefire-1534-reuse-forks-false-java-module");
    }

}
