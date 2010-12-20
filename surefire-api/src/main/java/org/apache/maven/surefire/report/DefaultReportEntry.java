package org.apache.maven.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Kristian Rosenvold
 */
public class DefaultReportEntry
    implements ReportEntry
{
    private final String source;

    private final String name;

    private final String group;

    private final String message;

    private final StackTraceWriter stackTraceWriter;

    private final Integer elapsed;

    protected DefaultReportEntry( String name, String group )
    {
        this.name = name;
        this.group = group;
        this.stackTraceWriter = null;
        this.elapsed = null;
        this.message = null;
        this.source = null;
    }

    public DefaultReportEntry( String source, String name, String message )
    {
        this( source, name, null, message, null, null );
    }

    public DefaultReportEntry( String source, String name, String message, StackTraceWriter stackTraceWriter )
    {
        this( source, name, null, message, stackTraceWriter, null );
    }

    public DefaultReportEntry( String source, String name, String group, String message, StackTraceWriter stackTraceWriter )
    {
        this( source, name, group, message, stackTraceWriter, null );
    }

    public DefaultReportEntry( String source, String name, String group, String message, StackTraceWriter stackTraceWriter,
                        Integer elapsed )
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

        this.group = group;

        this.message = message;

        this.stackTraceWriter = stackTraceWriter;

        this.elapsed = elapsed;
    }

    public String getSourceName()
    {
        return source;
    }

    public String getName()
    {
        return name;
    }

    public String getGroup()
    {
        return group;
    }

    public String getMessage()
    {
        return message;
    }

    public StackTraceWriter getStackTraceWriter()
    {
        return stackTraceWriter;
    }

    public Integer getElapsed()
    {
        return elapsed;
    }

    public String toString()
    {
        return "ReportEntry{" + "source='" + source + '\'' + ", name='" + name + '\'' + ", group='" + group + '\'' +
            ", message='" + message + '\'' + ", stackTraceWriter=" + stackTraceWriter + ", elapsed=" + elapsed + '}';
    }

    public DefaultReportEntry( String source, String name, String message, Integer elapsed )
    {
        this( source, name, null, message, null, elapsed );
    }

    public DefaultReportEntry( String source, String name, String group, String message )
    {
        this( source, name, group, message, null, null );
    }

    public DefaultReportEntry( String source, String name, String message, StackTraceWriter stackTraceWriter,
                               Integer elapsed )
    {
        this( source, name, null, message, stackTraceWriter, elapsed );
    }

    public static ReportEntry nameGroup( String name, String group )
    {
        return new DefaultReportEntry( name, group );
    }

    /** @noinspection RedundantIfStatement*/
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        DefaultReportEntry that = (DefaultReportEntry) o;

        if ( elapsed != null ? !elapsed.equals( that.elapsed ) : that.elapsed != null )
        {
            return false;
        }
        if ( group != null ? !group.equals( that.group ) : that.group != null )
        {
            return false;
        }
        if ( message != null ? !message.equals( that.message ) : that.message != null )
        {
            return false;
        }
        if ( name != null ? !name.equals( that.name ) : that.name != null )
        {
            return false;
        }
        if ( source != null ? !source.equals( that.source ) : that.source != null )
        {
            return false;
        }
        if ( stackTraceWriter != null
            ? !stackTraceWriter.equals( that.stackTraceWriter )
            : that.stackTraceWriter != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + ( name != null ? name.hashCode() : 0 );
        result = 31 * result + ( group != null ? group.hashCode() : 0 );
        result = 31 * result + ( message != null ? message.hashCode() : 0 );
        result = 31 * result + ( stackTraceWriter != null ? stackTraceWriter.hashCode() : 0 );
        result = 31 * result + ( elapsed != null ? elapsed.hashCode() : 0 );
        return result;
    }


}
