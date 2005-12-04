package org.apache.maven.surefire.report;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

