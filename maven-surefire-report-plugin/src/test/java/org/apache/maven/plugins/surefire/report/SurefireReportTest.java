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
package org.apache.maven.plugins.surefire.report;

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getVariableValueFromObject;
import static org.apache.maven.plugins.surefire.report.Utils.toSystemNewLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
@SuppressWarnings("checkstyle:linelength")
@MojoTest(realRepositorySession = true)
@Basedir("/unit")
class SurefireReportTest {

    @Inject
    protected MavenProject mavenProject;

    @Test
    @InjectMojo(goal = "report", pom = "basic-surefire-report-test/plugin-config.xml")
    void testBasicSurefireReport(SurefireReport mojo) throws Exception {
        File outputDir = mojo.getReportOutputDirectory();
        boolean showSuccess = getVariableValueFromObject(mojo, "showSuccess");
        File reportsDir = getVariableValueFromObject(mojo, "reportsDirectory");
        String outputName = getVariableValueFromObject(mojo, "outputName");
        File xrefTestLocation = getVariableValueFromObject(mojo, "xrefTestLocation");
        boolean linkXRef = getVariableValueFromObject(mojo, "linkXRef");

        assertEquals(new File(mavenProject.getBasedir(), "/target/site/unit/basic-surefire-report-test"), outputDir);
        assertTrue(showSuccess);
        assertEquals(
                new File(mavenProject.getBasedir(), "basic-surefire-report-test/surefire-reports").getAbsolutePath(),
                reportsDir.getAbsolutePath());
        assertEquals("surefire", outputName);
        assertEquals(
                new File(mavenProject.getBasedir(), "/target/site/unit/basic-surefire-report-test/xref-test")
                        .getAbsolutePath(),
                xrefTestLocation.getAbsolutePath());
        assertTrue(linkXRef);

        mojo.execute();
        File report = new File(mavenProject.getBasedir(), "target/site/unit/basic-surefire-report-test/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join("\n", Files.readAllLines(report.toPath()));

        int idx = htmlContent.indexOf("images/icon_success_sml.gif");
        assertTrue(idx >= 0, "Wrong content in file: " + report);
    }

    @Test
    @InjectMojo(goal = "report", pom = "basic-surefire-report-success-false/plugin-config.xml")
    void testBasicSurefireReportIfShowSuccessIsFalse(SurefireReport mojo) throws Exception {
        boolean showSuccess = getVariableValueFromObject(mojo, "showSuccess");
        assertFalse(showSuccess);
        mojo.execute();
        File report = new File(
                mavenProject.getBasedir(), "target/site/unit/basic-surefire-report-success-false/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        int idx = htmlContent.indexOf("images/icon_success_sml.gif");
        assertTrue(idx < 0);
    }

    @Test
    @InjectMojo(goal = "report", pom = "basic-surefire-report-linkxref-false/plugin-config.xml")
    void testBasicSurefireReportIfLinkXrefIsFalse(SurefireReport mojo) throws Exception {
        boolean linkXRef = getVariableValueFromObject(mojo, "linkXRef");
        assertFalse(linkXRef);
        mojo.execute();
        File report = new File(
                mavenProject.getBasedir(), "target/site/unit/basic-surefire-report-linkxref-false/surefire.html");
        assertTrue(report.exists());

        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        int idx = htmlContent.indexOf("./xref-test/com/shape/CircleTest.html#L44");
        assertEquals(-1, idx);
    }

    @Test
    @InjectMojo(goal = "report", pom = "basic-surefire-report-reporting-null/plugin-config.xml")
    void testBasicSurefireReportIfReportingIsNull(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report = new File(
                mavenProject.getBasedir(), "target/site/unit/basic-surefire-report-reporting-null/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        int idx = htmlContent.indexOf("./xref-test/com/shape/CircleTest.html#L44");
        assertTrue(idx < 0);
    }

    @SuppressWarnings("checkstyle:methodname")
    @Test
    @InjectMojo(goal = "report", pom = "basic-surefire-report-anchor-test-cases/plugin-config.xml")
    void testBasicSurefireReport_AnchorTestCases(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report = new File(
                mavenProject.getBasedir(), "target/site/unit/basic-surefire-report-anchor-test-cases/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        int idx = htmlContent.indexOf("<td><a id=\"TC_com.shape.CircleTest.testX\"></a>testX</td>");
        assertTrue(idx > 0);

        idx = htmlContent.indexOf("<td><a id=\"TC_com.shape.CircleTest.testRadius\"></a>"
                + "<a href=\"#com.shape.CircleTest.testRadius\">testRadius</a>");
        assertTrue(idx > 0);
    }

    @Test
    @InjectMojo(goal = "report", pom = "surefire-report-single-error/plugin-config.xml")
    void testSurefireReportSingleError(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report =
                new File(mavenProject.getBasedir(), "target/site/unit/surefire-report-single-error/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td>")));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td><a href=\"#surefire\">surefire</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>"
                        + "<a href=\"#surefire.MyTest\">"
                        + "<img src=\"images/icon_error_sml.gif\" />"
                        + "</a>"
                        + "</td>\n"
                        + "<td><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<pre>"
                        + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
                        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                        + "\tat surefire.MyTest.test(MyTest.java:13)\n"
                        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n"
                        + "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
                        + "\tat java.lang.reflect.Method.invoke(Method.java:606)\n"
                        + "\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n"
                        + "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n"
                        + "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n"
                        + "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n"
                        + "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n"
                        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n"
                        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n"
                        + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
                        + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
                        + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
                        + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
                        + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
                        + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:272)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:167)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:147)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:130)\n"
                        + "\tat org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:211)\n"
                        + "\tat org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:163)\n"
                        + "\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:105)\n"
                        + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)" + "</pre>")));
    }

    @Test
    @InjectMojo(goal = "report", pom = "surefire-report-nestedClass-trimStackTrace/plugin-config.xml")
    void testSurefireReportNestedClassTrimStackTrace(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report = new File(
                mavenProject.getBasedir(), "target/site/unit/surefire-report-nestedClass-trimStackTrace/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td>")));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td><a href=\"#surefire\">surefire</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>"
                        + "<a href=\"#surefire.MyTest\">"
                        + "<img src=\"images/icon_error_sml.gif\" />"
                        + "</a>"
                        + "</td>\n"
                        + "<td><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(htmlContent, containsString(">surefire.MyTest:13</div>"));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<pre>"
                        + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
                        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                        + "\tat surefire.MyTest.test(MyTest.java:13)\n"
                        + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)"
                        + "</pre>")));
    }

    @Test
    @InjectMojo(goal = "report", pom = "surefire-report-nestedClass/plugin-config.xml")
    void testSurefireReportNestedClass(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report = new File(mavenProject.getBasedir(), "target/site/unit/surefire-report-nestedClass/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td>")));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td><a href=\"#surefire\">surefire</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>"
                        + "<a href=\"#surefire.MyTest\">"
                        + "<img src=\"images/icon_error_sml.gif\" />"
                        + "</a>"
                        + "</td>\n"
                        + "<td><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(htmlContent, containsString(">surefire.MyTest:13</div>"));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<pre>"
                        + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
                        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                        + "\tat surefire.MyTest.test(MyTest.java:13)\n"
                        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                        + "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n"
                        + "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
                        + "\tat java.lang.reflect.Method.invoke(Method.java:606)\n"
                        + "\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n"
                        + "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n"
                        + "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n"
                        + "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n"
                        + "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n"
                        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n"
                        + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n"
                        + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
                        + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
                        + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
                        + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
                        + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
                        + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:272)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:167)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:147)\n"
                        + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:130)\n"
                        + "\tat org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:211)\n"
                        + "\tat org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:163)\n"
                        + "\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:105)\n"
                        + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)"
                        + "</pre>")));
    }

    @Test
    @InjectMojo(goal = "report", pom = "surefire-report-enclosed-trimStackTrace/plugin-config.xml")
    void testSurefireReportEnclosedTrimStackTrace(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report = new File(
                mavenProject.getBasedir(), "target/site/unit/surefire-report-enclosed-trimStackTrace/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td>")));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td><a href=\"#surefire\">surefire</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>"
                        + "<a href=\"#surefire.MyTest$A\">"
                        + "<img src=\"images/icon_error_sml.gif\" />"
                        + "</a>"
                        + "</td>\n"
                        + "<td><a href=\"#surefire.MyTest$A\">MyTest$A</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));

        assertThat(htmlContent, containsString(">surefire.MyTest$A:45</div>"));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<pre>"
                        + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)\n"
                        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                        + "\tat surefire.MyTest.access$200(MyTest.java:9)\n"
                        + "\tat surefire.MyTest$A.t(MyTest.java:45)\n"
                        + "</pre>")));
    }

    @Test
    @InjectMojo(goal = "report", pom = "surefire-report-enclosed/plugin-config.xml")
    void testSurefireReportEnclosed(SurefireReport mojo) throws Exception {
        mojo.execute();
        File report = new File(mavenProject.getBasedir(), "target/site/unit/surefire-report-enclosed/surefire.html");
        assertTrue(report.exists());
        String htmlContent = String.join(System.lineSeparator(), Files.readAllLines(report.toPath()));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td>")));

        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td><a href=\"#surefire\">surefire</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));
        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<tr class=\"b\">\n"
                        + "<td>"
                        + "<a href=\"#surefire.MyTest$A\">"
                        + "<img src=\"images/icon_error_sml.gif\" />"
                        + "</a>"
                        + "</td>\n"
                        + "<td><a href=\"#surefire.MyTest$A\">MyTest$A</a></td>\n"
                        + "<td>1</td>\n"
                        + "<td>1</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0</td>\n"
                        + "<td>0%</td>\n"
                        + "<td>0 s</td></tr>")));

        assertThat(htmlContent, containsString(">surefire.MyTest$A:45</div>"));

        assertThat(
                htmlContent,
                containsString(
                        toSystemNewLine("<pre>" + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                                + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
                                + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                                + "\tat surefire.MyTest.access$200(MyTest.java:9)\n"
                                + "\tat surefire.MyTest$A.t(MyTest.java:45)\n"
                                + "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                                + "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\n"
                                + "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
                                + "\tat java.lang.reflect.Method.invoke(Method.java:606)\n"
                                + "\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)\n"
                                + "\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\n"
                                + "\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)\n"
                                + "\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\n"
                                + "\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)\n"
                                + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)\n"
                                + "\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)\n"
                                + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
                                + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
                                + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
                                + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
                                + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
                                + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
                                + "\tat org.junit.runners.Suite.runChild(Suite.java:128)\n"
                                + "\tat org.junit.runners.Suite.runChild(Suite.java:27)\n"
                                + "\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)\n"
                                + "\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)\n"
                                + "\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)\n"
                                + "\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)\n"
                                + "\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)\n"
                                + "\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)\n"
                                + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.execute(JUnit4Provider.java:272)\n"
                                + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeWithRerun(JUnit4Provider.java:167)\n"
                                + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.executeTestSet(JUnit4Provider.java:147)\n"
                                + "\tat org.apache.maven.surefire.junit4.JUnit4Provider.invoke(JUnit4Provider.java:130)\n"
                                + "\tat org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:211)\n"
                                + "\tat org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:163)\n"
                                + "\tat org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:105)\n"
                                + "\tCaused by: java.lang.IndexOutOfBoundsException\n"
                                + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
                                + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                                + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                                + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                                + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)\n"
                                + "</pre>")));
    }

    @Test
    @InjectMojo(goal = "report", pom = "surefire-1183/plugin-config.xml")
    void testCustomTitleAndDescriptionReport(SurefireReport mojo) throws Exception {
        File outputDir = getVariableValueFromObject(mojo, "outputDirectory");
        String outputName = getVariableValueFromObject(mojo, "outputName");
        File reportsDir = getVariableValueFromObject(mojo, "reportsDirectory");

        assertEquals(new File(mavenProject.getBasedir(), "target/site/unit/surefire-1183"), outputDir);
        assertEquals(
                new File(mavenProject.getBasedir(), "surefire-1183/acceptancetest-reports").getAbsolutePath(),
                reportsDir.getAbsolutePath());
        assertEquals("acceptance-test", outputName);

        mojo.execute();

        File report = new File(mavenProject.getBasedir(), "target/site/unit/surefire-1183/acceptance-test.html");

        assertTrue(report.exists());

        String htmlContent = String.join("\n", Files.readAllLines(report.toPath()));
        assertThat(
                htmlContent,
                containsString(toSystemNewLine("<section><a id=\"Acceptance_Test\"></a>\n<h1>Acceptance Test</h1>")));
    }
}
