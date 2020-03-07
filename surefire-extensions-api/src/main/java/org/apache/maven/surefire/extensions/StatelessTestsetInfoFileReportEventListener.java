package org.apache.maven.surefire.extensions;

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

import org.apache.maven.surefire.report.TestSetReportEntry;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Extension listener for stateless file reporter of test-set.
 * Signatures can be changed between major, minor versions or milestones.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 * @param <R> report entry type, see <em>WrappedReportEntry</em> from module the <em>maven-surefire-common</em>
 * @param <S> test-set statistics, see <em>TestSetStats</em> from module the <em>maven-surefire-common</em>
 */
public abstract class StatelessTestsetInfoFileReportEventListener<R extends TestSetReportEntry, S>
{
    private final File reportsDirectory;
    private final String reportNameSuffix;
    private final Charset encoding;

    public StatelessTestsetInfoFileReportEventListener( File reportsDirectory, String reportNameSuffix,
                                                        Charset encoding )
    {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
        this.encoding = encoding;
    }

    public abstract void testSetCompleted( R report, S testSetStats, List<String> testResults );

    protected File getReportsDirectory()
    {
        return reportsDirectory;
    }

    protected String getReportNameSuffix()
    {
        return reportNameSuffix;
    }

    protected Charset getEncoding()
    {
        return encoding;
    }
}
