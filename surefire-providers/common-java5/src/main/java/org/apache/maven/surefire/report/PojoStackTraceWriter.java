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
        if ( t != null )
        {
            t.printStackTrace( new PrintWriter( w ) );
            w.flush();
        }
        return w.toString();
    }

    public String smartTrimmedStackTrace()
    {
        SmartStackTraceParser parser = new SmartStackTraceParser( testClass, t, testMethod );
        return parser.getString();
    }

    public String writeTrimmedTraceToString()
    {
        return SmartStackTraceParser.innerMostWithFocusOnClass( t, testClass );
    }

    public SafeThrowable getThrowable()
    {
        return new SafeThrowable( t );
    }
}
