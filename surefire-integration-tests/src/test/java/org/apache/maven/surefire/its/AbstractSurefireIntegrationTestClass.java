package org.apache.maven.surefire.its;

import junit.framework.TestCase;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

import java.util.ArrayList;


/**
 * Base class of all integration test cases. Mainly used to pickup surefire version
 * from system property
 *
 * @author Dan T. Tran
 */
public abstract class AbstractSurefireIntegrationTestClass
    extends TestCase
{
    private String surefireVersion = System.getProperty( "surefire.version" );

    protected ArrayList getInitialGoals()
    {
        ArrayList goals = new ArrayList();
        goals.add( "-Dsurefire.version=" + surefireVersion );
        return goals;
    }

    protected void executeGoal( Verifier verifier, String goal )
        throws VerificationException
    {
        ArrayList goals = this.getInitialGoals();
        goals.add( goal );
        if ( !verifier.getCliOptions().contains( "-s" ) )
        {
            verifier.getCliOptions().add( "-s" );
            verifier.getCliOptions().add( System.getProperty( "maven.staged.local.repo" ) );
        }
        verifier.executeGoals( goals );
    }

}
