package org.codehaus.surefire.report;

public class ReportEntry
{
    private Object source;

    private String name;

    private String message;

    private Throwable throwable;
        
    public ReportEntry( Object source, String name, String message )
    {
        if ( source == null )
        {
            throw new NullPointerException( "source is null" );
        }
        if ( name == null )
        {
            throw new NullPointerException( "name is null" );
        }
        if ( message == null )
        {
            throw new NullPointerException( "message is null" );
        }

        this.source = source;

        this.name = name;

        this.message = message;
    }

    public ReportEntry( Object source, String name, String message, Throwable throwable )
    {
        if ( source == null )
        {
            throw new NullPointerException( "source is null" );
        }
        if ( name == null )
        {
            throw new NullPointerException( "name is null" );
        }
        if ( message == null )
        {
            throw new NullPointerException( "message is null" );
        }
        if ( throwable == null )
        {
            throw new NullPointerException( "throwable is null" );
        }

        this.source = source;

        this.name = name;

        this.message = message;

        this.throwable = throwable;
    }

    public Object getSource()
    {
        return source;
    }

    public String getName()
    {
        return name;
    }

    public String getMessage()
    {
        return message;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }
}

