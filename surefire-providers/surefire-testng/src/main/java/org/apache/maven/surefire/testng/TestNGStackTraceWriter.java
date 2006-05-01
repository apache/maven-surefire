/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

package org.apache.maven.surefire.testng;

import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.codehaus.plexus.util.StringUtils;
import org.testng.ITestResult;

/**
 * Write out stack traces for TestNG.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TestNGStackTraceWriter
    extends PojoStackTraceWriter
{
    public TestNGStackTraceWriter( ITestResult result )
    {
        super( result.getTestClass().getRealClass().getName(), result.getMethod().getMethodName(),
               result.getThrowable() );
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
            // if you call assertions from JUnit tests in TestNG, it ends up at the top of the trace
            if ( !lines[i].trim().startsWith( "at junit.framework.Assert" ) )
            {
                trace.append( lines[i] );
                trace.append( "\n" );
            }
        }

        return trace.toString();
    }
}
