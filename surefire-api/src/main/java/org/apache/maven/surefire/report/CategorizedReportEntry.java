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

import java.util.Collections;
import java.util.Map;

/**
 * @author Kristian Rosenvold
 */
public class CategorizedReportEntry
    extends SimpleReportEntry
    implements ReportEntry
{
    private static final String GROUP_PREFIX = " (of ";

    private static final String GROUP_SUFIX = ")";

    private final String group;

    public CategorizedReportEntry( String source, String name, String group )
    {
        this( source, name, group, null, null );
    }

    public CategorizedReportEntry( String source, String name, String group, StackTraceWriter stackTraceWriter,
                                   Integer elapsed )
    {
        super( source, name, stackTraceWriter, elapsed );
        this.group = group;
    }

    public CategorizedReportEntry( String source, String name, String group, StackTraceWriter stackTraceWriter,
                                   Integer elapsed, String message )
    {
        this( source, name, group, stackTraceWriter, elapsed, message, Collections.<String, String>emptyMap() );
    }

    public CategorizedReportEntry( String source, String name, String group, StackTraceWriter stackTraceWriter,
                                   Integer elapsed, String message, Map<String, String> systemProperties )
    {
        super( source, name, stackTraceWriter, elapsed, message, systemProperties );
        this.group = group;
    }

    public static TestSetReportEntry reportEntry( String source, String name, String group,
                                                  StackTraceWriter stackTraceWriter, Integer elapsed, String message,
                                                  Map<String, String> systemProperties )
    {
        return group != null
            ? new CategorizedReportEntry( source, name, group, stackTraceWriter, elapsed, message, systemProperties )
            : new SimpleReportEntry( source, name, stackTraceWriter, elapsed, message, systemProperties );
    }

    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public String getNameWithGroup()
    {
        return isNameWithGroup() ? getName() + GROUP_PREFIX + getGroup() + GROUP_SUFIX : getName();
    }

    @Override
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
        if ( !super.equals( o ) )
        {
            return false;
        }

        CategorizedReportEntry that = (CategorizedReportEntry) o;

        return !( group != null ? !group.equals( that.group ) : that.group != null );

    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + ( group != null ? group.hashCode() : 0 );
        return result;
    }

    private boolean isNameWithGroup()
    {
        return getGroup() != null && !getGroup().equals( getName() );
    }
}
