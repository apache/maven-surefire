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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.ConsoleReporter;
import org.apache.maven.plugin.surefire.report.FileReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoReporter;
import org.apache.maven.surefire.api.util.ReflectionUtils;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Default implementation for extension of
 * {@link StatelessTestsetInfoFileReportEventListener test-set event listener for stateless file and console reporter}
 * in plugin. Signatures can be changed between major, minor versions or milestones.
 * <br>
 * Builds {@link StatelessTestsetInfoFileReportEventListener listeners}.
 * The listener handles <em>testSetCompleted</em> event.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class SurefireStatelessTestsetInfoReporter
        extends StatelessTestsetInfoReporter<WrappedReportEntry, TestSetStats>
{
    @Override
    public StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> createListener(
            ConsoleLogger logger )
    {
        return new ConsoleReporter( logger, false, false );
    }

    @Override
    public StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> createListener(
            File reportsDirectory, String reportNameSuffix, Charset encoding )
    {
        return new FileReporter( reportsDirectory, reportNameSuffix, encoding, false, false, false );
    }

    @Override
    public Object clone( ClassLoader target )
    {
        try
        {
            Class<?> cls = ReflectionUtils.reloadClass( target, this );
            Object clone = cls.newInstance();

            cls.getMethod( "setDisable", boolean.class )
                    .invoke( clone, isDisable() );

            return clone;
        }
        catch ( ReflectiveOperationException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage() );
        }
    }

    @Override
    public String toString()
    {
        return "SurefireStatelessTestsetInfoReporter{disable=" + isDisable() + "}";
    }
}
