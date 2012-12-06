package org.apache.maven.plugins.surefire.report.stubs;

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

import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class SurefireRepMavenProjectStub
    extends MavenProjectStub
{
    /**
     * {@inheritDoc}
     */
    public List getReportPlugins()
    {
        Reporting reporting = new Reporting();

        ReportPlugin reportPlugin = new ReportPlugin();
        reportPlugin.setGroupId( "org.apache.maven.plugins" );
        reportPlugin.setArtifactId( "maven-jxr-plugin" );
        reportPlugin.setVersion( "2.0-SNAPSHOT" );
        reporting.addPlugin( reportPlugin );

        Model model = new Model();

        model.setReporting( reporting );

        return reporting.getPlugins();
    }
}
