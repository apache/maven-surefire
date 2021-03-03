package org.apache.maven.surefire.testng.conf;

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

import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.testng.xml.XmlSuite;

import java.util.Map;

import static java.lang.Integer.parseInt;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.PARALLEL_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.THREADCOUNT_PROP;
import static org.apache.maven.surefire.api.util.ReflectionUtils.invokeSetter;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryLoadClass;

/**
 * TestNG 7.4.0 configurator. Changed setParallel type to enum value.
 * Uses reflection since ParallelMode enum doesn't exist in supported
 * TestNG 5.x versions.
 *
 * @since 3.0.0-M6
 */
public class TestNG740Configurator extends TestNG60Configurator
{
    @Override
    public void configure( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException
    {
        String threadCountAsString = options.get( THREADCOUNT_PROP );
        int threadCount = threadCountAsString == null ? 1 : parseInt( threadCountAsString );
        suite.setThreadCount( threadCount );

        String parallel = options.get( PARALLEL_PROP );
        if ( parallel != null )
        {
            if ( !"methods".equalsIgnoreCase( parallel ) && !"classes".equalsIgnoreCase( parallel ) )
            {
                throw new TestSetFailedException( "Unsupported TestNG parallel setting: "
                    + parallel + " ( only METHODS or CLASSES supported )" );
            }
            Class enumClass = tryLoadClass( XmlSuite.class.getClassLoader(), "org.testng.xml.XmlSuite$ParallelMode" );
            Enum<?> parallelEnum = Enum.valueOf( enumClass, parallel.toUpperCase() );
            invokeSetter( suite, "setParallel", enumClass, parallelEnum );
        }

        String dataProviderThreadCount = options.get( "dataproviderthreadcount" );
        if ( dataProviderThreadCount != null )
        {
            suite.setDataProviderThreadCount( Integer.parseInt( dataProviderThreadCount ) );
        }
    }
}
