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

import static org.apache.maven.surefire.api.booter.ProviderParameterNames.PARALLEL_PROP;
import static org.apache.maven.surefire.api.util.ReflectionUtils.invokeSetter;
import static org.apache.maven.surefire.api.util.ReflectionUtils.loadClass;

/**
 * TestNG 7.4.0 configurator. Changed setParallel type to enum value.
 * Uses reflection since ParallelMode enum doesn't exist in supported
 * TestNG 5.x versions.
 *
 * @since 3.0.0-M6
 */
public class TestNG740Configurator
    extends TestNG60Configurator
{
    /**
     * Convert and apply the value of the [parallel] setting.
     * <p>
     * <b>NOTE</b>: Since TestNG 7.4, the value of the {@code parallel} setting of the {@link XmlSuite} class has been
     * specified via a <b>ParallelMode</b> enumeration. This method converts the [parallel] setting specified in the
     * Surefire plugin configuration to its corresponding constant and applies this to the specified suite object.
     * 
     * @param suite TestNG {@link XmlSuite} object
     * @param options Surefire plugin configuration options
     * @throws TestSetFailedException if unable to convert specified [parallel] setting
     */
    @Override
    protected void configureParallel( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException
    {
        String parallel = options.get( PARALLEL_PROP );
        if ( parallel != null )
        {
            Class enumClass = loadClass( XmlSuite.class.getClassLoader(), "org.testng.xml.XmlSuite$ParallelMode" );
            try
            {
                Enum<?> parallelEnum = Enum.valueOf( enumClass, parallel.toUpperCase() );
                invokeSetter( suite, "setParallel", enumClass, parallelEnum );
            }
            catch ( IllegalArgumentException e )
            {
                throw new TestSetFailedException( "Unsupported TestNG [parallel] setting: " + parallel, e );
            }
        }
    }
}
