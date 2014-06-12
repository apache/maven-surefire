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
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/**
 * Creates a nicely formatted Surefire Test Report in html format.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 */
@Mojo(name = "report", inheritByDefault = false)
@Execute(lifecycle = "surefire", phase = LifecyclePhase.TEST)
public class SurefireReportMojo
    extends AbstractSurefireReportMojo
{

    /**
     * The filename to use for the report.
     *
     * @noinspection UnusedDeclaration
     */
    @Parameter(defaultValue = "surefire-report", property = "outputName", required = true)
    private String outputName;

    /**
     * If set to true the surefire report will be generated even when there are no surefire result files.
     * Defaults to <code>true</code> to preserve legacy behaviour pre 2.10.
     *
     * @noinspection UnusedDeclaration
     * @since 2.11
     */
    @Parameter(defaultValue = "true", property = "alwaysGenerateSurefireReport")
    private boolean alwaysGenerateSurefireReport;

    /**
     * If set to true the surefire report generation will be skipped.
     *
     * @noinspection UnusedDeclaration
     * @since 2.11
     */
    @Parameter(defaultValue = "false", property = "skipSurefireReport")
    private boolean skipSurefireReport;

    protected File getSurefireReportsDirectory( MavenProject subProject )
    {
        String buildDir = subProject.getBuild().getDirectory();
        return new File( buildDir + "/surefire-reports" );
    }

    public String getOutputName()
    {
        return outputName;
    }

    protected boolean isSkipped()
    {
        return skipSurefireReport;
    }

    protected boolean isGeneratedWhenNoResults()
    {
        return alwaysGenerateSurefireReport;
    }
}
