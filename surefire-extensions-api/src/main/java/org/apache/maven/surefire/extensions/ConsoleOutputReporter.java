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

import java.io.File;
import java.io.PrintStream;

import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;

/**
 * Extension for logger.
 * The signature can be changed between major, minor versions or milestones.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public abstract class ConsoleOutputReporter
{
    /**
     * {@code false} by default
     */
    private boolean disable;

    /**
     * The content is encoded <em>UTF-8</em> by default.
     */
    private String encoding;

    public abstract ConsoleOutputReportEventListener createListener( File reportsDirectory, String reportNameSuffix,
                                                                     Integer forkNumber );

    public abstract ConsoleOutputReportEventListener createListener( PrintStream out, PrintStream err );

    public abstract Object clone( ClassLoader target );

    public boolean isDisable()
    {
        return disable;
    }

    public void setDisable( boolean disable )
    {
        this.disable = disable;
    }

    public String getEncoding()
    {
        return isBlank( encoding ) ? "UTF-8" : encoding;
    }

    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }
}
