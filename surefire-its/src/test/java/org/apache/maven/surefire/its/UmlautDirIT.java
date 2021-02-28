package org.apache.maven.surefire.its;

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

import org.apache.maven.shared.utils.io.FileUtils;
import org.apache.maven.surefire.its.fixture.MavenLauncher;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersionExcluded;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.junit.Assume.assumeTrue;

/**
 * Test a directory with an umlaut
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class UmlautDirIT extends SurefireJUnit4IntegrationTestCase
{
    private String localRepo;

    @Before
    public void backupLocalRepo()
    {
        // We touched the Javac bug, see the discussion [1].
        // The fix [2] will be in Java 17, not in Java 16. So we cannot use Java 16 then!
        // [1]: http://ant.1045680.n5.nabble.com/JDK-16-is-in-Rampdown-Phase-One-td5720549.html#a5720552
        // [2]: https://bugs.openjdk.java.net/browse/JDK-8258246
        assumeJavaVersionExcluded( 16 );

        localRepo = System.getProperty( "maven.repo.local" );
    }

    @After
    public void restoreLocalRepo()
    {
        if ( localRepo == null )
        {
            System.clearProperty( "maven.repo.local" );
        }
        else
        {
            System.setProperty( "maven.repo.local", localRepo );
        }
    }

    @Test
    public void surefire1617WithColonInLocalRepo()
            throws Exception
    {
        assumeTrue( IS_OS_LINUX );

        unpack( "junit-pathWithUmlaut", "_compiled" )
                .maven()
                .execute( "compiler:testCompile" );

        String cwd = System.getProperty( "user.dir" );

        Path from = Paths.get( cwd, "target", "UmlautDirIT_surefire1617WithColonInLocalRepo_compiled", "target",
                "test-classes", "umlautTest" );

        Path to = Paths.get( cwd, "target", "UmlautDirIT_surefire1617WithColonInLocalRepo", "target",
                "test-classes", "umlautTest" );

        MavenLauncher mavenLauncher = unpackWithColonInLocalRepo()
                .maven();

        mavenLauncher.setForkJvm( true );
        mavenLauncher.setAutoclean( false );

        if ( !Files.exists( to ) )
        {
            Files.createDirectories( to );
        }

        FileUtils.copyDirectory( from.toFile(), to.toFile() );

        mavenLauncher.sysProp( "skipCompiler", true )
                .debugLogging()
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test
    public void surefire1617WithColonInProjectDir()
            throws Exception
    {
        assumeTrue( IS_OS_LINUX );
        unpackWithNewProjectDirectory( "this is: a test", "_surefire-1617" )
                .setForkJvm()
                .sysProp( "argLine", "-Dpath.separator=;" )
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test
    public void testUmlaut()
        throws Exception
    {
        unpackWithNewProjectDirectory( "/junit-pathWith\u00DCmlaut_", "1" )
                .executeTest()
                .verifyErrorFreeLog()
                .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    @Test
    public void testUmlautIsolatedClassLoader()
        throws Exception
    {
        unpackWithNewProjectDirectory( "/junit-pathWith\u00DCmlaut_", "2" )
                .useSystemClassLoader( false )
                .executeTest()
                .assertTestSuiteResults( 1, 0, 0, 0 );
    }

    private SurefireLauncher unpackWithNewProjectDirectory( String projectDirectory, String postfix )
            throws IOException
    {
        SurefireLauncher unpack = unpack( "junit-pathWithUmlaut" );
        MavenLauncher maven = unpack.maven();

        File dest = new File( maven.getUnpackedAt().getParentFile().getPath(), projectDirectory + postfix );
        maven.moveUnpackTo( dest );
        return unpack;
    }

    private SurefireLauncher unpackWithColonInLocalRepo()
            throws IOException
    {
        String newLocalRepo =
                Paths.get( System.getProperty( "user.dir" ), "target", "local repo for: SUREFIRE-1617" ).toString();
        String defaultLocalRepo = new MavenLauncher( getClass(), "junit-pathWithUmlaut", null ).getLocalRepository();

        copyFolder( Paths.get( defaultLocalRepo, "org", "apache", "maven", "surefire" ),
                Paths.get( newLocalRepo, "org", "apache", "maven", "surefire" ) );

        copyFolder( Paths.get( defaultLocalRepo, "org", "apache", "maven", "plugins", "maven-surefire-plugin" ),
                Paths.get( newLocalRepo, "org", "apache", "maven", "plugins", "maven-surefire-plugin" ) );

        System.setProperty( "maven.repo.local", newLocalRepo );
        return unpack( "junit-pathWithUmlaut" );
    }

    private static void copyFolder( Path src, Path dest )
            throws IOException
    {
        if ( !exists( dest ) )
        {
            createDirectories( dest );
        }

        for ( File from : requireNonNull( src.toFile().listFiles() ) )
        {
            Path to = dest.resolve( from.getName() );
            if ( from.isDirectory() )
            {
                copyFolder( from.toPath(), to );
            }
            else if ( from.isFile() )
            {
                copy( from.toPath(), to, REPLACE_EXISTING );
            }
        }
    }
}
