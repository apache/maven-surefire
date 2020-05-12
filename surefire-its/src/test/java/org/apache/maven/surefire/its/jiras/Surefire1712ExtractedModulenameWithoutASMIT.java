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

import org.apache.maven.surefire.its.AbstractJigsawIT;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

/**
 * See the JIRA https://issues.apache.org/jira/browse/SUREFIRE-1712
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class Surefire1712ExtractedModulenameWithoutASMIT
        extends AbstractJigsawIT
{
    @Test
    public void test()
            throws Exception
    {
        assumeJava9()
            .debugLogging()
            .executeTest()
            .assertTestSuiteResults( 1, 0, 0, 0 )
            .assertThatLogLine( containsString( "Unsupported class file major version" ), is( 0 ) )
            .assertThatLogLine( containsString( "at org.objectweb.asm.ClassReader.<init>" ), is( 0 ) )
            .verifyTextInLog( "main module descriptor name: wtf.g4s8.oot" );
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "surefire-1712-extracted-modulename-without-asm";
    }
}
