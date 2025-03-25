/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.failsafe;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerifyMojoTest {
    private VerifyMojo mojo;

    @TempDir
    private File tempFolder;

    private Logger logger = mock(Logger.class);

    @BeforeEach
    void init() throws UnsupportedEncodingException {
        mojo = new VerifyMojo(logger);
        mojo.setTestClassesDirectory(tempFolder);
        mojo.setReportsDirectory(getTestBaseDir());
    }

    private void setupExecuteMocks() {
        when(logger.isErrorEnabled()).thenReturn(true);
        when(logger.isWarnEnabled()).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(false);

        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(request.isShowErrors()).thenReturn(true);
        when(request.getReactorFailureBehavior()).thenReturn(null);
        when(session.getRequest()).thenReturn(request);
        mojo.setSession(session);
    }

    private File getTestBaseDir() throws UnsupportedEncodingException {
        URL resource = getClass().getResource("/verify-mojo");
        // URLDecoder.decode necessary for JDK 1.5+, where spaces are escaped to %20
        return new File(URLDecoder.decode(resource.getPath(), "UTF-8")).getAbsoluteFile();
    }

    @Test
    void executeForForkError() throws UnsupportedEncodingException {
        setupExecuteMocks();
        mojo.setSummaryFile(new File(getTestBaseDir(), "failsafe-summary-booter-fork-error.xml"));

        assertThatCode(mojo::execute).isExactlyInstanceOf(MojoExecutionException.class);
    }

    @Test
    void executeForForkErrorTestFailureIgnore() throws UnsupportedEncodingException {
        setupExecuteMocks();
        mojo.setSummaryFile(new File(getTestBaseDir(), "failsafe-summary-booter-fork-error.xml"));
        mojo.setTestFailureIgnore(true);

        assertThatCode(mojo::execute).isExactlyInstanceOf(MojoExecutionException.class);
    }

    @Test
    void executeForPassingTests() throws MojoExecutionException, MojoFailureException, UnsupportedEncodingException {
        setupExecuteMocks();
        mojo.setSummaryFile(new File(getTestBaseDir(), "failsafe-summary-success.xml"));
        mojo.execute();
    }
}
