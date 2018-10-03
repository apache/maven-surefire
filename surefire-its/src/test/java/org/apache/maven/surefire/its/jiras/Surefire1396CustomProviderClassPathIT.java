package org.apache.maven.surefire.its.jiras;

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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jonathan Bell
 */
public class Surefire1396CustomProviderClassPathIT
    extends SurefireJUnit4IntegrationTestCase
{
    @BeforeClass
    public static void installProvider()
    {
        unpack( Surefire1396CustomProviderClassPathIT.class, "surefire-1396-pluggableproviders-classpath-provider", "prov" ).executeInstall();
    }
    
    @Test
    public void pluggableProviderClasspathCorrect()
    {
        unpack( "surefire-1396-pluggableproviders-classpath" )
            .setForkJvm()
            .maven()
            .showExceptionMessages()
            .debugLogging()
            .executeVerify()
            .verifyErrorFreeLog();
    }
}
