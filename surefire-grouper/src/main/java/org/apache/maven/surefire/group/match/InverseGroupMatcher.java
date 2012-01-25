package org.apache.maven.surefire.group.match;


public class InverseGroupMatcher
    implements GroupMatcher
{

    private final GroupMatcher matcher;

    public InverseGroupMatcher( GroupMatcher matcher )
    {
        this.matcher = matcher;
    }

    public boolean enabled( Class<?>... cats )
    {
        return cats == null ? true : !matcher.enabled( cats );
    }

    public boolean enabled( String... cats )
    {
        return cats == null ? true : !matcher.enabled( cats );
    }

    @Override
    public String toString()
    {
        return "NOT " + matcher;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( matcher == null ) ? 0 : matcher.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        InverseGroupMatcher other = (InverseGroupMatcher) obj;
        if ( matcher == null )
        {
            if ( other.matcher != null )
                return false;
        }
        else if ( !matcher.equals( other.matcher ) )
            return false;
        return true;
    }

    public void loadGroupClasses( ClassLoader cloader )
    {
        matcher.loadGroupClasses( cloader );
    }

}
