package org.apache.maven.surefire.its;

import java.io.File;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;

/**
 * Test basic default configuration, runs the JUnit 3 test in the src/test directory.
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class DefaultConfigurationTest
    extends AbstractMavenIntegrationTestCase
{
    public void testDefaultConfiguration ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.surefire.its", "default-configuration", "1.0-SNAPSHOT", "jar" );

//        verifier.executeGoal( "clean" );
//        verifier.verifyErrorFreeLog();
//        verifier.resetStreams();
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        SurefireReportParser parser = new SurefireReportParser();
        File reportsDir = new File( testDir, "target/surefire-reports" );
        assertTrue( "Reports directory is missing: " + reportsDir.getAbsolutePath(), reportsDir.exists() );
        parser.setReportsDirectory( reportsDir );
        List reports = parser.parseXMLReportFiles();
        assertTrue( "No reports!", reports.size() > 0 );
        ReportTestSuite suite = (ReportTestSuite) reports.get( 0 );
        HelperAssertions.assertTestSuite( 1, 0, 0, 0, suite );
        
    }
}
