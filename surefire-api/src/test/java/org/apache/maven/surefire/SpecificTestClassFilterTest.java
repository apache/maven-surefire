package org.apache.maven.surefire;

import junit.framework.TestCase;

public class SpecificTestClassFilterTest
    extends TestCase
{

    public void testMatchSingleCharacterWildcard()
    {
        SpecificTestClassFilter filter =
            new SpecificTestClassFilter( new String[] { "org/apache/maven/surefire/?pecificTestClassFilter.class" } );

        assertTrue( filter.accept( SpecificTestClassFilter.class ) );
    }

    public void testMatchSingleSegmentWordWildcard()
    {
        SpecificTestClassFilter filter =
            new SpecificTestClassFilter( new String[] { "org/apache/maven/surefire/*TestClassFilter.class" } );

        assertTrue( filter.accept( SpecificTestClassFilter.class ) );
    }

    public void testMatchMultiSegmentWildcard()
    {
        SpecificTestClassFilter filter =
            new SpecificTestClassFilter( new String[] { "org/**/SpecificTestClassFilter.class" } );

        assertTrue( filter.accept( SpecificTestClassFilter.class ) );
    }

    public void testMatchSingleSegmentWildcard()
    {
        SpecificTestClassFilter filter =
            new SpecificTestClassFilter( new String[] { "org/*/maven/surefire/SpecificTestClassFilter.class" } );

        assertTrue( filter.accept( SpecificTestClassFilter.class ) );
    }

}
