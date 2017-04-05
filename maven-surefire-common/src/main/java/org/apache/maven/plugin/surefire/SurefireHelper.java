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

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.internal.DumpFileUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.apache.maven.surefire.booter.DumpErrorSingleton.DUMPSTREAM_FILE_EXT;
import static org.apache.maven.surefire.booter.DumpErrorSingleton.DUMP_FILE_EXT;
import static org.apache.maven.surefire.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.cli.CommandLineOption.LOGGING_LEVEL_ERROR;
import static org.apache.maven.surefire.cli.CommandLineOption.LOGGING_LEVEL_INFO;
import static org.apache.maven.surefire.cli.CommandLineOption.LOGGING_LEVEL_WARN;
import static org.apache.maven.surefire.cli.CommandLineOption.SHOW_ERRORS;

/**
 * Helper class for surefire plugins
 */
public final class SurefireHelper
{
    private static final String DUMP_FILE_DATE = DumpFileUtils.newFormattedDateFileName();

    public static final String DUMP_FILE_PREFIX = DUMP_FILE_DATE + "-jvmRun";

    public static final String DUMPSTREAM_FILENAME_FORMATTER = DUMP_FILE_PREFIX + "%d" + DUMPSTREAM_FILE_EXT;

    private static final String[] DUMP_FILES_PRINT =
            {
                    "[date]-jvmRun[N]" + DUMP_FILE_EXT,
                    "[date]" + DUMPSTREAM_FILE_EXT,
                    "[date]-jvmRun[N]" + DUMPSTREAM_FILE_EXT
            };

    /**
     * Do not instantiate.
     */
    private SurefireHelper()
    {
        throw new IllegalAccessError( "Utility class" );
    }

    public static String[] getDumpFilesToPrint()
    {
        return DUMP_FILES_PRINT.clone();
    }

    public static void reportExecution( SurefireReportParameters reportParameters, RunResult result,
                                        PluginConsoleLogger log, Exception firstForkException )
        throws MojoFailureException, MojoExecutionException
    {
        if ( firstForkException == null && !result.isTimeout() && result.isErrorFree() )
        {
            if ( result.getCompletedCount() == 0 && failIfNoTests( reportParameters ) )
            {
                throw new MojoFailureException( "No tests were executed!  "
                                                        + "(Set -DfailIfNoTests=false to ignore this error.)" );
            }
            return;
        }

        if ( reportParameters.isTestFailureIgnore() )
        {
            log.error( createErrorMessage( reportParameters, result, firstForkException ) );
        }
        else
        {
            throwException( reportParameters, result, firstForkException );
        }
    }

    public static List<CommandLineOption> commandLineOptions( MavenSession session, PluginConsoleLogger log )
    {
        List<CommandLineOption> cli = new ArrayList<CommandLineOption>();
        if ( log.isErrorEnabled() )
        {
            cli.add( LOGGING_LEVEL_ERROR );
        }

        if ( log.isWarnEnabled() )
        {
            cli.add( LOGGING_LEVEL_WARN );
        }

        if ( log.isInfoEnabled() )
        {
            cli.add( LOGGING_LEVEL_INFO );
        }

        if ( log.isDebugEnabled() )
        {
            cli.add( LOGGING_LEVEL_DEBUG );
        }

        try
        {
            Method getRequestMethod = session.getClass().getMethod( "getRequest" );
            MavenExecutionRequest request = (MavenExecutionRequest) getRequestMethod.invoke( session );

            if ( request.isShowErrors() )
            {
                cli.add( SHOW_ERRORS );
            }

            String f = getFailureBehavior( request );
            if ( f != null )
            {
                // compatible with enums Maven 3.0
                cli.add( CommandLineOption.valueOf( f.startsWith( "REACTOR_" ) ? f : "REACTOR_" + f ) );
            }
        }
        catch ( Exception e )
        {
            // don't need to log the exception that Maven 2 does not have getRequest() method in Maven Session
        }
        return unmodifiableList( cli );
    }

    public static void logDebugOrCliShowErrors( String s, PluginConsoleLogger log, Collection<CommandLineOption> cli )
    {
        if ( cli.contains( LOGGING_LEVEL_DEBUG ) )
        {
            log.debug( s );
        }
        else if ( cli.contains( SHOW_ERRORS ) )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( s );
            }
            else
            {
                log.info( s );
            }
        }
    }

    private static String getFailureBehavior( MavenExecutionRequest request )
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        try
        {
            return request.getFailureBehavior();
        }
        catch ( NoSuchMethodError e )
        {
            return (String) request.getClass()
                .getMethod( "getReactorFailureBehavior" )
                .invoke( request );
        }
    }

    private static boolean failIfNoTests( SurefireReportParameters reportParameters )
    {
        return reportParameters.getFailIfNoTests() != null && reportParameters.getFailIfNoTests();
    }

    private static boolean isFatal( Exception firstForkException )
    {
        return firstForkException != null && !( firstForkException instanceof TestSetFailedException );
    }

    private static void throwException( SurefireReportParameters reportParameters, RunResult result,
                                           Exception firstForkException )
            throws MojoFailureException, MojoExecutionException
    {
        if ( isFatal( firstForkException ) || result.isInternalError()  )
        {
            throw new MojoExecutionException( createErrorMessage( reportParameters, result, firstForkException ),
                                                    firstForkException );
        }
        else
        {
            throw new MojoFailureException( createErrorMessage( reportParameters, result, firstForkException ),
                                                  firstForkException );
        }
    }

    private static String createErrorMessage( SurefireReportParameters reportParameters, RunResult result,
                                              Exception firstForkException )
    {
        StringBuilder msg = new StringBuilder( 512 );

        if ( result.isTimeout() )
        {
            msg.append( "There was a timeout or other error in the fork" );
        }
        else
        {
            msg.append( "There are test failures.\n\nPlease refer to " )
                    .append( reportParameters.getReportsDirectory() )
                    .append( " for the individual test results." )
                    .append( '\n' )
                    .append( "Please refer to dump files (if any exist) " )
                    .append( DUMP_FILES_PRINT[0] )
                    .append( ", " )
                    .append( DUMP_FILES_PRINT[1] )
                    .append( " and " )
                    .append( DUMP_FILES_PRINT[2] )
                    .append( "." );
        }

        if ( firstForkException != null && firstForkException.getLocalizedMessage() != null )
        {
            msg.append( '\n' )
                    .append( firstForkException.getLocalizedMessage() );
        }

        if ( result.isFailure() )
        {
            msg.append( '\n' )
                    .append( result.getFailure() );
        }

        return msg.toString();
    }

}
