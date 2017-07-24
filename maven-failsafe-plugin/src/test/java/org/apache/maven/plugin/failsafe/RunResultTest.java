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
import org.apache.maven.surefire.suite.RunResult;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.apache.maven.surefire.util.internal.StringUtils.UTF_8;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public class RunResultTest
{

    @Test
    public void testAggregatedValues()
    {
        RunResult simple = getSimpleAggregate();

        assertThat( simple.getCompletedCount() )
                .isEqualTo( 20 );

        assertThat( simple.getErrors() )
                .isEqualTo( 3 );

        assertThat( simple.getFailures() )
                .isEqualTo( 7 );

        assertThat( simple.getSkipped() )
                .isEqualTo( 4 );

        assertThat( simple.getFlakes() )
                .isEqualTo( 2 );
    }

    @Test
    public void testSerialization()
            throws IOException, JAXBException
    {
        writeReadCheck( getSimpleAggregate() );
    }

    @Test
    public void testFailures()
            throws IOException, JAXBException
    {
        writeReadCheck( new RunResult( 0, 1, 2, 3, "stacktraceHere", false ) );
    }

    @Test
    public void testSkipped()
            throws IOException, JAXBException
    {
        writeReadCheck( new RunResult( 3, 2, 1, 0, null, true ) );
    }

    @Test
    public void testAppendSerialization()
            throws IOException, JAXBException
    {
        RunResult simpleAggregate = getSimpleAggregate();
        RunResult additional = new RunResult( 2, 1, 2, 2, "msg " + ( (char) 0x0E01 ), true );

        File summary = File.createTempFile( "failsafe", "test" );
        FailsafeSummaryXmlUtils.writeSummary( simpleAggregate, summary, false, UTF_8 );
        FailsafeSummaryXmlUtils.writeSummary( additional, summary, true, UTF_8 );
        RunResult actual = FailsafeSummaryXmlUtils.toRunResult( summary );
        //noinspection ResultOfMethodCallIgnored
        summary.delete();

        RunResult expected = simpleAggregate.aggregate( additional );

        assertThat( expected.getCompletedCount() )
                .isEqualTo( 22 );

        assertThat( expected.getErrors() )
                .isEqualTo( 4 );

        assertThat( expected.getFailures() )
                .isEqualTo( 9 );

        assertThat( expected.getSkipped() )
                .isEqualTo( 6 );

        assertThat( expected.getFlakes() )
                .isEqualTo( 2 );

        assertThat( expected.getFailure() )
                .isEqualTo( "msg " + ( (char) 0x0E01 ) );

        assertThat( expected.isTimeout() )
                .isTrue();

        assertThat( actual )
                .isEqualTo( expected );
    }

    @Test
    public void shouldAcceptAliasCharset()
    {
        Charset charset1 = IntegrationTestMojo.toCharset( "UTF8" );
        assertThat( charset1.name() ).isEqualTo( "UTF-8" );

        Charset charset2 = IntegrationTestMojo.toCharset( "utf8" );
        assertThat( charset2.name() ).isEqualTo( "UTF-8" );
    }

    private void writeReadCheck( RunResult expected )
            throws IOException, JAXBException
    {
        File tmp = File.createTempFile( "test", "xml" );
        FailsafeSummaryXmlUtils.fromRunResultToFile( expected, tmp );

        RunResult actual = FailsafeSummaryXmlUtils.toRunResult( tmp );
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();

        assertThat( actual )
                .isEqualTo( expected );
    }

    private RunResult getSimpleAggregate()
    {
        RunResult resultOne = new RunResult( 10, 1, 3, 2, 1 );
        RunResult resultTwo = new RunResult( 10, 2, 4, 2, 1 );
        return resultOne.aggregate( resultTwo );
    }
}
