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

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.util.ScanResult;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
                DependencyScanner.filter(Collections.singletonList(artifact), scanDependencies),
                new TestListResolver( include, exclude ), new TestListResolver( "" ) );

        ScanResult classNames = scanner.scan();
        assertNotNull( classNames );
        System.out.println( "classNames " + classNames.toString() );
        assertEquals( 1, classNames.size() );
        System.out.println(classNames.getClassName(0));

        Map<String, String> props = new HashMap<String, String>();
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
