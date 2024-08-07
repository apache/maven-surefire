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
package org.apache.maven.plugins.surefire.report.stubs;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class EnclosedTrimStackTraceStub extends SurefireReportMavenProjectStub {
    private List<ReportPlugin> reportPlugins = new ArrayList<>();

    public EnclosedTrimStackTraceStub() {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try (InputStream is = new FileInputStream(getFile())) {
            model = pomReader.read(is);
            setModel(model);
        } catch (Exception e) {
        }

        setReportPlugins(model.getReporting().getPlugins());
    }

    public void setReportPlugins(List<ReportPlugin> plugins) {
        this.reportPlugins = plugins;
    }

    /** {@inheritDoc} */
    @Override
    public List<ReportPlugin> getReportPlugins() {
        return reportPlugins;
    }

    @Override
    protected String getProjectDirName() {
        return "surefire-report-enclosed-trimStackTrace";
    }
}
