package org.apache.maven.surefire.booter;
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

import junit.framework.TestCase;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;

public class SurefireReflectorTest
        extends TestCase
{
    public void testShouldCreateFactoryWithoutException()
    {
        ReporterFactory factory = new ReporterFactory() {
            public RunListener createReporter() {
                return null;
            }

            public RunResult close() {
                return null;
            }
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        SurefireReflector reflector = new SurefireReflector( cl );
        BaseProviderFactory baseProviderFactory =
                (BaseProviderFactory) reflector.createBooterConfiguration( cl, factory, true );
        assertNotNull( baseProviderFactory.getReporterFactory() );
        assertSame( factory, baseProviderFactory.getReporterFactory() );
    }
}
