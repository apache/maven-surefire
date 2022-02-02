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

import org.apache.maven.surefire.its.fixture.AbstractJava9PlusIT;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for <a href="https://issues.apache.org/jira/browse/SUREFIRE-1733">SUREFIRE-1733</a>.
 */
public class MultiModuleProjectWithJPMSIT extends AbstractJava9PlusIT
{
    @Test
    public void test() throws Exception
    {
        OutputValidator validator = assumeJava9()
            .debugLogging()
            .executeVerify()
            .verifyErrorFreeLog()
            .assertThatLogLine( containsString( "Lets see JDKModulePath" ), is( 2 ) )
            .assertThatLogLine( containsString( "Lets see JDKModulePath: null" ), is( 0 ) );

        List<String> lines = validator.loadLogLines( containsString( "Lets see JDKModulePath" ) );
        int i = 0;
        for ( String line : lines )
        {
            assertThat( line )
                .contains( "com.foo.api" )
                .contains( "junit-jupiter-api" )
                .contains( "junit-jupiter-engine" )
                .contains( "slf4j-simple" )
                .contains( "slf4j-api" )
                .contains( "jakarta.xml.bind-api" )
                .contains( "jakarta.ws.rs-api" )
                .contains( "jakarta.persistence-api" );

            assertThat( line )
                .contains( i++ == 0 ? "test-classes" : "com.foo.impl" );
        }
    }

    @Override
    protected String getProjectDirectoryName()
    {
        return "maven-multimodule-project-with-jpms";
    }
}
