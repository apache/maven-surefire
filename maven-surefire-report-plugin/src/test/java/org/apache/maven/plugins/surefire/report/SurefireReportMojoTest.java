package org.apache.maven.plugins.surefire.report;

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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;

import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.utils.WriterFactory;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.apache.maven.plugins.surefire.report.Utils.toSystemNewLine;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class SurefireReportMojoTest
    extends AbstractMojoTestCase
{
    private Renderer renderer;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        renderer = (Renderer) lookup( Renderer.ROLE );
    }

    public void testBasicSurefireReport()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "basic-surefire-report-test/plugin-config.xml" );

        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        assertNotNull( mojo );

        File outputDir = (File) getVariableValueFromObject( mojo, "outputDirectory" );

        boolean showSuccess = (Boolean) getVariableValueFromObject( mojo, "showSuccess" );

        File reportsDir = (File) getVariableValueFromObject( mojo, "reportsDirectory" );

        String outputName = (String) getVariableValueFromObject( mojo, "outputName" );

        File xrefLocation = (File) getVariableValueFromObject( mojo, "xrefLocation" );

        boolean linkXRef = (Boolean) getVariableValueFromObject( mojo, "linkXRef" );

        assertEquals( new File( getBasedir() + "/target/site/unit/basic-surefire-report-test" ), outputDir );

        assertTrue( showSuccess );

        assertEquals( new File(
            getBasedir() + "/src/test/resources/unit/basic-surefire-report-test/surefire-reports" ).getAbsolutePath(),
                      reportsDir.getAbsolutePath() );

        assertEquals( "surefire-report", outputName );
        assertEquals(
            new File( getBasedir() + "/target/site/unit/basic-surefire-report-test/xref-test" ).getAbsolutePath(),
            xrefLocation.getAbsolutePath() );

        assertTrue( linkXRef );

        mojo.execute();

        File report = new File( getBasedir(), "target/site/unit/basic-surefire-report-test/surefire-report.html" );

        renderer( mojo, report );

        assertTrue( report.exists() );

        String htmlContent = FileUtils.fileRead( report );

        int idx = htmlContent.indexOf( "images/icon_success_sml.gif" );

        assertTrue( idx >= 0 );
    }

    private File getUnitBaseDir()
        throws UnsupportedEncodingException
    {
        URL resource = getClass().getResource( "/unit" );
        // URLDecoder.decode necessary for JDK 1.5+, where spaces are escaped to %20
        return new File( URLDecoder.decode( resource.getPath(), "UTF-8" ) ).getAbsoluteFile();
    }

    public void testBasicSurefireReportIfShowSuccessIsFalse()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "basic-surefire-report-success-false/plugin-config.xml" );

        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        assertNotNull( mojo );

        boolean showSuccess = (Boolean) getVariableValueFromObject( mojo, "showSuccess" );

        assertFalse( showSuccess );

        mojo.execute();

        File report =
            new File( getBasedir(), "target/site/unit/basic-surefire-report-success-false/surefire-report.html" );

        renderer( mojo, report );

        assertTrue( report.exists() );

        String htmlContent = FileUtils.fileRead( report );

        int idx = htmlContent.indexOf( "images/icon_success_sml.gif" );

        assertTrue( idx < 0 );
    }

    public void testBasicSurefireReportIfLinkXrefIsFalse()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "basic-surefire-report-linkxref-false/plugin-config.xml" );

        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        assertNotNull( mojo );

        boolean linkXRef = (Boolean) getVariableValueFromObject( mojo, "linkXRef" );

        assertFalse( linkXRef );

        mojo.execute();

        File report =
            new File( getBasedir(), "target/site/unit/basic-surefire-report-success-false/surefire-report.html" );

        renderer( mojo, report );

        assertTrue( report.exists() );

        String htmlContent = FileUtils.fileRead( report );

        int idx = htmlContent.indexOf( "./xref-test/com/shape/CircleTest.html#44" );

        assertTrue( idx == -1 );
    }

    public void testBasicSurefireReportIfReportingIsNull()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "basic-surefire-report-reporting-null/plugin-config.xml" );

        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        assertNotNull( mojo );

        mojo.execute();

        File report =
            new File( getBasedir(), "target/site/unit/basic-surefire-report-reporting-null/surefire-report.html" );

        renderer( mojo, report );

        assertTrue( report.exists() );

        String htmlContent = FileUtils.fileRead( report );

        int idx = htmlContent.indexOf( "./xref-test/com/shape/CircleTest.html#44" );

        assertTrue( idx < 0 );
    }
    
    public void testBasicSurefireReport_AnchorTestCases()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "basic-surefire-report-anchor-test-cases/plugin-config.xml" );

        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        assertNotNull( mojo );

        mojo.execute();

        File report = new File( getBasedir(),
                                "target/site/unit/basic-surefire-report-anchor-test-cases/surefire-report.html" );

        renderer( mojo, report );

        assertTrue( report.exists() );

        String htmlContent = FileUtils.fileRead( report );

        int idx = htmlContent.indexOf( "<td><a name=\"TC_com.shape.CircleTest.testX\"></a>testX</td>" );
        assertTrue( idx > 0 );

        idx = htmlContent.indexOf( "<td><a name=\"TC_com.shape.CircleTest.testRadius\"></a>"
                                       + "<a href=\"#com.shape.CircleTest.testRadius\">testRadius</a>" );
        assertTrue( idx > 0 );
    }

    public void testSurefireReportSingleError()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "surefire-report-single-error/plugin-config.xml" );
        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );
        assertNotNull( mojo );
        mojo.execute();
        File report = new File( getBasedir(), "target/site/unit/surefire-report-single-error/surefire-report.html" );
        renderer( mojo, report );
        assertTrue( report.exists() );
        String htmlContent = FileUtils.fileRead( report );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td>" ) ) );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td><a href=\"#surefire\">surefire</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>"
                                                         + "<a href=\"#surefire.MyTest\">"
                                                         + "<img src=\"images/icon_error_sml.gif\" alt=\"\" />"
                                                         + "</a>"
                                                         + "</td>\n"
                                                         + "<td><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );

        assertThat( htmlContent, containsString( ">surefire.MyTest:13</a>" ) );

        assertThat( htmlContent, containsString( "./xref-test/surefire/MyTest.html#13" ) );

        assertThat( htmlContent, containsString( toSystemNewLine( "<pre>"
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
        + "\tat surefire.MyTest.failure(MyTest.java:33)\n" + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)" + "</pre>" ) ) );
    }

    public void testSurefireReportNestedClassTrimStackTrace()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "surefire-report-nestedClass-trimStackTrace/plugin-config.xml" );
        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );
        assertNotNull( mojo );
        mojo.execute();
        File report = new File( getBasedir(), "target/site/unit/surefire-report-nestedClass-trimStackTrace/surefire-report.html" );
        renderer( mojo, report );
        assertTrue( report.exists() );
        String htmlContent = FileUtils.fileRead( report );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td>" ) ) );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                        + "<td><a href=\"#surefire\">surefire</a></td>\n"
                                        + "<td>1</td>\n"
                                        + "<td>1</td>\n"
                                        + "<td>0</td>\n"
                                        + "<td>0</td>\n"
                                        + "<td>0%</td>\n"
                                        + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>"
                                                         + "<a href=\"#surefire.MyTest\">"
                                                         + "<img src=\"images/icon_error_sml.gif\" alt=\"\" />"
                                                         + "</a>"
                                                         + "</td>\n"
                                                         + "<td><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( ">surefire.MyTest:13</a>" ) );

        assertThat( htmlContent, containsString( "./xref-test/surefire/MyTest.html#13" ) );

        assertThat( htmlContent, containsString( toSystemNewLine( "<pre>"
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
        + "</pre>" ) ) );
    }

    public void testSurefireReportNestedClass()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "surefire-report-nestedClass/plugin-config.xml" );
        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );
        assertNotNull( mojo );
        mojo.execute();
        File report = new File( getBasedir(), "target/site/unit/surefire-report-nestedClass/surefire-report.html" );
        renderer( mojo, report );
        assertTrue( report.exists() );
        String htmlContent = FileUtils.fileRead( report );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td>" ) ) );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td><a href=\"#surefire\">surefire</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>"
                                                         + "<a href=\"#surefire.MyTest\">"
                                                         + "<img src=\"images/icon_error_sml.gif\" alt=\"\" />"
                                                         + "</a>"
                                                         + "</td>\n"
                                                         + "<td><a href=\"#surefire.MyTest\">MyTest</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( ">surefire.MyTest:13</a>" ) );

        assertThat( htmlContent, containsString( "./xref-test/surefire/MyTest.html#13" ) );

        assertThat( htmlContent, containsString( toSystemNewLine( "<pre>"
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
        + "</pre>" ) ) );
    }

    public void testSurefireReportEnclosedTrimStackTrace()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "surefire-report-enclosed-trimStackTrace/plugin-config.xml" );
        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );
        assertNotNull( mojo );
        mojo.execute();
        File report = new File( getBasedir(), "target/site/unit/surefire-report-enclosed-trimStackTrace/surefire-report.html" );
        renderer( mojo, report );
        assertTrue( report.exists() );
        String htmlContent = FileUtils.fileRead( report );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td>" ) ) );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td><a href=\"#surefire\">surefire</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>"
                                                         + "<a href=\"#surefire.MyTest$A\">"
                                                         + "<img src=\"images/icon_error_sml.gif\" alt=\"\" />"
                                                         + "</a>"
                                                         + "</td>\n"
                                                         + "<td><a href=\"#surefire.MyTest$A\">MyTest$A</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );

        assertThat( htmlContent, containsString( ">surefire.MyTest$A:45</a>" ) );

        assertThat( htmlContent, containsString( "./xref-test/surefire/MyTest$A.html#45" ) );

        assertThat( htmlContent, containsString( toSystemNewLine( "<pre>"
        + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
        + "\tat surefire.MyTest.failure(MyTest.java:33)\n"
        + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
        + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
        + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
        + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)\n"
        + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
        + "\tat surefire.MyTest.access$200(MyTest.java:9)\n"
        + "\tat surefire.MyTest$A.t(MyTest.java:45)\n"
        + "</pre>" ) ) );
    }

    public void testSurefireReportEnclosed()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "surefire-report-enclosed/plugin-config.xml" );
        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );
        assertNotNull( mojo );
        mojo.execute();
        File report = new File( getBasedir(), "target/site/unit/surefire-report-enclosed/surefire-report.html" );
        renderer( mojo, report );
        assertTrue( report.exists() );
        String htmlContent = FileUtils.fileRead( report );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td>" ) ) );

        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                                         + "<td><a href=\"#surefire\">surefire</a></td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>1</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0</td>\n"
                                                         + "<td>0%</td>\n"
                                                         + "<td>0</td></tr>" ) ) );
        assertThat( htmlContent,
                    containsString( toSystemNewLine( "<tr class=\"b\">\n"
                                        + "<td>"
                                        + "<a href=\"#surefire.MyTest$A\">"
                                        + "<img src=\"images/icon_error_sml.gif\" alt=\"\" />"
                                        + "</a>"
                                        + "</td>\n"
                                        + "<td><a href=\"#surefire.MyTest$A\">MyTest$A</a></td>\n"
                                        + "<td>1</td>\n"
                                        + "<td>1</td>\n"
                                        + "<td>0</td>\n"
                                        + "<td>0</td>\n"
                                        + "<td>0%</td>\n"
                                        + "<td>0</td></tr>" ) ) );

        assertThat( htmlContent, containsString( ">surefire.MyTest$A:45</a>" ) );

        assertThat( htmlContent, containsString( "./xref-test/surefire/MyTest$A.html#45" ) );

        assertThat( htmlContent, containsString( toSystemNewLine(
            "<pre>" + "java.lang.RuntimeException: java.lang.IndexOutOfBoundsException\n"
                + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:24)\n"
                + "\tat surefire.MyTest.newRethrownDelegate(MyTest.java:17)\n"
                + "\tat surefire.MyTest.access$200(MyTest.java:9)\n" + "\tat surefire.MyTest$A.t(MyTest.java:45)\n"
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
                + "\tat surefire.MyTest.failure(MyTest.java:33)\n" + "\tat surefire.MyTest.access$100(MyTest.java:9)\n"
                + "\tat surefire.MyTest$Nested.run(MyTest.java:38)\n"
                + "\tat surefire.MyTest.delegate(MyTest.java:29)\n"
                + "\tat surefire.MyTest.rethrownDelegate(MyTest.java:22)\n"
                + "</pre>" ) ) );
    }

    /**
     * Renderer the sink from the report mojo.
     *
     * @param mojo       not null
     * @param outputHtml not null
     * @throws RendererException if any
     * @throws IOException       if any
     */
    private void renderer( SurefireReportMojo mojo, File outputHtml )
        throws RendererException, IOException
    {
        Writer writer = null;
        SiteRenderingContext context = new SiteRenderingContext();
        context.setDecoration( new DecorationModel() );
        context.setTemplateName( "org/apache/maven/doxia/siterenderer/resources/default-site.vm" );
        context.setLocale( Locale.ENGLISH );

        try
        {
            outputHtml.getParentFile().mkdirs();
            writer = WriterFactory.newXmlWriter( outputHtml );

            renderer.generateDocument( writer, (SiteRendererSink) mojo.getSink(), context );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }
}
