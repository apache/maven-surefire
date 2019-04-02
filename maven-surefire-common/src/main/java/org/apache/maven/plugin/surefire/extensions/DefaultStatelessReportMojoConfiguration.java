package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.extensions.StatelessReportMojoConfiguration;

import java.io.File;
import java.util.Deque;
import java.util.Map;

/**
 * Why Deprecated: The field {@code testClassMethodRunHistory} makes
 * {@link org.apache.maven.plugin.surefire.report.StatelessXmlReporter} stateful and overloads reporter by permanently
 * overriding XML using re-run feature. To fix this issue, the providers should use more events for re-run feature and
 * events bounding provider's execution. After provider's execution is finished, this reporter should be announced
 * only once per test class. All test report entries should be cached in
 * {@link org.apache.maven.plugin.surefire.report.TestSetRunListener} keeping it already stateful.
 */
@Deprecated
public class DefaultStatelessReportMojoConfiguration
        extends StatelessReportMojoConfiguration
{
    private final Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistory;

    public DefaultStatelessReportMojoConfiguration( File reportsDirectory,
                                                    String reportNameSuffix,
                                                    boolean trimStackTrace,
                                                    int rerunFailingTestsCount,
                                                    String xsdSchemaLocation,
                                                    Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistory )
    {
        super( reportsDirectory, reportNameSuffix, trimStackTrace, rerunFailingTestsCount, xsdSchemaLocation );
        this.testClassMethodRunHistory = testClassMethodRunHistory;
    }

    public Map<String, Deque<WrappedReportEntry>> getTestClassMethodRunHistory()
    {
        return testClassMethodRunHistory;
    }
}
