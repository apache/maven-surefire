package org.apache.maven.surefire.group.match;

import java.util.HashSet;
import java.util.Set;

public abstract class JoinGroupMatcher
    implements GroupMatcher
{

    Set<GroupMatcher> matchers = new HashSet<GroupMatcher>();

    public final boolean addMatcher( GroupMatcher matcher )
    {
        return matchers.add( matcher );
    }

    protected final Set<GroupMatcher> getMatchers()
    {
        return matchers;
    }

    public void loadGroupClasses( ClassLoader cloader )
    {
        for ( GroupMatcher matcher : matchers )
        {
            matcher.loadGroupClasses( cloader );
        }
    }

}
