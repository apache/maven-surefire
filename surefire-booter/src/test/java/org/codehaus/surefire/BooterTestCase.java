package org.codehaus.surefire;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * JUnit test class whose tests invoke SurefireBooter with forkMode set to
 * "none" and "once" in sequence.
 *
 * @author <a href="mailto:andyglick@acm.org">Andy Glick</a>
 * @version $Id$
 */
public class BooterTestCase extends TestCase
{
    private static final Log log = LogFactory.getLog( BooterTestCase.class );

    private static String mavenRepoLocal = "F:\\m2-repository\\repository";

    private static String repoLocal = mavenRepoLocal;

    private String basedir = null;

    private static String OS = System.getProperty("os.name");

    private static File classesDirectory = null;
    private static File testClassesDirectory = null;

    private static String reportsDirectory = null;

    /**
     * Default no arg constructor
     */
    public BooterTestCase()
    {
        super();
    }

    /**
     * Implements JUnit constructor idiom.
     *
     * @param name the name of the test case
     */
    public BooterTestCase( String name )
    {
        super( name );
    }

    /**
     * JUnit setUp method: establishes data values reused by tests
     */
    public void setUp() throws Exception
    {
        super.setUp();

        String dir = System.getProperty( "user.dir" ); // ${basedir}

        File currentDirectory = new File( dir );

        basedir = currentDirectory.getAbsolutePath();

        String parent = currentDirectory.getParent();

        log.debug("parent is " + parent );

        classesDirectory
            = new File( parent,  "surefire\\target\\classes" );
        testClassesDirectory
            = new File( parent,  "surefire\\target\\test-classes" );

        reportsDirectory
            = "target\\surefire-reports";
    }

    /**
     * testSurefireViaBooter: tests surefire-booter & surefire without forking
     */
    public void testSurefireViaBooter() throws Exception
    {
        String forkMode = "none";

        log.info("forkmode is " + forkMode);

        surefireBooterTestFixture( forkMode );
    }

    /**
     * testForkSurefireViaBooter: tests surefire-booter forking surefire
     */
    public void testForkSurefireViaBooter() throws Exception
    {
        String forkMode = "once";

        log.info("forkmode is " + forkMode);

        surefireBooterTestFixture( forkMode );
    }

    public void surefireBooterTestFixture(String forkMode)
    {
        log.debug( "entering testSurefireBooter" );

        boolean skip = false; // ${maven.test.skip}
        boolean testFailureIgnore = false; // ${maven.test.failure.ignore}

        log.debug( "basedir is " + basedir );

        List classpathElements = new ArrayList();

        String test = "";

        List includes = new ArrayList();

        List excludes = new ArrayList();

        ArtifactRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();

        ArtifactRepository localRepository = new DefaultArtifactRepository( "local",
            "file://" + repoLocal, repositoryLayout );

        SurefireBooter sfb = new SurefireBooter();

        sfb.setForkMode( forkMode );

        sfb.setReportsDirectory( reportsDirectory );

        System.setProperty( "basedir", basedir );

        log.debug( "localRepository set into System Properties as " +
            localRepository.getBasedir() );

        System.setProperty( "localRepository", localRepository.getBasedir() );

        includes = new ArrayList(
            Arrays.asList( new String[]{"**/Test*.java",
                "**/*Test.java",
                "**/*TestCase.java"} ) );

        excludes = new ArrayList(
            Arrays.asList( new String[]{"**/Abstract*Test.java",
                "**/Abstract*TestCase.java"} ) );

        sfb.addBattery( "org.codehaus.surefire.battery.DirectoryBattery",
            new Object[]{testClassesDirectory,
                includes,
                excludes} );

        sfb.addReport( "org.codehaus.surefire.report.ConsoleReporter" );

        sfb.addReport( "org.codehaus.surefire.report.FileReporter" );

        sfb.addReport( "org.codehaus.surefire.report.XMLReporter" );

        log.debug( "Test Classpath :" );

        log.debug( testClassesDirectory.getAbsolutePath() );

        sfb.addClassPathUrl( testClassesDirectory.getAbsolutePath() );

        log.debug( classesDirectory.getAbsolutePath() );

        sfb.addClassPathUrl( classesDirectory.getAbsolutePath() );

        classpathElements.add( "target\\classes" );

        classpathElements.addAll( dependenciesToList() );

        for( Iterator i = classpathElements.iterator(); i.hasNext(); )
        {
            String classpathElement = ( String ) i.next();

            log.debug( classpathElement );

            sfb.addClassPathUrl( classpathElement );
        }

        boolean success = true;

        try
        {
            success = sfb.run();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }

        if ( success )
        {
            log.info( "---------------------------------------------------" );
            log.info( "BUILD SUCCESSFUL" );
            log.info( "---------------------------------------------------" );
        }
        else
        {
            log.info( "---------------------------------------------------" );
            log.info( "BUILD FAILED" );
            log.info( "---------------------------------------------------" );
            log.info( "Reason: There were 1 or more test failures or errors" );
        }
    }

    /**
     * Transform that maps groupId, artifactId, version and artifact type
     * to a file system path spec in an M2 repository
     *
     * @return List of classpath entries
     */
    private List dependenciesToList()
    {
        String[] dependencies =
        {
            "junit:junit:3.8.1:jar",
            "org.apache.maven:maven-artifact:2.0-beta-2-SNAPSHOT:jar",
            "org.apache.maven:maven-artifact-manager:2.0-beta-2-SNAPSHOT:jar",
            "plexus:plexus-utils:1.0.3:jar",
            "org.apache.maven.wagon:wagon-provider-api:1.0-alpha-4:jar",
            "commons-logging:commons-logging:1.0.4:jar"
        };

        List dependencyList = new ArrayList();

        String fileSeparator = System.getProperty( "file.separator" );

        for( int i = 0; i < dependencies.length; i++ )
        {
            String dependencyElement = dependencies[i];

            String[] dependencyElements = dependencyElement.split( ":" );

            StringBuffer dependency = new StringBuffer();

            dependency.append( repoLocal );
            dependency.append( fileSeparator );

            dependency.append( dependencyElements[0].replace( '.', '\\' ) );

            dependency.append( fileSeparator );
            dependency.append( dependencyElements[1] );
            dependency.append( fileSeparator );
            dependency.append( dependencyElements[2] );
            dependency.append( fileSeparator );
            dependency.append( dependencyElements[1] );
            dependency.append( "-" );
            dependency.append( dependencyElements[2] );
            dependency.append( "." );
            dependency.append( dependencyElements[3] );

            String jar = new String( dependency );

            log.debug( "jar name is " + jar );

            dependencyList.add( jar );
        }

        return dependencyList;
    }
}