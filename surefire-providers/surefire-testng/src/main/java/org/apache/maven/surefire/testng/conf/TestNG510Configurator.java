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

/**
 * TestNG 5.10 configurator. Added support of dataproviderthreadcount.
 *
 * @since 2.19
 */
public class TestNG510Configurator
    extends TestNGMapConfigurator
{

    @Override
    public void configure( XmlSuite suite, Map<String, String> options )
        throws TestSetFailedException
    {
        super.configure( suite, options );

        String dataProviderThreadCount = options.get( "dataproviderthreadcount" );
        if ( dataProviderThreadCount != null )
        {
            suite.setDataProviderThreadCount( Integer.parseInt( dataProviderThreadCount ) );
        }
    }
}
