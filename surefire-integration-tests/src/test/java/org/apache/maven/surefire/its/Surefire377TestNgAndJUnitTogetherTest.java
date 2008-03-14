package org.apache.maven.surefire.its;


import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * SUREFIRE-377 (When JUnit and TestNG tests are in same project, only one set gets run).
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class Surefire377TestNgAndJUnitTogetherTest
    extends TestCase
{
    public void testTestNgAndJUnitTogether ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-junit-together" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, testDir );
    }
}
