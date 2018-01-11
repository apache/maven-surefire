package org.apache.maven.plugin.surefire.util;

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

import static org.junit.Assert.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.ScanResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Aslak Knutsen
 */
public class DependenciesScannerTest
{

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLocateTestClasses()
        throws Exception
    {
        File testFile = writeTestFile( "DependenciesScannerTest-tests.jar", "org/test/TestA.class", "org/test/TestB.class" );

        // use target as people can configure ide to compile in an other place than maven
        Artifact artifact =
            new DefaultArtifact( "org.surefire.dependency", "test-jar", VersionRange.createFromVersion( "1.0" ), "test",
                                 "jar", "tests", null );
        artifact.setFile( testFile );

        List<String> scanDependencies = new ArrayList<>();
        scanDependencies.add( "org.surefire.dependency:test-jar" );

        List<String> include = new ArrayList<>();
        include.add( "**/*A.java" );
        List<String> exclude = new ArrayList<>();

        List<File> dependenciesToScan = filterArtifactsAsFiles(scanDependencies, Collections.singletonList(artifact));

        DependencyScanner scanner =
            new DependencyScanner( dependenciesToScan, new TestListResolver( include, exclude ) );

        ScanResult classNames = scanner.scan();
        assertNotNull( classNames );
        assertEquals( 1, classNames.size() );

        Map<String, String> props = new HashMap<>();
        classNames.writeTo( props );
        assertEquals( 1, props.size() );
    }

    /**
     * Test for artifact with classifier
     */
    @Test
    public void testLocateTestClassesFromArtifactWithClassifier()
        throws Exception
    {
        File testJarFile = writeTestFile( "DependenciesScannerTest2-1.0-tests-jdk15.jar", "org/test/TestA.class",
                                          "org/test/other/TestAA.class", "org/test/TestB.class" );
        Artifact testArtifact =
            new DefaultArtifact( "org.surefire.dependency", "dependent-artifact2",
                                 VersionRange.createFromVersion( "1.0" ), "test", "jar", "tests-jdk15", null );
        testArtifact.setFile( testJarFile );

        List<String> scanDependencies = new ArrayList<String>();
        scanDependencies.add( "org.surefire.dependency:dependent-artifact2:*:*:tests-jdk15" );

        List<String> include = new ArrayList<String>();
        include.add( "**/*A.java" );
        List<String> exclude = new ArrayList<String>();


        List<File> filesToScan = filterArtifactsAsFiles(scanDependencies, Collections.singletonList(testArtifact));

        DependencyScanner scanner =
            new DependencyScanner( filesToScan, new TestListResolver( include, exclude ) );

        ScanResult classNames = scanner.scan();
        assertNotNull( classNames );
        assertEquals( 2, classNames.size() );

        Map<String, String> props = new HashMap<String, String>();
        classNames.writeTo( props );
        assertEquals( 2, props.size() );
    }

    /**
     * Test with type when two artifacts are present, should only find the class in jar with correct type
     */
    @Test
    public void testLocateTestClassesFromMultipleArtifactsWithType()
        throws Exception
    {
        File jarFile =
            writeTestFile( "DependenciesScannerTest3-1.0.jar", "org/test/ClassA.class", "org/test/ClassB.class" );
        Artifact mainArtifact = new DefaultArtifact( "org.surefire.dependency", "dependent-artifact3",
                                                     VersionRange.createFromVersion( "1.0" ), "test", "jar", null,
                                                     new DefaultArtifactHandler() );
        mainArtifact.setFile( jarFile );

        File testJarFile =
            writeTestFile( "DependenciesScannerTest3-1.0-tests.jar", "org/test/TestA.class", "org/test/TestB.class" );
        Artifact testArtifact = new DefaultArtifact( "org.surefire.dependency", "dependent-artifact3",
                                                     VersionRange.createFromVersion( "1.0" ), "test", "test-jar", null,
                                                     new DefaultArtifactHandler() );
        testArtifact.setFile( testJarFile );

        List<String> scanDependencies = new ArrayList<String>();
        scanDependencies.add( "org.surefire.dependency:dependent-artifact3:test-jar" );

        List<String> include = new ArrayList<String>();
        include.add( "**/*A.java" );
        List<String> exclude = new ArrayList<String>();

        List<Artifact> artifacts = Arrays.asList( mainArtifact, testArtifact );

        List<File> filesToScan = filterArtifactsAsFiles(scanDependencies, artifacts);

        DependencyScanner scanner = new DependencyScanner( filesToScan, new TestListResolver( include, exclude ) );

        ScanResult classNames = scanner.scan();
        assertNotNull( classNames );
        assertEquals( 1, classNames.size() );
        assertEquals( "org.test.TestA", classNames.getClassName( 0 ) );

        Map<String, String> props = new HashMap<String, String>();
        classNames.writeTo( props );
        assertEquals( 1, props.size() );
    }

    /**
     * Test to pick the right version of an artifact to scan
     */
    @Test
    public void testLocateTestClassesFromMultipleVersionsOfArtifact()
        throws Exception
    {
        File jarFile10 =
            writeTestFile( "DependenciesScannerTest4-1.0.jar", "org/test/ClassA.class", "org/test/ClassB.class" );
        Artifact artifact10 = new DefaultArtifact( "org.surefire.dependency", "dependent-artifact4",
                                                   VersionRange.createFromVersion( "1.0" ), "test", "jar", null,
                                                   new DefaultArtifactHandler() );
        artifact10.setFile( jarFile10 );

        File jarFile20 =
            writeTestFile( "DependenciesScannerTest4-2.0.jar", "org/test2/ClassA.class", "org/test2/ClassB.class" );
        Artifact artifact20 = new DefaultArtifact( "org.surefire.dependency", "dependent-artifact4",
                                                   VersionRange.createFromVersion( "2.0" ), "test", "jar", null,
                                                   new DefaultArtifactHandler() );
        artifact20.setFile( jarFile20 );

        List<String> scanDependencies = new ArrayList<String>();
        scanDependencies.add( "org.surefire.dependency:dependent-artifact4:*:2.0" );

        List<String> include = new ArrayList<String>();
        include.add( "**/*A.java" );
        List<String> exclude = new ArrayList<String>();

        List<Artifact> artifacts = Arrays.asList( artifact10, artifact20 );

        List<File> filesToScan = filterArtifactsAsFiles(scanDependencies, artifacts);
        DependencyScanner scanner = new DependencyScanner( filesToScan, new TestListResolver( include, exclude ) );

        ScanResult classNames = scanner.scan();
        assertNotNull( classNames );
        assertEquals( 1, classNames.size() );
        assertEquals( "org.test2.ClassA", classNames.getClassName( 0 ) );

        Map<String, String> props = new HashMap<String, String>();
        classNames.writeTo( props );
        assertEquals( 1, props.size() );
        assertFalse( props.values().contains( "org.test.ClassA" ) );
    }

    private static List<File> filterArtifactsAsFiles(List<String> scanDependencies, List<Artifact> artifacts) {
        List<File> filesToScan = new ArrayList<>();
        for (Artifact a : DependencyScanner.filter(artifacts, scanDependencies)) {
            filesToScan.add(a.getFile());
        }
        return filesToScan;
    }

    private File writeTestFile( String fileName, String... entries )
        throws Exception
    {
        File output = tempFolder.newFile( fileName );

        try ( ZipOutputStream out = new ZipOutputStream( new FileOutputStream( output ) ) )
        {
            for ( String entry : entries )
            {
                out.putNextEntry( new ZipEntry( entry ) );
                out.closeEntry();
            }
            return output;
        }
    }
}
