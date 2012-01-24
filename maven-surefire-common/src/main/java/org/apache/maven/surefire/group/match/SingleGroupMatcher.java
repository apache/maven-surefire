package org.apache.maven.surefire.group.match;


public class SingleGroupMatcher
    implements GroupMatcher
{

    private String enabled;

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( enabled == null ) ? 0 : enabled.hashCode() );
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
        SingleGroupMatcher other = (SingleGroupMatcher) obj;
        if ( enabled == null )
        {
            if ( other.enabled != null )
                return false;
        }
        else if ( !enabled.equals( other.enabled ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "*" + enabled;
    }

    public SingleGroupMatcher( String disabled )
    {
        this.enabled = disabled;
    }

    public boolean enabled( Class<?>... cats )
    {
        if ( cats != null )
        {
            for ( Class<?> cls : cats )
            {
                String name = cls.getName();
                if ( name.endsWith( enabled ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean enabled( String... cats )
    {
        if ( enabled == null )
        {
            return true;
        }

        for ( String cat : cats )
        {
            if ( cat == null || cat.trim().length() < 1 )
            {
                continue;
            }

            System.out.println( cat + ".endsWith(" + enabled + ")? " + ( cat.endsWith( enabled ) ) );
            if ( cat.endsWith( enabled ) )
            {
                return true;
            }
        }

        return false;
    }

}
