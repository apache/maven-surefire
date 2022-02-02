package org.apache.maven.plugin.failsafe;

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
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 2.20
 */
public class IntegrationTestMojoTest
{
    private IntegrationTestMojo mojo;

    @Before
    public void init() throws InvalidVersionSpecificationException, IOException
    {
        Artifact artifact = new DefaultArtifact( "g", "a", createFromVersionSpec( "1.0" ), "compile", "jar", "", null );
        artifact.setFile( new File( "./target/tmp/a-1.0.jar" ) );
        new File( "./target/tmp" ).mkdir();
        artifact.getFile().createNewFile();
        mojo = new IntegrationTestMojo();
        MavenProject project = mock( MavenProject.class );
        when( project.getArtifact() ).thenReturn( artifact );
        mojo.setProject( project );
    }

    @Test
    public void shouldBeJar()
    {
        mojo.setDefaultClassesDirectory( new File( "./target/classes" ) );
        File binaries = mojo.getMainBuildPath();
        assertThat( binaries.getName() ).isEqualTo( "a-1.0.jar" );
    }

    @Test
    public void shouldBeAnotherJar()
    {
        mojo.setMainBuildPath( new File( "./target/another-1.0.jar" ) );
        mojo.setDefaultClassesDirectory( new File( "./target/classes" ) );
        File binaries = mojo.getMainBuildPath();
        assertThat( binaries.getName() ).isEqualTo( "another-1.0.jar" );
    }

    @Test
    public void shouldBeClasses()
    {
        mojo.setMainBuildPath( new File( "./target/classes" ) );
        mojo.setDefaultClassesDirectory( new File( "./target/classes" ) );
        File binaries = mojo.getMainBuildPath();
        assertThat( binaries.getName() ).isEqualTo( "classes" );
    }

    @Test
    public void shouldGetNullEnv()
    {
        assertThat( mojo.getExcludedEnvironmentVariables() )
                .hasSize( 0 );
    }

    @Test
    public void shouldGetEnv()
    {
        mojo.setExcludedEnvironmentVariables( new String[] { "ABC", "KLM" } );
        assertThat( mojo.getExcludedEnvironmentVariables() )
                .hasSize( 2 )
                .contains( "ABC", "KLM" );
    }

    @Test
    public void testShouldGetPropertyFile()
    {
        mojo.setSystemPropertiesFile( new File( "testShouldGetPropertyFile" ) );
        assertThat( mojo.getSystemPropertiesFile() )
                .isEqualTo( new File( "testShouldGetPropertyFile" ) );
    }

    @Test
    public void shouldHaveJUnit5EnginesFilter()
    {
        mojo.setIncludeJUnit5Engines( new String[] { "e1", "e2" } );
        assertThat( mojo.getIncludeJUnit5Engines() ).isEqualTo( new String[] { "e1", "e2" } );

        mojo.setExcludeJUnit5Engines( new String[] { "e1", "e2" } );
        assertThat( mojo.getExcludeJUnit5Engines() ).isEqualTo( new String[] { "e1", "e2" } );
    }
}
