package org.apache.maven.surefire.testprovider;

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

/**
 * @author Kristian Rosenvold
 */
public class TestProvider
    extends AbstractProvider
{

    public TestProvider( ProviderParameters booterParameters )
    {
        invokeRuntimeExceptionIfSet( System.getProperty( "constructorCrash" ) );
    }

    public Iterable<Class<?>> getSuites()
    {
        invokeRuntimeExceptionIfSet( System.getProperty( "getSuitesCrash" ) );
        return null;
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        throwIfSet( System.getProperty( "invokeCrash" ) );
        return new RunResult( 1, 0, 0, 2 );
    }

    private void throwIfSet( String throwError )
        throws TestSetFailedException, ReporterException
    {
        if ( "testSetFailed".equals( throwError ) )
        {
            throw new TestSetFailedException( "Let's fail" );
        }
        if ( "reporterException".equals( throwError ) )
        {
            throw new ReporterException( "Let's fail with a reporterexception", new RuntimeException() );
        }

        invokeRuntimeExceptionIfSet( throwError );
    }

    private void invokeRuntimeExceptionIfSet( String throwError )
    {
        if ( "runtimeException".equals( throwError ) )
        {
            throw new RuntimeException( "Let's fail with a runtimeException" );
        }
    }
}
