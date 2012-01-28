package org.apache.maven.surefire.group.match;


public interface GroupMatcher
{

    void loadGroupClasses( ClassLoader cloader );

    boolean enabled( Class<?>... cats );

    boolean enabled( String... cats );

}
