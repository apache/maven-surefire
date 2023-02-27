package org.acme.tests;

import org.junit.Test;

public class ClasspathTestA
{

    @Test
    public void verifyAllTestClassesAreInClasspath()
        throws Exception
    {
        Class.forName( "org.acme.tests.TestA" );
        Class.forName( "org.acme.tests.TestB" );
        Class.forName( "org.acme.othertests.OtherTestA" );
        Class.forName( "org.acme.classifiedtests.ClassifiedTestA" );
    }
}
