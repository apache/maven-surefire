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

import org.apache.maven.surefire.its.fixture.AbstractJava9PlusIT;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 *
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1570">SUREFIRE-1570</a>
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire1570ModularFailsafeIT
        extends AbstractJava9PlusIT
{
    @Test
    public void shouldRunWithJupiterApi() throws Exception
    {
        assumeJava9()
            .debugLogging()
            .executeVerify()
            .verifyErrorFreeLog()
            .assertThatLogLine( containsString( "Lets see JDKModulePath" ), is( 2 ) )
            .assertThatLogLine( containsString( "Lets see JDKModulePath: null" ), is( 0 ) );
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "surefire-1570";
    }
}
