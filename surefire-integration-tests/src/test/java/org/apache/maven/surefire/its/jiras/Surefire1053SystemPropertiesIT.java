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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1053">SUREFIRE-1053</a>
 * @since 2.18
 */
public class Surefire1053SystemPropertiesIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void checkWarningsFileEncoding()
    {
        unpack().sysProp( "file.encoding", "ISO-8859-1" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "file.encoding cannot be set as system property, use <argLine>-D"
                                  + "file.encoding=...</argLine> instead" );
    }
    @Test
    public void checkWarningsSysPropTwice() throws Exception
    {
        OutputValidator validator = unpack()
            .argLine( "-DmyArg=myVal2 -Dfile.encoding=ISO-8859-1" )
            .sysProp( "file.encoding", "ISO-8859-1" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "The system property myArg is configured twice! "
                                  + "The property appears in <argLine/> and any of <systemPropertyVariables/>, "
                                  + "<systemProperties/> or user property." );

        for ( String line : validator.loadLogLines() )
        {
            assertFalse( "no warning for file.encoding not in argLine",
                         line.contains( "file.encoding cannot be set as system property, use <argLine>" ) );
            assertFalse( "no warning for double definition of file.encoding",
                         line.contains( "The system property file.encoding is configured twice!" ) );
        }

    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1053-system-properties" );
    }
}
