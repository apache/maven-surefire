package org.apache.maven.surefire.junitcore;

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


import org.apache.maven.surefire.providerapi.BaseProviderFactory;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.junit.runner.JUnitCore;

import java.util.Properties;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnitCoreProviderFactory
    extends BaseProviderFactory
    implements ProviderPropertiesAware
{
    Properties providerProperties;

    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
    }

    public SurefireProvider createProvider()
    {
        return new JUnitCoreProvider( providerProperties, getReporterManagerFactory(), getTestClassLoader(),
                                      getDirectoryScanner() );
    }
}
