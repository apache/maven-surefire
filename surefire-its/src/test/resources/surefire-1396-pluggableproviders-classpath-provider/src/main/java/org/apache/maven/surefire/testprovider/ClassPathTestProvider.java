package org.apache.maven.surefire.testprovider;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map.Entry;

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;

/**
 * @author Jonathan Bell
 */
public class ClassPathTestProvider
    extends AbstractProvider
{
    boolean hasSLF4J; // SLF4J is not being included in our deps, so if it's in the classpath, that's a problem...

    public ClassPathTestProvider( ProviderParameters params )
    {
        for ( Entry<String, String> propEntry : params.getProviderProperties().entrySet() )
        {
            if ( propEntry.getKey().startsWith( "surefireClassPathUrl" ) && propEntry.getValue().contains( "slf4j" ) )
            {
                hasSLF4J = true;
            }
        }
    }

    public Iterable<Class<?>> getSuites()
    {
        return Collections.<Class<?>>emptySet();
    }

    public RunResult invoke( Object arg0 )
        throws TestSetFailedException, ReporterException, InvocationTargetException
    {
        if ( hasSLF4J )
        {
            throw new TestSetFailedException( "SLF4J was found on the boot classpath" );
        }
        return new RunResult( 1, 0, 0, 0 );
    }

}
