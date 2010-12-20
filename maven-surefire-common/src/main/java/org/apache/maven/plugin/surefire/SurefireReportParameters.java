package org.apache.maven.plugin.surefire;

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

/**
 * The parameters required to report on a surefire execution.
 *
 * @author Stephen Connolly
 * @noinspection UnusedDeclaration
 */
public interface SurefireReportParameters
{
    boolean isSkipTests();

    void setSkipTests( boolean skipTests );

    boolean isSkipExec();

    void setSkipExec( boolean skipExec );

    boolean isSkip();

    void setSkip( boolean skip );

    boolean isTestFailureIgnore();

    void setTestFailureIgnore( boolean testFailureIgnore );

    File getBasedir();

    void setBasedir( File basedir );

    File getTestClassesDirectory();

    void setTestClassesDirectory( File testClassesDirectory );

    File getReportsDirectory();

    void setReportsDirectory( File reportsDirectory );

    Boolean getFailIfNoTests();

    void setFailIfNoTests( Boolean failIfNoTests );
}
