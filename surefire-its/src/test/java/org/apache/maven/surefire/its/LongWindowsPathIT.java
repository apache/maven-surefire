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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeTrue;

/**
 * Testing long path of base.dir where Windows CLI crashes.
 * <br>
 * Integration test for <a href="https://issues.apache.org/jira/browse/SUREFIRE-1400">SUREFIRE-1400</a>.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class LongWindowsPathIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final String PROJECT_DIR = "long-windows-path";
    private static final String LONG_PATH = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    // the IT engine crashes using long path
    private static final String LONG_DIR = LONG_PATH + LONG_PATH + LONG_PATH;

    @Test
    public void shouldRunInSystemTmp() throws Exception
    {
        assumeTrue( IS_OS_WINDOWS );

        OutputValidator validator = unpack().setForkJvm()
                                            .showErrorStackTraces()
                                            .executeTest()
                                            .verifyErrorFreeLog();

        validator.assertThatLogLine( containsString( "SUREFIRE-1400 user.dir=" ), is( 1 ) )
                .assertThatLogLine( containsString( "SUREFIRE-1400 surefire.real.class.path=" ), is( 1 ) );

        for ( String line : validator.loadLogLines() )
        {
            if ( line.contains( "SUREFIRE-1400 user.dir=" ) )
            {
                File buildDir = new File( System.getProperty( "user.dir" ), "target" );
                File itBaseDir = new File( buildDir, "LongWindowsPathIT_shouldRunInSystemTmp" );

                assertThat( line )
                        .contains( itBaseDir.getAbsolutePath() );
            }
            else if ( line.contains( "SUREFIRE-1400 surefire.real.class.path=" ) )
            {
                assertThat( line )
                        .contains( new File( System.getProperty( "java.io.tmpdir" ) ).getCanonicalPath() );
            }
        }
    }

    private SurefireLauncher unpack() throws IOException
    {
        return unpack( PROJECT_DIR/*, "_" + LONG_DIR*/ );
    }
}
