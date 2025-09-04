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
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ArchUnitIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void simpleCucumberUsage() throws Exception {
        OutputValidator outputValidator = unpack("archunit")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("BUILD FAILURE")
                .verifyTextInLog("classes that have simple name ending with 'DTO'")
                .assertTestSuiteResults(1, 0, 1, 0);

        TestFile xmlReportFile = outputValidator.getSurefireReportsXmlFile("TEST-custom.ArchUnitTest.xml");
        xmlReportFile.assertFileExists();

        Source source = Input.fromFile(xmlReportFile.getFile()).build();

        Iterable<Node> ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='ArchUnitTest']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine().selectNodes("//testcase[@name='DTO_IN_PACKAGE_DTO']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
    }
}
