package org.apache.maven.surefire.booter;

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


import org.apache.maven.surefire.api.suite.RunResult;

import static org.apache.maven.surefire.shared.utils.StringUtils.isNotBlank;

/**
 * Encapsulates exceptions thrown during Surefire forking.
 */
public class SurefireBooterForkException
    extends Exception
{
    public SurefireBooterForkException( String message, RunResult runResult )
    {
        this( message, null, null, runResult );
    }

    public SurefireBooterForkException( String message, String rethrownMessage, Throwable rethrownCause,
                                        RunResult runResult )
    {
        super( toString( message, rethrownMessage, rethrownCause, runResult ), rethrownCause );
    }

    public SurefireBooterForkException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public SurefireBooterForkException( String msg )
    {
        super( msg );
    }

    private static String toString( String message, String rethrownMessage, Throwable rethrownCause,
                                    RunResult runResult )
    {
        return toNewLines( message,
                                 rethrownMessage,
                                 rethrownCause == null ? null : rethrownCause.getLocalizedMessage(),
                                 runResult == null ? null : runResult.getFailure(),
                                 runResult == null ? null : toString( runResult ) );
    }

    private static String toString( RunResult runResult )
    {
        return "Fatal Tests run: " + runResult.getCompletedCount()
                       + ", Failures: " + runResult.getFailures()
                       + ", Errors: " + runResult.getErrors()
                       + ", Skipped: " + runResult.getSkipped()
                       + ", Flakes: " + runResult.getFlakes()
                       + ", Elapsed timeout: " + runResult.isTimeout();
    }

    private static String toNewLines( String... messages )
    {
        StringBuilder result = new StringBuilder();
        for ( String message : messages )
        {
            if ( isNotBlank( message ) )
            {
                if ( result.length() == 0 )
                {
                    result.append( '\n' );
                }
                result.append( message );
            }
        }
        return result.toString();
    }
}
