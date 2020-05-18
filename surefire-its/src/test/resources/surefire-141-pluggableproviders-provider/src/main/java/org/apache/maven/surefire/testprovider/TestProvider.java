package org.apache.maven.surefire.testprovider;

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
