package org.apache.maven.surefire.group.match;

import junit.framework.TestCase;

public class SingleGroupMatcherTest
    extends TestCase
{

    public void testMatchExactClassName()
    {
        SingleGroupMatcher matcher = new SingleGroupMatcher( SingleGroupMatcher.class.getName() );
        assertTrue( matcher.enabled( SingleGroupMatcher.class ) );
    }

    public void testMatchLoadedClass()
    {
        SingleGroupMatcher matcher = new SingleGroupMatcher( SingleGroupMatcher.class.getName() );
        matcher.loadGroupClasses( Thread.currentThread().getContextClassLoader() );
        assertTrue( matcher.enabled( SingleGroupMatcher.class ) );
    }

    public void testMatchClassNameWithoutPackage()
    {
        SingleGroupMatcher matcher = new SingleGroupMatcher( SingleGroupMatcher.class.getSimpleName() );
        assertTrue( matcher.enabled( SingleGroupMatcher.class ) );
    }

}
