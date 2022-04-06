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
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;

/**
 *
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class JUnitPlatformStreamCorruptionIT
        extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void warningIsNotEmitted() throws VerificationException
    {
        OutputValidator validator = unpack( "/surefire-1614-stream-corruption" )
                .executeTest()
                .verifyErrorFree( 1 );

        List<String> lines = validator.loadLogLines(
                startsWith( "[WARNING] Corrupted channel by directly writing to native stream in forked JVM" ) );

        assertThat( lines )
                .isEmpty();
    }

    @Test
    public void warningIsNotEmittedWithJulToSlf4j() throws VerificationException
    {
        OutputValidator validator = unpack( "/surefire-1659-stream-corruption" )
                .activateProfile( "junit-platform-with-jul-to-slf4j" )
                .executeTest()
                .verifyErrorFree( 1 );

        List<String> lines = validator.loadLogLines(
                startsWith( "[WARNING] Corrupted channel by directly writing to native stream in forked JVM" ) );

        assertThat( lines )
                .isEmpty();
    }

    @Test
    @Ignore( "https://issues.apache.org/jira/browse/SUREFIRE-1659?focusedCommentId=17374005&page="
                 + "com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-17374005" )
    public void warningIsNotEmittedWithJulToLog4j() throws VerificationException
    {
        OutputValidator validator = unpack( "/surefire-1659-stream-corruption" )
                .activateProfile( "junit-platform-with-jul-to-log4j" )
                .executeTest()
                .verifyErrorFree( 1 );

        List<String> lines = validator.loadLogLines(
                startsWith( "[WARNING] Corrupted channel by directly writing to native stream in forked JVM" ) );

        assertThat( lines )
                .isEmpty();
    }
}
