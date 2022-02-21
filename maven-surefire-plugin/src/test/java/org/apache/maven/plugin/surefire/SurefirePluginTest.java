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

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.codehaus.plexus.logging.Logger;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class SurefirePluginTest extends TestCase
{
    @Rule
    public final ExpectedException e = ExpectedException.none();

    public void testDefaultIncludes()
    {
        assertThat( new SurefirePlugin().getDefaultIncludes() )
                .containsOnly( "**/Test*.java", "**/*Test.java", "**/*Tests.java", "**/*TestCase.java" );
    }

    public void testReportSchemaLocation()
    {
        assertThat( new SurefirePlugin().getReportSchemaLocation() )
            .isEqualTo( "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd" );
    }

    public void testFailIfNoTests() throws Exception
    {
        RunResult runResult = new RunResult( 0, 0, 0, 0 );
        try
        {
            SurefirePlugin plugin = new SurefirePlugin();
            plugin.setFailIfNoTests( true );
            plugin.handleSummary( runResult, null );
        }
        catch ( MojoFailureException e )
        {
            assertThat( e.getLocalizedMessage() )
                    .isEqualTo( "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)" );
            return;
        }
        fail( "Expected MojoFailureException with message "
                + "'No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)'" );
    }

    public void testTestFailure() throws Exception
    {
        RunResult runResult = new RunResult( 1, 0, 1, 0 );
        try
        {
            SurefirePlugin plugin = new SurefirePlugin();
            plugin.handleSummary( runResult, null );
        }
        catch ( MojoFailureException e )
        {
            assertThat( e.getLocalizedMessage() )
                    .isEqualTo( "There are test failures.\n\nPlease refer to null "
                            + "for the individual test results.\nPlease refer to dump files (if any exist) "
                            + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream." );
            return;
        }
        fail( "Expected MojoFailureException with message "
                + "'There are test failures.\n\nPlease refer to null "
                + "for the individual test results.\nPlease refer to dump files (if any exist) "
                + "[date].dump, [date]-jvmRun[N].dump and [date].dumpstream.'" );
    }

    public void testPluginName()
    {
        assertThat( new SurefirePlugin().getPluginName() )
                .isEqualTo( "surefire" );
    }

    public void testShouldGetNullEnv()
    {
        SurefirePlugin plugin = new SurefirePlugin();
        assertThat( plugin.getExcludedEnvironmentVariables() )
                .hasSize( 0 );
    }

    public void testShouldGetEnv()
    {
        SurefirePlugin plugin = new SurefirePlugin();
        plugin.setExcludedEnvironmentVariables( new String[] { "ABC", "KLM" } );
        assertThat( plugin.getExcludedEnvironmentVariables() )
                .hasSize( 2 )
                .contains( "ABC", "KLM" );
    }

    public void testShouldGetPropertyFile()
    {
        SurefirePlugin plugin = new SurefirePlugin();
        plugin.setSystemPropertiesFile( new File( "testShouldGetPropertyFile" ) );
        assertThat( plugin.getSystemPropertiesFile() )
                .isEqualTo( new File( "testShouldGetPropertyFile" ) );
    }

    public void testNegativeFailOnFlakeCount()
    {
        SurefirePlugin plugin = new SurefirePlugin();
        plugin.setFailOnFlakeCount( -1 );
        e.expect( MojoFailureException.class );
        e.expectMessage( "Parameter \"failOnFlakeCount\" should not be negative." );
    }

    public void testFailOnFlakeCountWithoutRerun()
    {
        SurefirePlugin plugin = new SurefirePlugin();
        plugin.setFailOnFlakeCount( 1 );
        e.expect( MojoFailureException.class );
        e.expectMessage( "\"failOnFlakeCount\" requires rerunFailingTestsCount to be at least 1." );
    }

    public void testShouldHaveJUnit5EnginesFilter()
    {
        SurefirePlugin plugin = new SurefirePlugin();

        plugin.setIncludeJUnit5Engines( new String[] { "e1", "e2" } );
        assertThat( plugin.getIncludeJUnit5Engines() ).isEqualTo( new String[] { "e1", "e2" } );

        plugin.setExcludeJUnit5Engines( new String[] { "e1", "e2" } );
        assertThat( plugin.getExcludeJUnit5Engines() ).isEqualTo( new String[] { "e1", "e2" } );
    }
    
    public void testShouldNotPerformMethodFilteringOnIncludes() throws IOException 
    {
        MockSurefirePlugin plugin = mockSurefirePluginWithMethodFiltering( false );

        List<String> includes = new LinkedList<>();
        includes.add( "AnotherTest#method " );
        plugin.setIncludes( includes );
        
        try
        {
            plugin.scanDependencies();
        }
        catch ( MojoFailureException e ) 
        {
            assertThat( e.getLocalizedMessage() )
            .isEqualTo( "Method filter prohibited in includes|excludes parameter: AnotherTest#method " );
        }
    }
    
    public void testShouldPerformMethodFilteringOnIncludesExcludesFile() throws IOException, MojoFailureException
    {
        MockSurefirePlugin plugin = mockSurefirePluginWithMethodFiltering( true );
        DefaultScanResult result = plugin.scanDependencies();
        assertThat ( result.getClasses().size() ).isEqualTo( 1 );
        assertThat ( result.getClasses().iterator().next() ).isEqualTo( "AnotherTest" );
    }
    
    private static MockSurefirePlugin mockSurefirePluginWithMethodFiltering( boolean file ) throws IOException 
    {
        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "test-jar", null, handler );

        File artifactFile = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile.getCanonicalPath();
        assertThat( artifactFile.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();

        testDeps.setFile( classes );

        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
                .isTrue();

        List<Artifact> projectTestArtifacts = singletonList( testDeps );
        String[] dependenciesToScan = { "g:a" };
        MockSurefirePlugin plugin = new MockSurefirePlugin( projectTestArtifacts, dependenciesToScan, file );
        plugin.setLogger( mock( Logger.class ) );
        return plugin;
    }
    
    private static final class MockSurefirePlugin
        extends SurefirePlugin 
    {
        private final List<Artifact> projectTestArtifacts;
        
        private final String[] dependenciesToScan;
        
        private final boolean file;

        MockSurefirePlugin( List<Artifact> projectTestArtifacts, String[] dependenciesToScan, boolean file )
        {
            this.projectTestArtifacts = projectTestArtifacts;
            this.dependenciesToScan = dependenciesToScan;
            this.file = file;
        }
        
        @Override
        public String[] getDependenciesToScan()
        {
            return dependenciesToScan;
        }
        
        @Override
        List<Artifact> getProjectTestArtifacts()
        {
            return projectTestArtifacts;
        }
        
        @Override
        public File getIncludesFile()
        {
            return this.file ? getFile() : null;
        }
        
        @Override
        public File getExcludesFile()
        {
            return getIncludesFile();
        }
        
        private File getFile() 
        {
            File file = null;
            try 
            {
                file = File.createTempFile( "surefire", "-includes" );
                FileUtils.write( file, "AnotherTest#method" , StandardCharsets.UTF_8 );
            } 
            catch ( IOException e ) 
            {
                // do nothing
            }
            return file;
        }

    }
    
}
