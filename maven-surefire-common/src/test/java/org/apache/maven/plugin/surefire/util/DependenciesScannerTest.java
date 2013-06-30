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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ScanResult;

/**
 * @author Aslak Knutsen
 */
public class DependenciesScannerTest
    extends TestCase
{
    public void testLocateTestClasses()
        throws Exception
    {
        File testFile = writeTestFile();

        // use target as people can configure ide to compile in an other place than maven
        Artifact artifact = new DefaultArtifact(
                "org.surefire.dependency", "test-jar",
                VersionRange.createFromVersion("1.0"), "test", "jar", "tests", null);
        artifact.setFile(testFile);

        List<String> scanDependencies = new ArrayList<String>();
        scanDependencies.add("org.surefire.dependency:test-jar");

        List<String> include = new ArrayList<String>();
        include.add( "**/*A.java" );
        List<String> exclude = new ArrayList<String>();

        DependencyScanner scanner = new DependencyScanner(
                DependencyScanner.filter(Arrays.asList(artifact), scanDependencies),
                include, exclude, new ArrayList<String>() );

        ScanResult classNames = scanner.scan();
        assertNotNull( classNames );
        System.out.println( "classNames " + classNames.toString() );
        assertEquals( 1, classNames.size() );
        System.out.println(classNames.getClassName(0));

        Properties props = new Properties();
        classNames.writeTo( props );
        assertEquals( 1, props.size() );
    }

    private File writeTestFile() throws Exception {
        File output = new File("target/DependenciesScannerTest-tests.jar");
        output.delete();

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(output));

        out.putNextEntry(new ZipEntry("org/test/TestA.class"));
        out.closeEntry();
        out.putNextEntry(new ZipEntry("org/test/TestB.class"));
        out.closeEntry();
        out.close();
        return output;
    }
}
