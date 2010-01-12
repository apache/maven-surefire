package org.apache.maven.surefire.its;


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.List;

/**
 * Basic suite test using all known versions of JUnit 4.x
 *
 * @author <a href="mailto:kristian.rosenvold@gmail.com">Kristian Rosenvold</a>
 *
 */
public class Junit47concurrencyIT
    extends AbstractSurefireIntegrationTestClass
{


    public void test47 () throws Exception
    {
        runJUnitTest();
    }


    public void runJUnitTest ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/concurrentjunit47" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List arguments = this.getInitialGoals();
        arguments.add( "test" );
        // DGF we have to pass in the version as a command line argument
        // and NOT as a system property; otherwise our setting will be ignored
        arguments.add( "-DjunitVersion=4.7");
        verifier.executeGoals( arguments );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        IntegrationTestSuiteResults suite = HelperAssertions.parseTestResults( new File[] { testDir } );
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, suite );
    }
}