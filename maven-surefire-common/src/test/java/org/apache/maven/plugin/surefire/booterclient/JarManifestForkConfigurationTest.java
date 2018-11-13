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
import java.io.IOException;
import java.net.URI;

import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;

import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.relativize;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.toAbsoluteUri;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.toClasspathElementUri;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit tests for {@link JarManifestForkConfiguration}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( { JarManifestForkConfiguration.class, InPluginProcessDumpSingleton.class } )
public class JarManifestForkConfigurationTest
{
    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static File dumpDirectory;

    @BeforeClass
    public static void createSystemTemporaryDir()
            throws IOException
    {
        TMP.create();
        dumpDirectory = TMP.newFolder();
    }

    @Test
    public void relativeClasspathUnixSimple()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "/home/me/prj/target/surefire";
        String classPathElement = "/home/me/.m2/repository/grp/art/1.0/art-1.0.jar";
        when( relativize( parent, classPathElement ) )
                .thenReturn( "../../../.m2/repository/grp/art/1.0/art-1.0.jar" );
        when( toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathUnixTricky()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "/home/me/prj/target/surefire";
        String classPathElement = "/the Maven repo/grp/art/1.0/art-1.0.jar";
        when( relativize( parent, classPathElement ) )
                .thenReturn( "../../../../../the Maven repo/grp/art/1.0/art-1.0.jar" );
        when( toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../../../the%20Maven%20repo/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathWindowsSimple()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "C:\\Windows\\Temp\\surefire";
        String classPathElement = "C:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar";
        when( relativize( parent, classPathElement ) )
                .thenReturn( "..\\..\\..\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "../../../Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathWindowsTricky()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "C:\\Windows\\Temp\\surefire";
        String classPathElement = "C:\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar";
        when( relativize( parent, classPathElement ) )
                .thenReturn( "..\\..\\..\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
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
        when( InPluginProcessDumpSingleton.getSingleton() ).thenReturn(mock( InPluginProcessDumpSingleton.class ));
        String parent = "C:\\Windows\\Temp\\surefire";
        String classPathElement = "X:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar";
        when( relativize( parent, classPathElement ) )
                .thenThrow( new IllegalArgumentException() );
        when( toAbsoluteUri( classPathElement ) )
                .thenReturn( "file:///X:/Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
        when( toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        assertThat( toClasspathElementUri( parent, classPathElement, dumpDirectory ) )
                .isEqualTo( "file:///X:/Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void shouldRelativizeOnRealPlatform()
            throws Exception
    {
        File parentDir = TMP.newFolder( "test-parent-1" )
                .getCanonicalFile();

        File testDir = TMP.newFolder( "@1 test with white spaces" )
                .getCanonicalFile();

        String relativeTestDir = relativize( parentDir.getPath(), testDir.getPath() );

        assertThat( relativeTestDir )
                .isEqualTo( ".." + File.separator + "@1 test with white spaces" );
    }

    @Test
    public void shouldMakeAbsoluteUriOnRealPlatform()
            throws Exception
    {
        File testDir = TMP.newFolder( "@2 test with white spaces" )
                .getCanonicalFile();

        URI testDirUri = new URI( toAbsoluteUri( testDir.getPath() ) );

        assertThat( testDirUri.getScheme() )
                .isEqualTo( "file" );

        assertThat( testDirUri.getRawPath() )
                .isEqualTo( testDir.toURI().getRawPath() );
    }

    @Test
    public void shouldMakeRelativeUriOnRealPlatform()
            throws Exception
    {
        File parentDir = TMP.newFolder( "test-parent-2" )
                .getCanonicalFile();

        File testDir = TMP.newFolder( "@3 test with white spaces" )
                .getCanonicalFile();

        String testDirUriPath = toClasspathElementUri( parentDir.getPath(), testDir.getPath(), dumpDirectory );

        assertThat( testDirUriPath )
                .isEqualTo( "../@3%20test%20with%20white%20spaces" );
    }
}
