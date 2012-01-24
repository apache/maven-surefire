package org.apache.maven.surefire.group.match;

import junit.framework.TestCase;

public class AndGroupMatcherTest
    extends TestCase
{

    public void testDontMatchOneInGroup()
    {
        AndGroupMatcher matcher =
            new AndGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );
        
        assertFalse( matcher.enabled( InverseGroupMatcher.class, AndGroupMatcher.class ) );
    }

    public void testMatchBothInGroup()
    {
        AndGroupMatcher matcher =
            new AndGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertTrue( matcher.enabled( InverseGroupMatcher.class, SingleGroupMatcher.class ) );
    }

    public void testDontMatchAnyInGroup()
    {
        AndGroupMatcher matcher =
            new AndGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertFalse( matcher.enabled( OrGroupMatcher.class, AndGroupMatcher.class ) );
    }

}
