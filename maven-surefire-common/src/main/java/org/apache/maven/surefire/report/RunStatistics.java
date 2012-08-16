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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.util.internal.StringUtils;

/**
 * @author Kristian Rosenvold
 */
public class RunStatistics {
    /**
     * Holds the source(s) that causes the error(s).
     */
    private final Sources errorSources = new Sources();

    /**
     * Holds the source(s) that causes the failure(s).
     */
    private final Sources failureSources = new Sources();
    private int completedCount;
    private int errors;
    private int failures;
    private int skipped;


    public void addErrorSource( String errorSource, StackTraceWriter stackTraceWriter )
    {
        errorSources.addSource( errorSource, stackTraceWriter );
    }

    public void addFailureSource( String failureSource, StackTraceWriter stackTraceWriter )
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

    public synchronized boolean hadFailures()
    {
        return failures > 0;
    }

    public synchronized boolean hadErrors()
    {
        return errors > 0;
    }

    public synchronized int getCompletedCount()
    {
        return completedCount;
    }

    public synchronized int getSkipped()
    {
        return skipped;
    }

    public synchronized void add( TestSetStats testSetStats){
        this.completedCount += testSetStats.getCompletedCount();
        this.errors += testSetStats.getErrors();
        this.failures += testSetStats.getFailures();
        this.skipped += testSetStats.getSkipped();
    }

    public synchronized RunResult getRunResult()
    {
        return new RunResult( completedCount, errors, failures, skipped );
    }

    public synchronized String getSummary()
    {
        return "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors + ", Skipped: "
            + skipped;
    }


    private static class Sources
    {
        private final Collection<String> listOfSources = new ArrayList<String>();

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
            String extendedSource =
                StringUtils.isBlank( message ) ? source : source + ": " + trimToSingleLine( message );
            addSource( extendedSource );
        }

        private String trimToSingleLine( String str )
        {
            int i = str.indexOf( "\n" );
            return i >= 0 ? str.substring( 0, i ) + "(..)" : str;
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

        private String getMessageOfThrowable( SafeThrowable throwable )
        {
            return throwable != null ? throwable.getLocalizedMessage() : "";
        }
    }
}
