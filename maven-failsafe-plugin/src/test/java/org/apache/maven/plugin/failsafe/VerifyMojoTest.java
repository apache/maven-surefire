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
import org.codehaus.plexus.logging.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VerifyMojoTest {
    private VerifyMojo mojo;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Logger logger = mock(Logger.class);

    @Before
    public void init() throws UnsupportedEncodingException {
        mojo = new VerifyMojo(logger);
        mojo.setTestClassesDirectory(tempFolder.getRoot());
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

    @Test(expected = MojoExecutionException.class)
    public void executeForForkError()
            throws MojoExecutionException, MojoFailureException, UnsupportedEncodingException {
        setupExecuteMocks();
        mojo.setSummaryFile(new File(getTestBaseDir(), "failsafe-summary-booter-fork-error.xml"));
        mojo.execute();
    }

    @Test(expected = MojoExecutionException.class)
    public void executeForForkErrorTestFailureIgnore()
            throws MojoExecutionException, MojoFailureException, UnsupportedEncodingException {
        setupExecuteMocks();
        mojo.setSummaryFile(new File(getTestBaseDir(), "failsafe-summary-booter-fork-error.xml"));
        mojo.setTestFailureIgnore(true);
        mojo.execute();
    }

    @Test
    public void executeForPassingTests()
            throws MojoExecutionException, MojoFailureException, UnsupportedEncodingException {
        setupExecuteMocks();
        mojo.setSummaryFile(new File(getTestBaseDir(), "failsafe-summary-success.xml"));
        mojo.execute();
    }
}
