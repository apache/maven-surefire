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
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Cucumber scenarios run by the Vintage engine must be counted on a JUnit Platform older than 1.8,
 * where {@code TestIdentifier#getParentIdObject} is not available.
 */
public class CucumberLegacyPlatformIT extends SurefireJUnit4IntegrationTestCase {

    @BeforeEach
    public void setUp() {
        assumeJavaVersion(17);
    }

    @Test
    public void scenariosAreCountedOnLegacyPlatform() {
        OutputValidator outputValidator =
                unpack("cucumber-legacy-platform").maven().executeTest().assertTestSuiteResults(2, 0, 0, 0);

        TestFile xmlReportFile =
                outputValidator.getSurefireReportsXmlFile("TEST-org.sample.cucumber.RunCucumberTest.xml");
        xmlReportFile.assertFileExists();

        Source source = Input.fromFile(xmlReportFile.getFile()).build();

        Iterable<Node> testCases = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(testCases, IsIterableWithSize.iterableWithSize(2));

        testCases = new JAXPXPathEngine()
                .selectNodes("//testcase[@classname='org.sample.cucumber.RunCucumberTest']", source);
        assertThat(testCases, IsIterableWithSize.iterableWithSize(2));
    }
}
