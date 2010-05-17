package org.apache.maven.plugin.surefire;

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
