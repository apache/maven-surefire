package org.apache.maven.surefire.group.match;

import junit.framework.TestCase;

public class OrGroupMatcherTest
    extends TestCase
{

    public void testMatchOneInOredGroup()
    {
        OrGroupMatcher matcher =
            new OrGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );
        
        assertTrue( matcher.enabled( InverseGroupMatcher.class, AndGroupMatcher.class ) );
    }

    public void testMatchBothInOredGroup()
    {
        OrGroupMatcher matcher =
            new OrGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertTrue( matcher.enabled( InverseGroupMatcher.class, SingleGroupMatcher.class ) );
    }

    public void testMatchNoneInOredGroup()
    {
        OrGroupMatcher matcher =
            new OrGroupMatcher( new SingleGroupMatcher( SingleGroupMatcher.class.getName() ),
                                new SingleGroupMatcher( InverseGroupMatcher.class.getName() ) );

        assertFalse( matcher.enabled( OrGroupMatcher.class, AndGroupMatcher.class ) );
    }

}
