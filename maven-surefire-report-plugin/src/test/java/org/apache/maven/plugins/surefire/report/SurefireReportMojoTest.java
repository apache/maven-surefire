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
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.utils.WriterFactory;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class SurefireReportMojoTest
    extends AbstractMojoTestCase
{
    public void testBasicSurefireReport()
        throws Exception
    {
        File testPom = new File( getUnitBaseDir(), "basic-surefire-report-test/plugin-config.xml" );

        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        assertNotNull( mojo );

        File outputDir = (File) getVariableValueFromObject( mojo, "outputDirectory" );

        boolean showSuccess = ( (Boolean) getVariableValueFromObject( mojo, "showSuccess" ) ).booleanValue();

        File reportsDir = (File) getVariableValueFromObject( mojo, "reportsDirectory" );

        String outputName = (String) getVariableValueFromObject( mojo, "outputName" );

        File xrefLocation = (File) getVariableValueFromObject( mojo, "xrefLocation" );

        boolean linkXRef = ( (Boolean) getVariableValueFromObject( mojo, "linkXRef" ) ).booleanValue();

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

        boolean showSuccess = ( (Boolean) getVariableValueFromObject( mojo, "showSuccess" ) ).booleanValue();

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

        boolean linkXRef = ( (Boolean) getVariableValueFromObject( mojo, "linkXRef" ) ).booleanValue();

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

            mojo.getSiteRenderer().generateDocument( writer, (SiteRendererSink) mojo.getSink(), context );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }
}
