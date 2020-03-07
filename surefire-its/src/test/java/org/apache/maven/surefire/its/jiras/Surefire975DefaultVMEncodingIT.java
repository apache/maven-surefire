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
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.convertUnicodeToUTF8;

/**
 *
 */
public class Surefire975DefaultVMEncodingIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void runWithRussian1251()
        throws Exception
    {
        OutputValidator outputValidator =
            unpack( "surefire-975-wrong-encoding" ).setMavenOpts( "-Dfile.encoding=windows-1251" ).executeTest();
        outputValidator.getSurefireReportsXmlFile( "TEST-EncodingInReportTest.xml" ).assertContainsText(
                // see project.build.sourceEncoding=UTF-8 in src/test/resources/surefire-975-wrong-encoding/pom.xml
                convertUnicodeToUTF8( "\u043A\u0438\u0440\u0438\u043B\u043B\u0438\u0446\u0435" ) );
    }

}
