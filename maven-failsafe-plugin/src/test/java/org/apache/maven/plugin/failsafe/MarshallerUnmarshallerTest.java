/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.failsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.SureFireFileManager;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 */
class MarshallerUnmarshallerTest {
    @Test
    void shouldUnmarshallExistingXmlFile() throws Exception {
        File xml = new File("target/test-classes/org/apache/maven/plugin/failsafe/failsafe-summary.xml");
        RunResult summary = FailsafeSummaryXmlUtils.toRunResult(xml);

        assertThat(summary.getCompletedCount()).isEqualTo(7);

        assertThat(summary.getErrors()).isEqualTo(1);

        assertThat(summary.getFailures()).isEqualTo(2);

        assertThat(summary.getSkipped()).isEqualTo(3);

        assertThat(summary.getFailure())
                .contains("There was an error in the forked processtest "
                        + "subsystem#no method RuntimeException Hi There!");

        assertThat(summary.getFailure())
                .contains("There was an error in the forked processtest "
                        + "subsystem#no method RuntimeException Hi There! $&>>"
                        + "\n\tat org.apache.maven.plugin.surefire.booterclient.ForkStarter"
                        + ".awaitResultsDone(ForkStarter.java:489)");
    }

    @Test
    void shouldMarshallAndUnmarshallSameXml() throws Exception {
        RunResult expected = new RunResult(
                7,
                1,
                2,
                3,
                2,
                "There was an error in the forked processtest "
                        + "subsystem#no method RuntimeException Hi There! $&>>"
                        + "\n\tat org.apache.maven.plugin.surefire.booterclient.ForkStarter"
                        + ".awaitResultsDone(ForkStarter.java:489)",
                true);

        File xml = SureFireFileManager.createTempFile("failsafe-summary", ".xml");
        FailsafeSummaryXmlUtils.writeSummary(expected, xml, false);

        RunResult actual = FailsafeSummaryXmlUtils.toRunResult(xml);

        assertThat(actual.getFailures()).isEqualTo(expected.getFailures());

        assertThat(actual.isTimeout()).isEqualTo(expected.isTimeout());

        assertThat(actual.getCompletedCount()).isEqualTo(expected.getCompletedCount());

        assertThat(actual.getErrors()).isEqualTo(expected.getErrors());

        assertThat(actual.getFailures()).isEqualTo(expected.getFailures());

        assertThat(actual.getSkipped()).isEqualTo(expected.getSkipped());

        assertThat(actual.getFailure()).isEqualTo(expected.getFailure());
    }

    @Test
    void shouldRejectSummaryWithDoctype() throws Exception {
        File secret = SureFireFileManager.createTempFile("failsafe-xxe-secret", ".txt");
        Files.write(secret.toPath(), "SENSITIVE_MARKER".getBytes(UTF_8));
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE failsafe-summary [<!ENTITY xxe SYSTEM \""
                + secret.toURI()
                + "\">]><failsafe-summary timeout=\"false\"><completed>1</completed>"
                + "<errors>0</errors><failures>0</failures><skipped>0</skipped>"
                + "<failureMessage>&xxe;</failureMessage><flakes>0</flakes></failsafe-summary>";

        File xmlFile = SureFireFileManager.createTempFile("failsafe-summary-doctype", ".xml");
        Files.write(xmlFile.toPath(), xml.getBytes(UTF_8));

        assertThrows(IOException.class, () -> FailsafeSummaryXmlUtils.toRunResult(xmlFile));
    }
}
