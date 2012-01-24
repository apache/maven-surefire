package org.apache.maven.surefire.group.match;


public interface GroupMatcher
{

    boolean enabled( Class<?>... cats );

    boolean enabled( String... cats );

}
