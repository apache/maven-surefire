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

import org.apache.maven.surefire.util.internal.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Kristian Rosenvold
 */
public class RunStatistics
    extends TestSetStatistics
{
    /**
     * Holds the source(s) that causes the error(s).
     */
    private final Sources errorSources = new Sources();

    /**
     * Holds the source(s) that causes the failure(s).
     */
    private final Sources failureSources = new Sources();


    // Todo remove when building with 2.7.2
    public void addErrorSource( String errorSource )
    {
        errorSources.addSource( errorSource );
    }

    public void addErrorSource( String errorSource, StackTraceWriter stackTraceWriter )
    {
        errorSources.addSource( errorSource, stackTraceWriter );
    }

    // Todo remove when building with 2.7.2
    public void addFailureSource( String failureSource )
    {
    	failureSources.addSource( failureSource );
    }

    public void addFailureSource( String failureSource, StackTraceWriter stackTraceWriter  )
    {
    	failureSources.addSource( failureSource, stackTraceWriter );
    }

    public Collection getErrorSources()
    {
        return errorSources.getListOfSources();
    }

    public Collection getFailureSources()
    {
        return failureSources.getListOfSources();
    }

    private static class Sources
    {
        private final Collection listOfSources = new ArrayList();

        void addSource( String source )
        {
            synchronized ( listOfSources )
            {
                listOfSources.add( source );  		
            }
        }

        void addSource( String source, StackTraceWriter stackTraceWriter )
        {
            String message = getMessageOfThrowable( stackTraceWriter );
            String extendedSource = StringUtils.isBlank( message ) ? source : source + ": " + message;
            addSource( extendedSource );
        }

        Collection getListOfSources()
        {
            synchronized ( listOfSources )
            {
                return Collections.unmodifiableCollection( listOfSources );
            }
        }

        private String getMessageOfThrowable( StackTraceWriter stackTraceWriter )
        {
            //noinspection ThrowableResultOfMethodCallIgnored
            return stackTraceWriter != null ? getMessageOfThrowable( stackTraceWriter.getThrowable() ) : "";
        }

        private String getMessageOfThrowable( Throwable throwable )
        {
            return throwable != null ? throwable.getLocalizedMessage() : "";
        }
    }
}
