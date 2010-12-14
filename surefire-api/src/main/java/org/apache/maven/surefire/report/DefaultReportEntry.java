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
    extends ReportEntry
{
    public DefaultReportEntry( String source, String name, String message )
    {
        super( source, name, null, message, null);
    }

    public DefaultReportEntry( String source, String name, String message, Integer elapsed )
    {
        super( source, name, null, message, null, elapsed );
    }

    public DefaultReportEntry( String source, String name, String group, String message )
    {
        super( source, name, group, message, null, null );
    }

    public DefaultReportEntry( String source, String name, String message, StackTraceWriter stackTraceWriter )
    {
        super( source, name, null, message, stackTraceWriter, null );
    }

    public DefaultReportEntry( String source, String name, String message, StackTraceWriter stackTraceWriter,
                               Integer elapsed )
    {
        super( source, name, null, message, stackTraceWriter, elapsed );
    }

    public static ReportEntry nameGroup( String name, String group )
    {
        return new ReportEntry( name, group );
    }
}
