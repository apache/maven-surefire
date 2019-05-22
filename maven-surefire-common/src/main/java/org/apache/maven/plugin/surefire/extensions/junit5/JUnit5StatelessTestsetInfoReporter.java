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

import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.ConsoleReporter;
import org.apache.maven.plugin.surefire.report.FileReporter;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Extension of {@link StatelessTestsetInfoConsoleReportEventListener file and console reporter of test-set} for JUnit5.
 * Signatures can be changed between major, minor versions or milestones.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class JUnit5StatelessTestsetInfoReporter
        extends SurefireStatelessTestsetInfoReporter
{
    /**
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedFileName;

    /**
     * Phrased class name of test case in the console log (see xxx)
     * <em>Running xxx</em> or file report log <em>Test set: xxx</em>.
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedClassNameInRunning;

    /**
     * Phrased class name of test case in the log (see xxx)
     * <em>Tests run: ., Failures: ., Errors: ., Skipped: ., Time elapsed: . s, - in xxx</em>.
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedClassNameInTestCaseSummary;

    public boolean isUsePhrasedFileName()
    {
        return usePhrasedFileName;
    }

    public void setUsePhrasedFileName( boolean usePhrasedFileName )
    {
        this.usePhrasedFileName = usePhrasedFileName;
    }

    public boolean isUsePhrasedClassNameInRunning()
    {
        return usePhrasedClassNameInRunning;
    }

    public void setUsePhrasedClassNameInRunning( boolean usePhrasedClassNameInRunning )
    {
        this.usePhrasedClassNameInRunning = usePhrasedClassNameInRunning;
    }

    public boolean isUsePhrasedClassNameInTestCaseSummary()
    {
        return usePhrasedClassNameInTestCaseSummary;
    }

    public void setUsePhrasedClassNameInTestCaseSummary( boolean usePhrasedClassNameInTestCaseSummary )
    {
        this.usePhrasedClassNameInTestCaseSummary = usePhrasedClassNameInTestCaseSummary;
    }

    @Override
    public StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> createListener(
            ConsoleLogger logger )
    {
        return new ConsoleReporter( logger, isUsePhrasedClassNameInRunning(),
                                    isUsePhrasedClassNameInTestCaseSummary() );
    }

    @Override
    public StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> createListener(
            File reportsDirectory, String reportNameSuffix, Charset encoding )
    {
        return new FileReporter( reportsDirectory, reportNameSuffix, encoding, isUsePhrasedFileName(),
                                 isUsePhrasedClassNameInRunning(), isUsePhrasedClassNameInTestCaseSummary() );
    }

    @Override
    public Object clone( ClassLoader target )
    {
        try
        {
            Object clone = super.clone( target );

            Class<?> cls = clone.getClass();
            cls.getMethod( "setUsePhrasedFileName", boolean.class )
                    .invoke( clone, isUsePhrasedFileName() );
            cls.getMethod( "setUsePhrasedClassNameInTestCaseSummary", boolean.class )
                    .invoke( clone, isUsePhrasedFileName() );
            cls.getMethod( "setUsePhrasedClassNameInRunning", boolean.class )
                    .invoke( clone, isUsePhrasedFileName() );

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
        return "JUnit5StatelessTestsetInfoReporter{"
                + "disable=" + isDisable()
                + ", usePhrasedFileName=" + isUsePhrasedFileName()
                + ", usePhrasedClassNameInRunning=" + isUsePhrasedClassNameInRunning()
                + ", usePhrasedClassNameInTestCaseSummary=" + isUsePhrasedClassNameInTestCaseSummary()
                + "}";
    }
}
