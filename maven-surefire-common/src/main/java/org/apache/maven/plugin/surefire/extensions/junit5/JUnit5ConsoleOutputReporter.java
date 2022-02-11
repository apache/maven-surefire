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

import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;

import java.io.File;

/**
 * The extension of {@link ConsoleOutputReportEventListener logger} for JUnit5.
 * Selectively enables report files upon JUnit5 annotation <em>DisplayName</em>.
 *
 * author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class JUnit5ConsoleOutputReporter
        extends SurefireConsoleOutputReporter
{
    /**
     * {@code false} by default.
     * <br>
     * This action takes effect only in JUnit5 provider together with a test class annotated <em>DisplayName</em>.
     */
    private boolean usePhrasedFileName;

    public boolean isUsePhrasedFileName()
    {
        return usePhrasedFileName;
    }

    public void setUsePhrasedFileName( boolean usePhrasedFileName )
    {
        this.usePhrasedFileName = usePhrasedFileName;
    }

    @Override
    public ConsoleOutputReportEventListener createListener( File reportsDirectory, String reportNameSuffix,
                                                            Integer forkNumber )
    {
        return new ConsoleOutputFileReporter( reportsDirectory, reportNameSuffix, isUsePhrasedFileName(), forkNumber,
                getEncoding() );
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
        return "JUnit5ConsoleOutputReporter{"
                + "disable=" + isDisable()
                + ", encoding=" + getEncoding()
                + ", usePhrasedFileName=" + isUsePhrasedFileName()
                + '}';
    }
}
