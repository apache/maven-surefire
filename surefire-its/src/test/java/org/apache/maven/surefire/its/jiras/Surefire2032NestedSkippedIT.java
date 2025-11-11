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
package org.apache.maven.surefire.its.jiras;

import javax.xml.transform.Source;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration Test for SUREFIRE-2032
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire2032NestedSkippedIT extends SurefireJUnitIntegrationTestCase {
    @Test
    public void testXmlReport() {
        OutputValidator validator =
                unpack("surefire-2032-nested-test-class-skipped").executeTest().assertTestSuiteResults(4, 0, 0, 2);

        TestFile xmlReportFile = validator.getSurefireReportsXmlFile("TEST-jira2032.DisabledNestedTest.xml");
        xmlReportFile.assertFileExists();

        Source source = Input.fromFile(xmlReportFile.getFile()).build();

        Iterable<Node> ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        MatcherAssert.assertThat(ite, IsIterableWithSize.iterableWithSize(4));

        ite = new JAXPXPathEngine()
                .selectNodes(
                        "//testcase[@name='test1' and @classname='jira2032.DisabledNestedTest$OrangeTaggedDisabledTest']",
                        source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine()
                .selectNodes(
                        "//testcase[@name='test2' and @classname='jira2032.DisabledNestedTest$OrangeTaggedDisabledTest']",
                        source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine()
                .selectNodes(
                        "//testcase[@name='test1' and @classname='jira2032.DisabledNestedTest$OrangeTaggedDisabledTest']/skipped[@message='class jira2032.DisabledNestedTest$OrangeTaggedDisabledTest is @Disabled']",
                        source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine()
                .selectNodes(
                        "//testcase[@name='test2' and @classname='jira2032.DisabledNestedTest$OrangeTaggedDisabledTest']/skipped[@message='class jira2032.DisabledNestedTest$OrangeTaggedDisabledTest is @Disabled']",
                        source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine()
                .selectNodes(
                        "//testcase[@name='test1' and @classname='jira2032.DisabledNestedTest$RedTaggedEnabledTest']",
                        source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        ite = new JAXPXPathEngine()
                .selectNodes(
                        "//testcase[@name='test2' and @classname='jira2032.DisabledNestedTest$RedTaggedEnabledTest']",
                        source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
    }
}
