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

import org.codehaus.plexus.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Write the trace out for a POJO test.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PojoStackTraceWriter
    implements StackTraceWriter
{
    private final Throwable t;

    protected final String testClass;

    protected final String testMethod;

    public PojoStackTraceWriter( String testClass, String testMethod, Throwable t )
    {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.t = t;
    }

    public String writeTraceToString()
    {
        StringWriter w = new StringWriter();
        t.printStackTrace( new PrintWriter( w ) );
        w.flush();
        return w.toString();
    }

    public String writeTrimmedTraceToString()
    {
        String text = writeTraceToString();

        String marker = "at " + testClass + "." + testMethod;

        String[] lines = StringUtils.split( text, "\n" );
        int lastLine = lines.length - 1;
        // skip first
        for ( int i = 1; i < lines.length; i++ )
        {
            if ( lines[i].trim().startsWith( marker ) )
            {
                lastLine = i;
            }
        }

        StringBuffer trace = new StringBuffer();
        for ( int i = 0; i <= lastLine; i++ )
        {
            trace.append( lines[i] );
            trace.append( "\n" );
        }

        for ( int i = lastLine; i < lines.length; i++ )
        {
            if ( lines[i].trim().startsWith( "Caused by" ) )
            {
                lastLine = i;
                break;
            }
        }

        for ( int i = lastLine; i < lines.length; i++ )
        {
            trace.append( lines[i] );
            trace.append( "\n" );
        }

        return trace.toString();
    }

    public Throwable getThrowable()
    {
        return t;
    }
}
