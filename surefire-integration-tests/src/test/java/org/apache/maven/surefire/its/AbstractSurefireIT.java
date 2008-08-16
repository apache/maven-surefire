package org.apache.maven.surefire.its;

import java.util.ArrayList;

import junit.framework.TestCase;


/**
 * Base class of all integration test cases. Mainly used to pickup surefire version
 * from system property
 * @author Dan T. Tran
 *
 */
public abstract class AbstractSurefireIT
    extends TestCase
{
    private String surefireVersion = System.getProperty( "surefire.version" );

    protected ArrayList getInitialGoals()
    {
        ArrayList goals = new ArrayList();
        goals.add( "-Dsurefire.version=" + surefireVersion );
        return goals;
    }

}
