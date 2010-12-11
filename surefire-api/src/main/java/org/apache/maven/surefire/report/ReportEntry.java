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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

// todo: Make this an interface when surefire 2.7.1 compiles with surefire 2.7.0
public class ReportEntry
{
    private final String source;

    private final String name;

    private final String group;

    private final String message;

    private final StackTraceWriter stackTraceWriter;

    protected ReportEntry( String name, String group )
    {
        this.name = name;
        this.group = group;
        this.stackTraceWriter = null;
        this.message = null;
        this.source = null;
    }

    public ReportEntry( String source, String name, String message )
    {
        this( source, name, null, message, null );
    }

    public ReportEntry( String source, String name, String message, StackTraceWriter stackTraceWriter )
    {
        this( source, name, null, message, stackTraceWriter );
    }

    protected ReportEntry( String source, String name, String group, String message, StackTraceWriter stackTraceWriter )
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

    public boolean equals( Object obj )
    {
        if ( !( obj instanceof ReportEntry ) )
        {
            return false;
        }
        if ( this == obj )
        {
            return true;
        }
        ReportEntry rhs = (ReportEntry) obj;
        return new EqualsBuilder().append( getSourceName(), rhs.getSourceName() ).append( getName(),
                                                                                          rhs.getName() ).append(
            getGroup(), rhs.getGroup() ).append( getMessage(), rhs.getMessage() ).append( getStackTraceWriter(),
                                                                                          rhs.getStackTraceWriter() ).isEquals();
    }

    public String toString()
    {
        return new ToStringBuilder( this ).append( "source", getSourceName() ).append( "name", getName() ).append(
            "group", getGroup() ).append( "message", getMessage() ).append( "stackTraceWriter",
                                                                            getStackTraceWriter() ).toString();
    }

    public int hashCode()
    {
        // you pick a hard-coded, randomly chosen, non-zero, odd number
        // ideally different for each class
        // good resource at http://primes.utm.edu/lists/small/1000.txt
        return new HashCodeBuilder( 5897, 6653 ).append( getSourceName() ).append( getName() ).append(
            getGroup() ).append( getMessage() ).append( getStackTraceWriter() ).toHashCode();
    }

}
