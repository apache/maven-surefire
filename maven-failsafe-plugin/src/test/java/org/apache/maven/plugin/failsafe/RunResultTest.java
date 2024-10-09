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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.SureFireFileManager;
import org.junit.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RunResultTest {

    @Test
    public void testAggregatedValues() {
        RunResult simple = getSimpleAggregate();

        assertThat(simple.getCompletedCount()).isEqualTo(20);

        assertThat(simple.getErrors()).isEqualTo(3);

        assertThat(simple.getFailures()).isEqualTo(7);

        assertThat(simple.getSkipped()).isEqualTo(4);

        assertThat(simple.getFlakes()).isEqualTo(2);
    }

    @Test
    public void testSerialization() throws Exception {
        writeReadCheck(getSimpleAggregate());
    }

    @Test
    public void testFailures() throws Exception {
        writeReadCheck(new RunResult(0, 1, 2, 3, "stacktraceHere", false));
    }

    @Test
    public void testSkipped() throws Exception {
        writeReadCheck(new RunResult(3, 2, 1, 0, null, true));
    }

    @Test
    public void testFlakes() throws Exception {
        writeReadCheck(new RunResult(3, 2, 1, 0, 2, null, true));
    }

    @Test
    public void testLegacyDeserialization() throws Exception {
        File legacySummary = SureFireFileManager.createTempFile("failsafe", "test");
        String legacyFailsafeSummaryXmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<failsafe-summary xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"https://maven.apache.org/surefire/maven-surefire-plugin/xsd/failsafe-summary.xsd\""
                + " result=\"%s\" timeout=\"%s\">\n"
                + "    <completed>%d</completed>\n"
                + "    <errors>%d</errors>\n"
                + "    <failures>%d</failures>\n"
                + "    <skipped>%d</skipped>\n"
                + "    %s\n"
                + "</failsafe-summary>";
        String xml = format(Locale.ROOT, legacyFailsafeSummaryXmlTemplate, 0, false, 3, 2, 1, 0, "msg");
        Files.write(
                legacySummary.toPath(),
                xml.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        // When the failsafe-summary.xml does not contain the <flakes> element, it should be considered as 0.
        RunResult expected = new RunResult(3, 2, 1, 0, 0, null, false);
        RunResult actual = FailsafeSummaryXmlUtils.toRunResult(legacySummary);

        assertThat(actual.getCompletedCount()).isEqualTo(expected.getCompletedCount());

        assertThat(actual.getErrors()).isEqualTo(expected.getErrors());

        assertThat(actual.getFailures()).isEqualTo(expected.getFailures());

        assertThat(actual.getSkipped()).isEqualTo(expected.getSkipped());

        assertThat(actual.getFlakes()).isEqualTo(expected.getFlakes());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testAppendSerialization() throws Exception {
        RunResult simpleAggregate = getSimpleAggregate();
        RunResult additional = new RunResult(2, 1, 2, 2, "msg " + ((char) 0x0E01), true);

        File summary = SureFireFileManager.createTempFile("failsafe", "test");
        FailsafeSummaryXmlUtils.writeSummary(simpleAggregate, summary, false);
        FailsafeSummaryXmlUtils.writeSummary(additional, summary, true);
        RunResult actual = FailsafeSummaryXmlUtils.toRunResult(summary);
        //noinspection ResultOfMethodCallIgnored
        summary.delete();

        RunResult expected = simpleAggregate.aggregate(additional);

        assertThat(expected.getCompletedCount()).isEqualTo(22);

        assertThat(expected.getErrors()).isEqualTo(4);

        assertThat(expected.getFailures()).isEqualTo(9);

        assertThat(expected.getSkipped()).isEqualTo(6);

        assertThat(expected.getFlakes()).isEqualTo(2);

        assertThat(expected.getFailure()).isEqualTo("msg " + ((char) 0x0E01));

        assertThat(expected.isTimeout()).isTrue();

        assertThat(actual).isEqualTo(expected);
    }

    private void writeReadCheck(RunResult expected) throws Exception {
        File tmp = SureFireFileManager.createTempFile("test", "xml");
        FailsafeSummaryXmlUtils.fromRunResultToFile(expected, tmp);

        RunResult actual = FailsafeSummaryXmlUtils.toRunResult(tmp);
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();

        assertThat(actual).isEqualTo(expected);
    }

    private RunResult getSimpleAggregate() {
        RunResult resultOne = new RunResult(10, 1, 3, 2, 1);
        RunResult resultTwo = new RunResult(10, 2, 4, 2, 1);
        return resultOne.aggregate(resultTwo);
    }
}
