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

import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;

/**
 * Extension for stateless reporter.
 * Signatures can be changed between major, minor versions or milestones.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 * @param <R> report entry type, see <em>WrappedReportEntry</em> from module the <em>maven-surefire-common</em>
 * @param <S> test-set statistics, see <em>TestSetStats</em> from module the <em>maven-surefire-common</em>
 * @param <C> mojo config, see <em>DefaultStatelessReportMojoConfiguration</em> from <em>maven-surefire-common</em>
 */
public abstract class StatelessReporter<R extends TestSetReportEntry, S, C extends StatelessReportMojoConfiguration>
{
    /**
     * {@code false} by default
     */
    //todo remove isDisableXmlReport() in AbstractSurefireMojo and use this param instead
    private boolean disable;

    /**
     * Version of reporter. It is version <em>3.0</em> used by default in XML reporter.
     */
    private String version;

    /**
     * Creates reporter.
     *
     * @return reporter object
     */
    public abstract StatelessReportEventListener<R, S> createListener( C configuration );

    public abstract Object clone( ClassLoader target );

    public boolean isDisable()
    {
        return disable;
    }

    public void setDisable( boolean disable )
    {
        this.disable = disable;
    }

    public String getVersion()
    {
        return isBlank( version ) ? "3.0" : version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName()
                + "{"
                + "version=" + getVersion()
                + ", disable=" + isDisable()
                + '}';
    }
}
