package org.apache.maven.surefire.its;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test files with "Abstract" in their name that aren't really abstract,
 * and abstract classes that don't say "Abstract" in their name 
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * 
 */
public class AbstractTestCaseTest
    extends TestCase
{
    public void testAbstractTestCase ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/default-configuration-abstract" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        HelperAssertions.assertTestSuiteResults( 1, 0, 0, 0, testDir );        
    }
}
