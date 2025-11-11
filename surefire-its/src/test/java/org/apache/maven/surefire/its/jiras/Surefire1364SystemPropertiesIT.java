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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Report XML should contain system properties of forked JVM.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public class Surefire1364SystemPropertiesIT extends SurefireJUnitIntegrationTestCase {

    @Test
    public void junit4Forked() {
        SurefireLauncher launcher = unpack("surefire-1364");
        OutputValidator validator =
                launcher.setForkJvm().forkOnce().executeTest().verifyErrorFree(2);

        validator
                .getSurefireReportsXmlFile("TEST-FirstTest.xml")
                .assertContainsText("<property name=\"forkedProp\" value=\"forkedValue1\"/>");

        validator
                .getSurefireReportsXmlFile("TEST-SecondTest.xml")
                .assertContainsText("<property name=\"forkedProp\" value=\"forkedValue1\"/>");
    }

    @Test
    public void junit4InProcess() {
        SurefireLauncher launcher = unpack("surefire-1364");
        OutputValidator validator =
                launcher.setForkJvm().forkNever().executeTest().verifyErrorFree(2);

        validator
                .getSurefireReportsXmlFile("TEST-FirstTest.xml")
                .assertContainsText("<property name=\"forkedProp\" value=\"forkedValue1\"/>");

        validator
                .getSurefireReportsXmlFile("TEST-SecondTest.xml")
                .assertContainsText("<property name=\"forkedProp\" value=\"forkedValue1\"/>");
    }

    @Test
    public void testNg() {
        SurefireLauncher launcher = unpack("surefire-1364");
        OutputValidator validator = launcher.setForkJvm()
                .activateProfile("testng")
                .forkOnce()
                .executeTest()
                .verifyErrorFree(3);

        validator
                .getSurefireReportsXmlFile("TEST-FirstTest.xml")
                .assertContainsText("<property name=\"forkedProp\" value=\"forkedValue1\"/>");
    }

    @Test
    public void testNgInProcess() {
        SurefireLauncher launcher = unpack("surefire-1364");
        OutputValidator validator = launcher.setForkJvm()
                .activateProfile("testng")
                .forkNever()
                .executeTest()
                .verifyErrorFree(3);

        validator
                .getSurefireReportsXmlFile("TEST-FirstTest.xml")
                .assertContainsText("<property name=\"forkedProp\" value=\"forkedValue1\"/>");
    }
}
