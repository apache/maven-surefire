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

import java.io.IOException;

@SuppressWarnings( { "javadoc", "checkstyle:javadoctype" } )
/**
 * IsolatedClassLoader should take platform ClassLoader as a parent ClassLoader if running on the top of JDK9.
 * The IsolatedClassLoader should not fail like this:
 *
 * [ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.19.1:test (default-test) on project
 * maven-surefire-plugin-example: Execution default-test of goal
 * org.apache.maven.plugins:maven-surefire-plugin:2.19.1:test failed:
 * java.lang.NoClassDefFoundError: java/sql/SQLException: java.sql.SQLException -> [Help 1]
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1265">SUREFIRE-1265</a>
 * @since 2.20.1
 */
public class Surefire1265Java9IT
        extends AbstractJigsawIT
{
    @Test
    public void shouldRunInPluginJava9() throws IOException
    {
        assumeJava9()
                .executeTest()
                .verifyErrorFree( 2 );
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "/surefire-1265";
    }
}
