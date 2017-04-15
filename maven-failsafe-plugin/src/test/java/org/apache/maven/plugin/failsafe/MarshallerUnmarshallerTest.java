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

import org.apache.maven.plugin.failsafe.xmlsummary.FailsafeSummary;
import org.apache.maven.plugin.failsafe.xmlsummary.JAXB;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.apache.maven.plugin.failsafe.xmlsummary.ErrorType.FAILURE;
import static org.apache.maven.surefire.util.internal.StringUtils.UTF_8;
import static org.fest.assertions.Assertions.assertThat;

public class MarshallerUnmarshallerTest
{
    @Test
    public void shouldUnmarshallExistingXmlFile() throws Exception
    {
        File xml = new File( "target/test-classes/org/apache/maven/plugin/failsafe/failsafe-summary.xml" );
        FailsafeSummary summary = JAXB.unmarshal( xml, FailsafeSummary.class );

        assertThat( summary.getCompleted() )
                .isEqualTo( 7 );

        assertThat( summary.getErrors() )
                .isEqualTo( 1 );

        assertThat( summary.getFailures() )
                .isEqualTo( 2 );

        assertThat( summary.getSkipped() )
                .isEqualTo( 3 );

        assertThat( summary.getFailureMessage() )
                .contains( "There was an error in the forked processtest "
                                   + "subsystem#no method RuntimeException Hi There!"
                );

        assertThat( summary.getFailureMessage() )
                .contains( "There was an error in the forked processtest "
                                   + "subsystem#no method RuntimeException Hi There!"
                                   + "\n\tat org.apache.maven.plugin.surefire.booterclient.ForkStarter"
                                   + ".awaitResultsDone(ForkStarter.java:489)"
                );

        List<String> exceptionMessages = summary.extractExceptionMessageLines();

        assertThat( exceptionMessages )
                .hasSize( 1 );

        assertThat( exceptionMessages.get( 0 ) )
                .isEqualTo( "ExecutionException There was an error in the forked processtest "
                                    + "subsystem#no method RuntimeException Hi There!"
                );

        summary.setFailureMessage( null );

        assertThat( summary.extractExceptionMessageLines() )
                .isEmpty();
    }

    @Test
    public void shouldEqualTwoSameXml() throws Exception
    {
        File xml = new File( "target/test-classes/org/apache/maven/plugin/failsafe/failsafe-summary.xml" );
        FailsafeSummary summary1 = JAXB.unmarshal( xml, FailsafeSummary.class );
        FailsafeSummary summary2 = JAXB.unmarshal( xml, FailsafeSummary.class );

        assertThat( summary1 )
                .isEqualTo( summary2 );
    }

    @Test
    public void shouldMarshallAndUnmarshallSameXml() throws Exception
    {
        FailsafeSummary expected = new FailsafeSummary();
        expected.setResult( FAILURE );
        expected.setTimeout( true );
        expected.setCompleted( 7 );
        expected.setErrors( 1 );
        expected.setFailures( 2 );
        expected.setSkipped( 3 );
        expected.setFailureMessage( "There was an error in the forked processtest "
                                            + "subsystem#no method RuntimeException Hi There!"
                                            + "\n\tat org.apache.maven.plugin.surefire.booterclient.ForkStarter"
                                            + ".awaitResultsDone(ForkStarter.java:489)"
        );

        File xml = File.createTempFile( "failsafe-summary", ".xml" );
        String xmlText = JAXB.marshal( expected, UTF_8 );
        FileWriter fileWriter = new FileWriter( xml );
        fileWriter.write( xmlText );
        fileWriter.flush();
        fileWriter.close();

        FailsafeSummary actual = JAXB.unmarshal( xml, FailsafeSummary.class );

        assertThat( actual )
                .isEqualTo( expected );

        List<String> exceptionMessages = actual.extractExceptionMessageLines();

        assertThat( exceptionMessages )
                .hasSize( 1 );

        assertThat( exceptionMessages.get( 0 ) )
                .isEqualTo( "There was an error in the forked processtest "
                                    + "subsystem#no method RuntimeException Hi There!"
                );
    }
}
