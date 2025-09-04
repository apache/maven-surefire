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
public class Surefire2298IT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void shouldNotCreateFilesForNested() throws Exception {
        OutputValidator outputValidator = unpack("surefire-2298-nested-class")
                .maven()
                .executeTest()
                .verifyTextInLog("BUILD SUCCESS")
                .assertTestSuiteResults(4, 0, 0, 0);

        outputValidator
                .getSurefireReportsXmlFile("io.olamy.BaseNestedTest$Inner.txt")
                .assertFileNotExists();

        TestFile xmlReportFile = outputValidator.getSurefireReportsXmlFile("TEST-io.olamy.FirstNestedTest.xml");
        xmlReportFile.assertFileExists();

        /**
         * <testsuite version="3.0.2" name="FirstNestedTest" time="0.019" tests="1" errors="0" skipped="0" failures="0">
         * <testcase name="outerTest" classname="FirstNestedTest" time="0.007"/>
         * <testcase name="innerTest" classname="BaseNestedTest$Inner" time="0.001"/>
         * </testsuite>
         **/
        Source source = Input.fromFile(xmlReportFile.getFile()).build();
        Iterable<Node> ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(2));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='io.olamy.FirstNestedTest']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='io.olamy.BaseNestedTest$Inner']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));

        xmlReportFile = outputValidator.getSurefireReportsXmlFile("TEST-io.olamy.SecondNestedTest.xml");
        /**
         * <testsuite  name="SecondNestedTest" time="0.003" tests="1" errors="0" skipped="0" failures="0">
         * <testcase name="outerTest" classname="SecondNestedTest" time="0.001"/>
         * <testcase name="innerTest" classname="BaseNestedTest$Inner" time="0.0"/>
         * </testsuite>
         */
        source = Input.fromFile(xmlReportFile.getFile()).build();

        ite = new JAXPXPathEngine().selectNodes("//testcase", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(2));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='io.olamy.SecondNestedTest']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
        ite = new JAXPXPathEngine().selectNodes("//testcase[@classname='io.olamy.BaseNestedTest$Inner']", source);
        assertThat(ite, IsIterableWithSize.iterableWithSize(1));
    }
}
