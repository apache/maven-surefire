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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertNull;
import static org.fest.assertions.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Test for {@link AbstractSurefireMojo}. jdkToolchain parameter
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( {AbstractSurefireMojo.class} )
@PowerMockIgnore( {"org.jacoco.agent.rt.*", "com.vladium.emma.rt.*"} )
public class AbstractSurefireMojoToolchainsTest
{

    /**
     * Ensure that we use the toolchain found by getToolchainMaven33x()
     * when the jdkToolchain parameter is set.
     */
    @Test
    public void shouldCallMaven33xMethodWhenSpecSet() throws Exception
    {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        Toolchain expectedFromMaven33Method = mock( Toolchain.class );
        MockToolchainManager toolchainManager = new MockToolchainManager( null, null );
        mojo.setToolchainManager( toolchainManager );
        mojo.setJdkToolchain( singletonMap( "version", "1.8" ) );

        mockStatic( AbstractSurefireMojo.class );
        when(
            AbstractSurefireMojo.class,
            "getToolchainMaven33x",
            ToolchainManager.class,
            toolchainManager,
            mojo.getSession(), mojo.getJdkToolchain() ).thenReturn( expectedFromMaven33Method );
        Toolchain actual = invokeMethod( mojo, "getToolchain" );
        assertThat( actual )
            .isSameAs( expectedFromMaven33Method );
    }

    /**
     * Ensure that we use the toolchain from build context when
     * no jdkToolchain map is configured in mojo parameters.
     * getToolchain() returns the main maven toolchain from the build context
     */
    @Test
    public void shouldFallthroughToBuildContextWhenNoSpecSet() throws Exception
    {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        Toolchain expectedFromContext = mock( Toolchain.class );
        Toolchain expectedFromSpec = mock( Toolchain.class ); //ensure it still behaves correctly even if not null
        mojo.setToolchainManager( new MockToolchainManager( expectedFromSpec, expectedFromContext ) );
        Toolchain actual = invokeMethod( mojo, "getToolchain" );
        assertThat( actual )
            .isSameAs( expectedFromContext );
    }

    @Test
    public void shouldReturnNoToolchainInMaven32() throws Exception
    {
        Toolchain toolchain = invokeMethod( AbstractSurefireMojo.class,
            "getToolchainMaven33x",
            MockToolchainManagerMaven32.class,
            new MockToolchainManagerMaven32( null ),
            mock( MavenSession.class ),
            emptyMap() );
        assertNull( toolchain );
    }

    @Test( expected = MojoFailureException.class )
    public void shouldThrowMaven33xToolchain() throws Exception
    {
        invokeMethod(
            AbstractSurefireMojo.class,
            "getToolchainMaven33x",
            MockToolchainManager.class,
            new MockToolchainManager( null, null ),
            mock( MavenSession.class ),
            emptyMap() );
    }

    @Test
    public void shouldGetMaven33xToolchain() throws Exception
    {
        Toolchain expected = mock( Toolchain.class );
        Toolchain actual = invokeMethod(
            AbstractSurefireMojo.class,
            "getToolchainMaven33x",
            MockToolchainManager.class,
            new MockToolchainManager( expected, null ),
            mock( MavenSession.class ),
            emptyMap() );

        assertThat( actual )
            .isSameAs( expected );
    }

    /**
     * Mocks a ToolchainManager
     */
    public static final class MockToolchainManager extends MockToolchainManagerMaven32
    {
        private final Toolchain specToolchain;

        public MockToolchainManager( Toolchain specToolchain, Toolchain buildContextToolchain )
        {
            super( buildContextToolchain );
            this.specToolchain = specToolchain;
        }

        public List<Toolchain> getToolchains( MavenSession session, String type, Map<String, String> requirements )
        {
            return specToolchain == null ? Collections.<Toolchain>emptyList() : singletonList( specToolchain );
        }
    }

    /**
     * Mocks an older version that does not implement getToolchains()
     * returns provided toolchain
     */
    public static class MockToolchainManagerMaven32 implements ToolchainManager
    {

        private final Toolchain buildContextToolchain;

        public MockToolchainManagerMaven32( Toolchain buildContextToolchain )
        {
            this.buildContextToolchain = buildContextToolchain;
        }

        @Override
        public Toolchain getToolchainFromBuildContext( String type, MavenSession context )
        {
            return buildContextToolchain;
        }
    }
}
