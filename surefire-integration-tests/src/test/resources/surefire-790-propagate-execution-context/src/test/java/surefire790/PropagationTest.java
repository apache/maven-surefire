package surefire790;

import junit.framework.TestCase;

public class PropagationTest
    extends TestCase
{

    public void testPropertyPropagation()
    {
        assertNotNull( "property pom not set", System.getProperty( "maven.execution.pom" ) );
    }
}
