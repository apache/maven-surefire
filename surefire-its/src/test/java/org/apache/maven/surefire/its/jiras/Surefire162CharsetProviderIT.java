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
package org.apache.maven.surefire.its.jiras;

import java.io.File;

import org.apache.maven.surefire.its.fixture.MavenLauncher;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

/**
 * Test charset provider (SUREFIRE-162)
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class Surefire162CharsetProviderIT extends SurefireJUnit4IntegrationTestCase {
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    public void testCharsetProvider() throws Exception {
        SurefireLauncher unpack = unpack("/surefire-162-charsetProvider");
        MavenLauncher maven = unpack.maven();
        OutputValidator verifier = maven.getValidator();
        File jarFile = maven.getArtifactPath("jcharset", "jcharset", "1.2.1", "jar");
        File pomFile = maven.getArtifactPath("jcharset", "jcharset", "1.2.1", "pom");
        jarFile.getParentFile().mkdirs();
        FileUtils.copyFile(verifier.getSubFile("repo/jcharset/jcharset/1.2.1/jcharset-1.2.1.jar"), jarFile);
        FileUtils.copyFile(verifier.getSubFile("repo/jcharset/jcharset/1.2.1/jcharset-1.2.1.pom"), pomFile);
        unpack.executeTest().verifyErrorFree(1);
    }
}
