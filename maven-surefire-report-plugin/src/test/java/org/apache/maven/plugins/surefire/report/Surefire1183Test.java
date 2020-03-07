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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.utils.WriterFactory;
import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.shared.utils.io.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * Prevent fom NPE if failure type and message is null however detail presents.
 */
public class Surefire1183Test extends AbstractMojoTestCase
{
    private Renderer renderer;

    @Override
    protected void setUp()
            throws Exception
    {
        super.setUp();
        renderer = (Renderer) lookup( Renderer.ROLE );
    }

    private File getTestBaseDir()
            throws UnsupportedEncodingException
    {
        URL resource = getClass().getResource( "/surefire-1183" );
        // URLDecoder.decode necessary for JDK 1.5+, where spaces are escaped to %20
        return new File( URLDecoder.decode ( resource.getPath(), "UTF-8" ) ).getAbsoluteFile();
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
            writer = WriterFactory.newXmlWriter ( outputHtml );

            renderer.generateDocument( writer, (SiteRendererSink ) mojo.getSink(), context );
        }
        finally
        {
            IOUtil.close ( writer );
        }
    }

    public void testCustomTitleAndDescriptionReport()
            throws Exception
    {
        File testPom = new File( getTestBaseDir(), "plugin-config.xml" );
        SurefireReportMojo mojo = (SurefireReportMojo) lookupMojo( "report", testPom );

        File outputDir = (File) getVariableValueFromObject( mojo, "outputDirectory" );
        String outputName = (String) getVariableValueFromObject( mojo, "outputName" );
        File reportsDir = (File) getVariableValueFromObject( mojo, "reportsDirectory" );
        String title = (String) getVariableValueFromObject( mojo, "title" );
        String description = (String) getVariableValueFromObject( mojo, "description" );

        assertEquals( new File( getBasedir() + "/target/site/surefire-1183" ), outputDir );
        assertEquals( new File( getBasedir() + "/src/test/resources/surefire-1183/acceptancetest-reports" )
                        .getAbsolutePath(), reportsDir.getAbsolutePath() );
        assertEquals( "acceptance-test-report", outputName );
        assertEquals( "Acceptance Test", title );
        assertEquals( "Acceptance Test Description", description );

        mojo.execute();

        File report = new File( getBasedir(), "target/site/acceptance-test-report.html" );
        renderer( mojo, report );

        assertTrue( report.exists() );

        String htmlContent = FileUtils.fileRead ( report );
        assertTrue( htmlContent.contains ( "<h2><a name=\"Acceptance_Test\"></a>Acceptance Test</h2></section>" ) );
    }
}
