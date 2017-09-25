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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.apache.commons.lang3.JavaVersion.JAVA_1_7;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AbstractSurefireMojo}.
 */
@RunWith( MockitoJUnitRunner.class )
public class AbstractSurefireMojoTest
{
    @Mock
    private AbstractSurefireMojo mojo;

    @Test
    public void shouldHaveTmpDirectory() throws IOException
    {
        assumeTrue( isJavaVersionAtLeast( JAVA_1_7 ) );

        Path path = (Path) AbstractSurefireMojo.createTmpDirectoryWithJava7( "surefire" );

        assertThat( path )
                .isNotNull();

        assertThat( path.startsWith( System.getProperty( "java.io.tmpdir" ) ) )
                .isTrue();

        String dir = path.getName( path.getNameCount() - 1 ).toString();

        assertThat( dir )
                .startsWith( "surefire" );

        assertThat( dir )
                .matches( "^surefire[\\d]+$" );
    }

    @Test
    public void shouldHaveTmpDirectoryName() throws IOException
    {
        assumeTrue( isJavaVersionAtLeast( JAVA_1_7 ) );

        String dir = AbstractSurefireMojo.createTmpDirectoryNameWithJava7( "surefire" );

        assertThat( dir )
                .isNotNull();

        assertThat( dir )
                .startsWith( "surefire" );

        assertThat( dir )
                .matches( "^surefire[\\d]+$" );
    }

    @Test
    public void shouldExistTmpDirectory()
    {
        when( mojo.getTempDir() ).thenReturn( "surefireX" );
        when( mojo.getProjectBuildDirectory() ).thenReturn( new File( System.getProperty( "user.dir" ), "target" ) );
        when( mojo.createSurefireBootDirectoryInTemp() ).thenCallRealMethod();
        when( mojo.createSurefireBootDirectoryInBuild() ).thenCallRealMethod();
        when( mojo.getSurefireTempDir() ).thenCallRealMethod();

        File tmp = mojo.createSurefireBootDirectoryInTemp();
        assertThat( tmp ).isNotNull();
        assertThat( tmp ).exists();
        assertThat( tmp.getAbsolutePath() )
                .startsWith( System.getProperty( "java.io.tmpdir" ) );
        assertThat( tmp.getName() )
                .startsWith( "surefireX" );

        tmp = mojo.createSurefireBootDirectoryInBuild();
        assertThat( tmp ).isNotNull();
        assertThat( tmp ).exists();
        assertThat( tmp.getAbsolutePath() ).startsWith( System.getProperty( "user.dir" ) );
        assertThat( tmp.getName() ).isEqualTo( "surefireX" );

        tmp = mojo.getSurefireTempDir();
        assertThat( tmp ).isNotNull();
        assertThat( tmp ).exists();
        assertThat( tmp.getAbsolutePath() )
              .startsWith( IS_OS_WINDOWS ? System.getProperty( "java.io.tmpdir" ) : System.getProperty( "user.dir" ) );
        if ( IS_OS_WINDOWS )
        {
            assertThat( tmp.getName() )
                    .startsWith( "surefireX" );
        }
        else
        {
            assertThat( tmp.getName() )
                    .isEqualTo( "surefireX" );
        }
    }
}
