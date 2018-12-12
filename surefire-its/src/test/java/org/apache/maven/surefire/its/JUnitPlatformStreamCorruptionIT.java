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

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;

public class JUnitPlatformStreamCorruptionIT
        extends SurefireJUnit4IntegrationTestCase
{
    @Before
    public void setUp()
    {
        assumeJavaVersion( 1.8d );
    }

    @Test
    public void warningIsNotEmitted() throws VerificationException
    {
        OutputValidator validator = unpack( "/surefire-1614-stream-corruption" )
                .executeTest()
                .verifyErrorFree( 1 );

        List<String> lines = validator.loadLogLines(
                startsWith( "[WARNING] Corrupted STDOUT by directly writing to native stream in forked JVM" ) );

        assertThat( lines )
                .isEmpty();
    }
}
