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
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Cucumber via JUnit 4 + vintage on JUnit Platform 1.7.2.
 * {@code TestIdentifier#getParentIdObject()} is absent there, so parent walks must
 * use {@code TestPlan#getParent} or scenarios are reported as {@code Tests run: 0}
 * (issue #3428). The modern {@link CucumberIT} stays on Platform 1.13 and cannot
 * show that regression.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CucumberJUnit4VintageIT extends SurefireJUnit4IntegrationTestCase {

    @Before
    public void setUp() {
        assumeJavaVersion(17);
    }

    @Test
    public void cucumberScenariosCountedOnPlatform17() throws Exception {
        OutputValidator outputValidator = unpack("cucumber-junit4-vintage-platform-1.7")
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults(1, 0, 0, 0);

        TestFile xmlReportFile = outputValidator.getSurefireReportsXmlFile("TEST-org.sample.cucumber.CucumberTest.xml");
        xmlReportFile.assertFileExists();

        Source source = Input.fromFile(xmlReportFile.getFile()).build();

        Iterable<Node> ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='org.sample.cucumber.CucumberTest']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
    }
}
