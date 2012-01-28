package org.apache.maven.surefire.group.match;

import java.util.Collection;

public class OrGroupMatcher
    extends JoinGroupMatcher
{

    public OrGroupMatcher( GroupMatcher... matchers )
    {
        for ( GroupMatcher matcher : matchers )
        {
            addMatcher( matcher );
        }
    }

    public OrGroupMatcher( Collection<GroupMatcher> matchers )
    {
        for ( GroupMatcher matcher : matchers )
        {
            addMatcher( matcher );
        }
    }

    public boolean enabled( Class<?>... cats )
    {
        for ( GroupMatcher matcher : getMatchers() )
        {
            boolean result = matcher.enabled( cats );
            if ( result )
            {
                return true;
            }
        }

        return false;
    }

    public boolean enabled( String... cats )
    {
        for ( GroupMatcher matcher : getMatchers() )
        {
            boolean result = matcher.enabled( cats );
            if ( result )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for ( GroupMatcher matcher : getMatchers() )
        {
            if ( sb.length() > 0 )
            {
                sb.append( " OR " );
            }
            sb.append( matcher );
        }

        return sb.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 37;
        int result = 1;
        result = prime * result;
        for ( GroupMatcher matcher : getMatchers() )
        {
            result += matcher.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        AndGroupMatcher other = (AndGroupMatcher) obj;
        if ( !getMatchers().equals( other.getMatchers() ) )
        {
            return false;
        }
        return true;
    }
}
