package org.apache.maven.surefire.group.match;

import junit.framework.TestCase;

public class InverseGroupMatcherTest
    extends TestCase
{

    public void testInvertSingleMatcher()
    {
        InverseGroupMatcher matcher =
            new InverseGroupMatcher( new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );
        assertFalse( matcher.enabled( InverseGroupMatcher.class ) );
    }

}
