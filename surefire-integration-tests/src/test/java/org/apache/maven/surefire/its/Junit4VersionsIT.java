package org.apache.maven.surefire.its;


import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Basic suite test using all known versions of JUnit 4.x
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Junit4VersionsIT
    extends AbstractSurefireIntegrationTestClass
{
    
    public void test40 () throws Exception
    {
        runJUnitTest( "4.0" );
    }
    
    public void test41 () throws Exception
    {
        runJUnitTest( "4.1" );
    }
    
    public void test42 () throws Exception
    {
        runJUnitTest( "4.2" );
    }
    
    public void test43 () throws Exception
    {
        runJUnitTest( "4.3" );
    }
    
    public void test431 () throws Exception
    {
        runJUnitTest( "4.3.1" );
    }
    
    public void test44 () throws Exception
    {
        runJUnitTest( "4.4" );
    }

    public void test45 () throws Exception
    {
        runJUnitTest( "4.5" );
    }

    public void test46 () throws Exception
    {
        runJUnitTest( "4.6" );
    }

    public void test47 () throws Exception
    {
        runJUnitTest( "4.7" );
    }

    
    public void runJUnitTest (String version)
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit4" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List arguments = this.getInitialGoals();
        arguments.add( "test" );
        // DGF we have to pass in the version as a command line argument
        // and NOT as a system property; otherwise our setting will be ignored
        arguments.add( "-DjunitVersion=" + version );
        verifier.executeGoals( arguments );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        IntegrationTestSuiteResults suite = HelperAssertions.parseTestResults( new File[] { testDir } );
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, suite );
    }
}
