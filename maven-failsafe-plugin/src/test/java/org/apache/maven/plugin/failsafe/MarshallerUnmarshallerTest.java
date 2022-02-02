package org.apache.maven.plugin.failsafe;

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

import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.surefire.api.suite.RunResult;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class MarshallerUnmarshallerTest
{
    @Test
    public void shouldUnmarshallExistingXmlFile() throws Exception
    {
        File xml = new File( "target/test-classes/org/apache/maven/plugin/failsafe/failsafe-summary.xml" );
        RunResult summary = FailsafeSummaryXmlUtils.toRunResult( xml );

        assertThat( summary.getCompletedCount() )
                .isEqualTo( 7 );

        assertThat( summary.getErrors() )
                .isEqualTo( 1 );

        assertThat( summary.getFailures() )
                .isEqualTo( 2 );

        assertThat( summary.getSkipped() )
                .isEqualTo( 3 );

        assertThat( summary.getFailure() )
                .contains( "There was an error in the forked processtest "
                                   + "subsystem#no method RuntimeException Hi There!"
                );

        assertThat( summary.getFailure() )
                .contains( "There was an error in the forked processtest "
                                   + "subsystem#no method RuntimeException Hi There! $&>>"
                                   + "\n\tat org.apache.maven.plugin.surefire.booterclient.ForkStarter"
                                   + ".awaitResultsDone(ForkStarter.java:489)"
                );
    }

    @Test
    public void shouldMarshallAndUnmarshallSameXml() throws Exception
    {
        RunResult expected =
                new RunResult( 7, 1, 2, 3, 2,
                                     "There was an error in the forked processtest "
                                             + "subsystem#no method RuntimeException Hi There! $&>>"
                                             + "\n\tat org.apache.maven.plugin.surefire.booterclient.ForkStarter"
                                             + ".awaitResultsDone(ForkStarter.java:489)", true );

        File xml = File.createTempFile( "failsafe-summary", ".xml" );
        FailsafeSummaryXmlUtils.writeSummary( expected, xml, false );

        RunResult actual = FailsafeSummaryXmlUtils.toRunResult( xml );

        assertThat( actual.getFailures() )
                .isEqualTo( expected.getFailures() );

        assertThat( actual.isTimeout() )
                .isEqualTo( expected.isTimeout() );

        assertThat( actual.getCompletedCount() )
                .isEqualTo( expected.getCompletedCount() );

        assertThat( actual.getErrors() )
                .isEqualTo( expected.getErrors() );

        assertThat( actual.getFailures() )
                .isEqualTo( expected.getFailures() );

        assertThat( actual.getSkipped() )
                .isEqualTo( expected.getSkipped() );

        assertThat( actual.getFailure() )
                .isEqualTo( expected.getFailure() );
    }
}
