package org.apache.maven.plugin.surefire.extensions.junit5;

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

import org.apache.maven.plugin.surefire.extensions.DefaultStatelessReportMojoConfiguration;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.report.StatelessXmlReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;

/**
 * The extension of {@link StatelessReportEventListener xml reporter} based on XSD version 3.0 for JUnit5.
 * Selectively enables phrased classes, methods and report files upon JUnit5 annotation <em>DisplayName</em>.
 *
 * author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class JUnit5Xml30StatelessReporter
        extends SurefireStatelessReporter
{
    /**
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedFileName;

    /**
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedTestSuiteClassName;

    /**
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedTestCaseClassName;

    /**
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test method annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedTestCaseMethodName;

    public boolean getUsePhrasedFileName()
    {
        return usePhrasedFileName;
    }

    public void setUsePhrasedFileName( boolean usePhrasedFileName )
    {
        this.usePhrasedFileName = usePhrasedFileName;
    }

    public boolean getUsePhrasedTestSuiteClassName()
    {
        return usePhrasedTestSuiteClassName;
    }

    public void setUsePhrasedTestSuiteClassName( boolean usePhrasedTestSuiteClassName )
    {
        this.usePhrasedTestSuiteClassName = usePhrasedTestSuiteClassName;
    }

    public boolean getUsePhrasedTestCaseClassName()
    {
        return usePhrasedTestCaseClassName;
    }

    public void setUsePhrasedTestCaseClassName( boolean usePhrasedTestCaseClassName )
    {
        this.usePhrasedTestCaseClassName = usePhrasedTestCaseClassName;
    }

    public boolean getUsePhrasedTestCaseMethodName()
    {
        return usePhrasedTestCaseMethodName;
    }

    public void setUsePhrasedTestCaseMethodName( boolean usePhrasedTestCaseMethodName )
    {
        this.usePhrasedTestCaseMethodName = usePhrasedTestCaseMethodName;
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
                getUsePhrasedFileName(),
                getUsePhrasedTestSuiteClassName(),
                getUsePhrasedTestCaseClassName(),
                getUsePhrasedTestCaseMethodName() );
    }

    @Override
    public Object clone( ClassLoader target )
    {
        try
        {
            Object clone = super.clone( target );

            Class<?> cls = clone.getClass();
            cls.getMethod( "setUsePhrasedFileName", boolean.class )
                    .invoke( clone, getUsePhrasedFileName() );
            cls.getMethod( "setUsePhrasedTestSuiteClassName", boolean.class )
                    .invoke( clone, getUsePhrasedTestSuiteClassName() );
            cls.getMethod( "setUsePhrasedTestCaseClassName", boolean.class )
                    .invoke( clone, getUsePhrasedTestCaseClassName() );
            cls.getMethod( "setUsePhrasedTestCaseMethodName", boolean.class )
                    .invoke( clone, getUsePhrasedTestCaseMethodName() );

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
        return "JUnit5Xml30StatelessReporter{"
                + "version=" + getVersion()
                + ", disable=" + isDisable()
                + ", usePhrasedFileName=" + getUsePhrasedFileName()
                + ", usePhrasedTestSuiteClassName=" + getUsePhrasedTestSuiteClassName()
                + ", usePhrasedTestCaseClassName=" + getUsePhrasedTestCaseClassName()
                + ", usePhrasedTestCaseMethodName=" + getUsePhrasedTestCaseMethodName()
                + '}';
    }
}
