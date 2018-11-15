package org.apache.maven.plugin.surefire.booterclient;

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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;

import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.relativize;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.toAbsoluteUri;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.toClasspathElementUri;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.util.Files.delete;
import static org.fest.util.Files.newTemporaryFolder;
import static org.mockito.ArgumentMatchers.same;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit tests for {@link JarManifestForkConfiguration}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( { JarManifestForkConfiguration.class, InPluginProcessDumpSingleton.class } )
public class JarManifestForkConfigurationTest
{
    private static final File TMP = newTemporaryFolder();

    private static File dumpDirectory;

    @BeforeClass
    public static void createSystemTemporaryDir()
    {
        dumpDirectory = new File( TMP, "dump" );
        assertThat( dumpDirectory.mkdir() )
                .isTrue();
    }

    @AfterClass
    public static void deleteSystemTemporaryDir()
    {
        delete( TMP );
    }

    @Test
    public void relativeClasspathUnixSimple()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        Path parent = mock( Path.class );
        when( parent.toString() ).thenReturn( "/home/me/prj/target/surefire" );
        Path classPathElement = mock( Path.class );
        when( classPathElement.toString() ).thenReturn( "/home/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
        when( relativize( parent, classPathElement ) )
                .thenReturn( "../../../.m2/repository/grp/art/1.0/art-1.0.jar" );
        when( toClasspathElementUri( same( parent ), same( classPathElement ), same( dumpDirectory ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathUnixTricky()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        Path parent = mock( Path.class );
        when( parent.toString() ).thenReturn( "/home/me/prj/target/surefire" );
        Path classPathElement = mock( Path.class );
        when( classPathElement.toString() ).thenReturn( "/the Maven repo/grp/art/1.0/art-1.0.jar" );
        when( relativize( parent, classPathElement ) )
                .thenReturn( "../../../../../the Maven repo/grp/art/1.0/art-1.0.jar" );
        when( toClasspathElementUri( same( parent ), same( classPathElement ), same( dumpDirectory ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../../../the%20Maven%20repo/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathWindowsSimple()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        Path parent = mock( Path.class );
        when( parent.toString() ).thenReturn( "C:\\Windows\\Temp\\surefire" );
        Path classPathElement = mock( Path.class );
        when( classPathElement.toString() ).thenReturn( "C:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( relativize( parent, classPathElement ) )
                .thenReturn( "..\\..\\..\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( toClasspathElementUri( same( parent ), same( classPathElement ), same( dumpDirectory ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathWindowsTricky()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        Path parent = mock( Path.class );
        when( parent.toString() ).thenReturn( "C:\\Windows\\Temp\\surefire" );
        Path classPathElement = mock( Path.class );
        when( classPathElement.toString() ).thenReturn( "C:\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( relativize( parent, classPathElement ) )
                .thenReturn( "..\\..\\..\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( toClasspathElementUri( same( parent ), same( classPathElement ), same( dumpDirectory ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../Test%20User/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void crossDriveWindows()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        mockStatic( InPluginProcessDumpSingleton.class );
        when( InPluginProcessDumpSingleton.getSingleton() ).thenReturn( mock( InPluginProcessDumpSingleton.class ) );
        Path parent = mock( Path.class );
        when( parent.toString() ).thenReturn( "C:\\Windows\\Temp\\surefire" );
        Path classPathElement = mock( Path.class );
        when( classPathElement.toString() ).thenReturn( "X:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( classPathElement.toUri() )
                .thenAnswer( new Answer<URI>()
                {
                    @Override
                    public URI answer( InvocationOnMock invocation ) throws URISyntaxException
                    {
                        String path = invocation.getMock().toString();
                        return new URI( "file", "", "/" + path.replace( '\\', '/' ), null );
                    }
                } );
        when( relativize( same( parent ), same( classPathElement ) ) )
                .thenThrow( new IllegalArgumentException() );
        when( toClasspathElementUri( same( parent ), same( classPathElement ), same( dumpDirectory ) ) )
                .thenCallRealMethod();
        when( toAbsoluteUri( same( classPathElement ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "file:///X:/Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void shouldRelativizeOnRealPlatform()
    {
        Path parentDir = new File( TMP, "test-parent-1" )
                .toPath();

        Path testDir = new File( TMP, "@1 test with white spaces" )
                .toPath();

        String relativeTestDir = relativize( parentDir, testDir );

        assertThat( relativeTestDir )
                .isEqualTo( ".." + File.separator + "@1 test with white spaces" );
    }

    @Test
    public void shouldMakeAbsoluteUriOnRealPlatform()
            throws Exception
    {
        Path testDir = new File( TMP, "@2 test with white spaces" )
                .toPath();

        URI testDirUri = new URI( toAbsoluteUri( testDir ) );

        assertThat( testDirUri.getScheme() )
                .isEqualTo( "file" );

        assertThat( testDirUri.getRawPath() )
                .isEqualTo( testDir.toUri().getRawPath() );
    }

    @Test
    public void shouldMakeRelativeUriOnRealPlatform()
            throws Exception
    {
        Path parentDir = new File( TMP, "test-parent-2" )
                .toPath();

        Path testDir = new File( TMP, "@3 test with white spaces" )
                .toPath();

        String testDirUriPath = toClasspathElementUri( parentDir, testDir, dumpDirectory );

        assertThat( testDirUriPath )
                .isEqualTo( "../@3%20test%20with%20white%20spaces" );
    }
}
