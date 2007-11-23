package org.apache.maven.surefire.its;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;

/**
 * Basic suite test using all known versions of TestNG
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class TestNgVersionsTest
    extends AbstractMavenIntegrationTestCase
{
    
    public void test47 () throws Exception
    {
        runTestNgTest( "4.7" );
    }
    
    public void test50 () throws Exception
    {
        runTestNgTest( "5.0" );
    }
    
    public void test501 () throws Exception
    {
        runTestNgTest( "5.0.1" );
    }
    
    public void test502 () throws Exception
    {
        runTestNgTest( "5.0.2" );
    }
    
    public void test51 () throws Exception
    {
        runTestNgTest( "5.1" );
    }
    
    public void test55 () throws Exception
    {
        runTestNgTest( "5.5" );
    }
    
    public void test56 () throws Exception
    {
        runTestNgTest( "5.6" );
    }
    
    public void test57 () throws Exception
    {
        runTestNgTest( "5.7" );
    }
    
    public void runTestNgTest (String version)
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-simple" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List arguments = new ArrayList();
        arguments.add( "test" );
        // DGF we have to pass in the version as a command line argument
        // and NOT as a system property; otherwise our setting will be ignored
        arguments.add( "-DtestNgVersion=" + version );
        verifier.executeGoals( arguments );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        ITSuiteResults suite = HelperAssertions.parseTestResults( testDir );
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, suite );
    }
}
