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
package org.apache.maven.surefire.its;

import javax.xml.transform.Source;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CucumberIT extends SurefireJUnitIntegrationTestCase {

    @Before
    public void setUp() {
        assumeJavaVersion(17);
    }

    @Test
    public void simpleCucumberUsage() throws Exception {
        OutputValidator outputValidator = unpack("cucumber-tests")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("BUILD FAILURE")
                .verifyTextInLog("expected: <11> but was: <15>")
                .assertTestSuiteResults(3, 0, 1, 0);

        TestFile xmlReportFile = outputValidator.getSurefireReportsXmlFile("TEST-cz.fafejta.SimpleTest.xml");
        xmlReportFile.assertFileExists();

        Source source = Input.fromFile(xmlReportFile.getFile()).build();

        Iterable<Node> ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='cz.fafejta.SimpleTest']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        xmlReportFile = outputValidator.getSurefireReportsXmlFile("TEST-cz.fafejta.RunCucumberTest.xml");
        xmlReportFile.assertFileExists();

        source = Input.fromFile(xmlReportFile.getFile()).build();

        ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(2));

        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='RunCucumberTest']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(2));

        ite = new JAXPXPathEngine().selectNodes("//testcase[@name='Sum test - Valid test']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine().selectNodes("//testcase[@name='Sum test - Invalid test']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
    }
}
