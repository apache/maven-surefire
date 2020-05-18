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
