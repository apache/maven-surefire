package org.apache.maven.surefire.its;

import org.apache.maven.surefire.its.fixture.SurefireIntegrationTestCase;

/**
 * Test when the configured working directory is an invalid property, SUREFIRE-715
 *
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class WorkingDirectoryIsInvalidPropertyIT
    extends SurefireIntegrationTestCase
{
    public void testWorkingDirectory()
        throws Exception
    {
        unpack( "working-directory-is-invalid-property" )
            .executeTestWithFailure()
            .verifyTextInLog( "workingDirectory cannot be null" );
    }
}