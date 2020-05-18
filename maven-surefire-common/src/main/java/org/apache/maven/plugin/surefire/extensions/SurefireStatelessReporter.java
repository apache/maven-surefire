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

import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.extensions.StatelessReporter;
import org.apache.maven.surefire.api.util.ReflectionUtils;

/**
 * Default implementation for extension of {@link StatelessXmlReporter} in plugin.
 * Signatures can be changed between major, minor versions or milestones.
 * <br>
 * This is a builder of {@link StatelessReportEventListener listener}.
 * The listener handles <em>testSetCompleted</em> event.
 *
 * author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class SurefireStatelessReporter
        extends StatelessReporter<WrappedReportEntry, TestSetStats, DefaultStatelessReportMojoConfiguration>
{
    /**
     * Activated in the injection point of MOJO.
     */
    public SurefireStatelessReporter()
    {
        this( false, "3.0" );
    }

    /**
     * Activated if null injection point in MOJO.
     * @param disable             {@code true} to disable performing the report
     * @param version             (xsd 3.0) version of the schema
     */
    public SurefireStatelessReporter( boolean disable, String version )
    {
        setDisable( disable );
        setVersion( version );
    }

    @Override
    public StatelessReportEventListener<WrappedReportEntry, TestSetStats> createListener(
            DefaultStatelessReportMojoConfiguration configuration )
    {
        return new StatelessXmlReporter( configuration.getReportsDirectory(),
                configuration.getReportNameSuffix(),
                configuration.isTrimStackTrace(),
                configuration.getRerunFailingTestsCount(),
                configuration.getTestClassMethodRunHistory(),
                configuration.getXsdSchemaLocation(),
                getVersion(),
                false,
                false,
                false,
                false );
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
            cls.getMethod( "setVersion", String.class )
                    .invoke( clone, getVersion() );

            return clone;
        }
        catch ( ReflectiveOperationException e )
        {
            throw new IllegalStateException( e.getLocalizedMessage() );
        }
    }
}
