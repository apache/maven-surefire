package org.apache.maven.surefire.junit4;

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

import java.util.Properties;
import org.apache.maven.surefire.booter.BaseProviderFactory;
import org.apache.maven.surefire.testset.TestRequest;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4ProviderTest
    extends TestCase
{
    public void testCreateProvider()
    {
        BaseProviderFactory providerParameters = new BaseProviderFactory( null, Boolean.TRUE );
        providerParameters.setProviderProperties( new Properties() );
        providerParameters.setClassLoaders( this.getClass().getClassLoader() );
        providerParameters.setTestRequest( new TestRequest( null, null, null ) );
        assertNotNull( new JUnit4Provider( providerParameters ) );
    }
}
