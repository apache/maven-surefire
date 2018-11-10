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

import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Test;
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
    private static final String BASE_DIR = System.getProperty( "user.dir" );

    @Test
    public void relativeClasspathUnixSimple()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "/home/me/prj/target/surefire";
        String classPathElement = "/home/me/.m2/repository/grp/art/1.0/art-1.0.jar";
        when( JarManifestForkConfiguration.relativize( parent, classPathElement ) )
                .thenReturn( "../../../.m2/repository/grp/art/1.0/art-1.0.jar" );
        when( JarManifestForkConfiguration.toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        File dump = new File( BASE_DIR, "target/test-dump" );
        //noinspection ResultOfMethodCallIgnored
        dump.mkdir();
        assertThat( JarManifestForkConfiguration.toClasspathElementUri( parent, classPathElement, dump ) )
                .isEqualTo( "../../../.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathUnixTricky()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "/home/me/prj/target/surefire";
        String classPathElement = "/the Maven repo/grp/art/1.0/art-1.0.jar";
        when( JarManifestForkConfiguration.relativize( parent, classPathElement ) )
                .thenReturn( "../../../../../the Maven repo/grp/art/1.0/art-1.0.jar" );
        when( JarManifestForkConfiguration.toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        File dump = new File( BASE_DIR, "target/test-dump" );
        assertThat( JarManifestForkConfiguration.toClasspathElementUri( parent, classPathElement, dump ) )
                .isEqualTo( "../../../../../the%20Maven%20repo/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathWindowsSimple()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "C:\\Windows\\Temp\\surefire";
        String classPathElement = "C:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar";
        when( JarManifestForkConfiguration.relativize( parent, classPathElement ) )
                .thenReturn( "..\\..\\..\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( JarManifestForkConfiguration.toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        File dump = new File( BASE_DIR, "target/test-dump" );
        assertThat( JarManifestForkConfiguration.toClasspathElementUri( parent, classPathElement, dump ) )
                .isEqualTo( "../../../Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void relativeClasspathWindowsTricky()
        throws Exception
    {
        mockStatic( JarManifestForkConfiguration.class );
        String parent = "C:\\Windows\\Temp\\surefire";
        String classPathElement = "C:\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar";
        when( JarManifestForkConfiguration.relativize( parent, classPathElement ) )
                .thenReturn( "..\\..\\..\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar" );
        when( JarManifestForkConfiguration.toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        File dump = new File( BASE_DIR, "target/test-dump" );
        assertThat( JarManifestForkConfiguration.toClasspathElementUri( parent, classPathElement, dump ) )
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
        when( JarManifestForkConfiguration.relativize( parent, classPathElement ) )
                .thenThrow( new IllegalArgumentException() );
        when( JarManifestForkConfiguration.toAbsoluteUri( classPathElement ) )
                .thenReturn( "file:///X:/Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
        when( JarManifestForkConfiguration.toClasspathElementUri( anyString(), anyString(), any( File.class ) ) )
                .thenCallRealMethod();
        File dump = new File( BASE_DIR, "target/test-dump" );
        assertThat( JarManifestForkConfiguration.toClasspathElementUri( parent, classPathElement, dump ) )
                .isEqualTo( "file:///X:/Users/me/.m2/repository/grp/art/1.0/art-1.0.jar" );
    }

    @Test
    public void shouldRelativizeOnRealPlatform()
            throws Exception
    {
        File parentDir = new File( BASE_DIR, "target/test-parent" );
        //noinspection ResultOfMethodCallIgnored
        parentDir.mkdir();
        File testDir = new File( BASE_DIR, "target/test with white spaces" );
        //noinspection ResultOfMethodCallIgnored
        testDir.mkdir();

        String relativeTestDir =
                JarManifestForkConfiguration.relativize( parentDir.getCanonicalPath(), testDir.getCanonicalPath() );

        assertThat( relativeTestDir )
                .isEqualTo( ".." + File.separator + "test with white spaces" );
    }

    @Test
    public void shouldMakeAbsoluteUriOnRealPlatform()
            throws Exception
    {
        File testDir = new File( BASE_DIR, "target/@2 test with white spaces" );
        //noinspection ResultOfMethodCallIgnored
        testDir.mkdir();

        URI testDirUri = new URI( JarManifestForkConfiguration.toAbsoluteUri( testDir.getCanonicalPath() ) );

        assertThat( testDirUri.getScheme() )
                .isEqualTo( "file" );

        assertThat( testDirUri.getRawPath() )
                .isEqualTo( testDir.toURI().getRawPath() );
    }

    @Test
    public void shouldMakeRelativeUriOnRealPlatform()
            throws Exception
    {
        File parentDir = new File( BASE_DIR, "target/test-parent" );
        //noinspection ResultOfMethodCallIgnored
        parentDir.mkdir();
        File testDir = new File( BASE_DIR, "target/@3 test with white spaces" );
        //noinspection ResultOfMethodCallIgnored
        testDir.mkdir();

        File dump = new File( BASE_DIR, "target/test-dump" );
        //noinspection ResultOfMethodCallIgnored
        dump.mkdir();

        String parent = parentDir.getCanonicalPath();
        String cpElement = testDir.getCanonicalPath();

        String testDirUriPath = JarManifestForkConfiguration.toClasspathElementUri( parent, cpElement, dump );

        assertThat( testDirUriPath )
                .isEqualTo( "../@3%20test%20with%20white%20spaces" );
    }
}
