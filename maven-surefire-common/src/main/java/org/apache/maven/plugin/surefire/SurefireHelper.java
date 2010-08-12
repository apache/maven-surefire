package org.apache.maven.plugin.surefire;

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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.surefire.booter.SurefireBooter;

/**
 * Helper class for surefire plugins
 */
public final class SurefireHelper
{

    /**
     * Do not instantiate.
     */
    private SurefireHelper()
    {
        throw new IllegalAccessError( "Utility class" );
    }

    public static void reportExecution( SurefireReportParameters reportParameters, int result, Log log )
        throws MojoFailureException
    {
        if ( result == 0 )
        {
            return;
        }

        String msg;

        if ( result == SurefireBooter.NO_TESTS_EXIT_CODE )
        {
            if ( ( reportParameters.getFailIfNoTests() == null ) || !reportParameters.getFailIfNoTests().booleanValue() )
            {
                return;
            }
            // TODO: i18n
            throw new MojoFailureException(
                "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
        }
        else
        {
            // TODO: i18n
            msg = "There are test failures.\n\nPlease refer to " + reportParameters.getReportsDirectory()
                + " for the individual test results.";

        }

        if ( reportParameters.isTestFailureIgnore() )
        {
            log.error( msg );
        }
        else
        {
            throw new MojoFailureException( msg );
        }
    }
}
