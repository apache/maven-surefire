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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * Test for {@link AbstractSurefireMojo}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractSurefireMojo.class, ResolvePathsRequest.class, ReflectionUtils.class})
@PowerMockIgnore({"org.jacoco.agent.rt.*", "com.vladium.emma.rt.*"})
public class AbstractSurefireMojoToolchainsTest
{

    private final AbstractSurefireMojo mojo = new AbstractSurefireMojoTest.Mojo();

    @Before
    public void setupMojo() throws Exception
    {
        MavenSession session = mock( MavenSession.class );
        mojo.setSession( session );

        MavenExecutionRequest mavenExecutionRequest = mock( MavenExecutionRequest.class );
        when( session.getRequest() ).thenReturn( mavenExecutionRequest );

        Logger logger = mock( Logger.class );
        mojo.setLogger( logger );

        final VersionRange surefireVersion = createFromVersion( "1" );
        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
    }

    @Test
    public void shouldHandleMaven30ToolchainManager() throws Exception

    {
        mockStatic( ReflectionUtils.class );
        //TODO: mock this to return a method
        when( ReflectionUtils.tryGetMethod( ToolchainManager.class, "getToolchains" ) ).thenReturn( null );

        ToolchainManager toolchainManager = mock( ToolchainManager.class );

        AbstractSurefireMojo mojo = spy( this.mojo );
        when( mojo.getToolchainManager() ).thenReturn( toolchainManager );

        Method method = AbstractSurefireMojo.class.getDeclaredMethod( "setupStuff", null );
        method.setAccessible( true );

        Map<String, String> toolchainSpec = new HashMap<String, String>();
        toolchainSpec.put( "version", "1.8" );
        mojo.setJdkToolchain( toolchainSpec );

        method.invoke( mojo );

        verifyStatic( ResolvePathsRequest.class, times( 1 ) );
        verify( toolchainManager, times( 1 )).getToolchainFromBuildContext( null, null );
        verify( mojo, times( 2 ) ).getToolchainManager();
    }
}
