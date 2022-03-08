package org.apache.maven.surefire.its;

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

import java.io.IOException;

import org.apache.maven.surefire.its.fixture.AbstractJava9PlusIT;
import org.junit.Test;

/**
 *
 */
public class Junit5ModulePathIT
        extends AbstractJava9PlusIT
{
    private String suffix;

    @Test
    public void testModulePath()
            throws IOException
    {
        assumeJava9()
                .debugLogging()
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults( 2 );
    }

    @Test
    public void testModulePathWithSpaces()
            throws IOException
    {
        suffix = " with spaces";
        assumeJava9()
                .debugLogging()
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults( 2 );
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "junit5-modulepath";
    }

    @Override
    protected String getSuffix()
    {
        return suffix;
    }
}
