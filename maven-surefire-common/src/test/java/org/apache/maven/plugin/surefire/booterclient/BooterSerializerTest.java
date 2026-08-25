/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire.booterclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.util.RunOrder;
import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.TypeEncodedValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the serialization of the fields added for the forked test discovery
 * (<a href="https://github.com/apache/maven-surefire/issues/2151">Issue 2151</a>).
 */
class BooterSerializerTest {
    private static int idx = 0;

    private File basedir;

    @BeforeEach
    void setupDirectories() throws IOException {
        File target = new File(System.getProperty("user.dir"), "target");
        basedir = new File(target, "BooterSerializerTest-" + ++idx);
        FileUtils.deleteDirectory(basedir);
        assertTrue(basedir.mkdirs());
    }

    @AfterEach
    void deleteDirectories() throws IOException {
        FileUtils.deleteDirectory(basedir);
    }

    @Test
    void discoverTestsOutputFileRoundTrips() throws IOException {
        String discoverTestsOutputFile = new File(basedir, "discovered.txt").getAbsolutePath();

        BooterDeserializer deserializer = serializeAndReload("aTest", discoverTestsOutputFile);

        assertEquals(discoverTestsOutputFile, deserializer.getDiscoverTestsOutputFile());
    }

    @Test
    void discoverTestsOutputFileIsNullForNormalRun() throws IOException {
        BooterDeserializer deserializer = serializeAndReload("aTest", null);

        assertNull(deserializer.getDiscoverTestsOutputFile());
    }

    @Test
    void classNameTestSetIsEncodedAsClass() throws IOException {
        TypeEncodedValue testSet = new TypeEncodedValue(Class.class.getName(), "com.example.SomeTest");

        BooterDeserializer deserializer = serializeAndReload(testSet, null);

        TypeEncodedValue forkTestSet = deserializer.deserialize().getTestForFork();
        assertEquals(Class.class.getName(), forkTestSet.getType());
        assertEquals("com.example.SomeTest", forkTestSet.getValue());
    }

    private BooterDeserializer serializeAndReload(Object testSet, String discoverTestsOutputFile) throws IOException {
        ForkConfiguration forkConfiguration = ForkConfigurationTest.getForkConfiguration(basedir, null);
        BooterSerializer booterSerializer = new BooterSerializer(forkConfiguration);
        File propsTest = booterSerializer.serialize(
                new PropertiesWrapper(new HashMap<>()),
                getProviderConfiguration(),
                getStartupConfiguration(),
                testSet,
                false,
                51L,
                1,
                "pipe://1",
                discoverTestsOutputFile);
        return new BooterDeserializer(new FileInputStream(propsTest));
    }

    private ProviderConfiguration getProviderConfiguration() {
        File cwd = new File(".");
        DirectoryScannerParameters directoryScannerParameters = new DirectoryScannerParameters(
                cwd,
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                RunOrder.asString(RunOrder.DEFAULT));
        return new ProviderConfiguration(
                directoryScannerParameters,
                new RunOrderParameters(RunOrder.DEFAULT, null),
                new ReporterConfiguration(cwd, true),
                new TestArtifactInfo("5.0", "ABC"),
                new TestRequest(new File("TestSrc"), new TestListResolver(""), 0),
                new HashMap<>(),
                null,
                false,
                Collections.<org.apache.maven.surefire.api.cli.CommandLineOption>emptyList(),
                0,
                Shutdown.DEFAULT,
                0);
    }

    private StartupConfiguration getStartupConfiguration() {
        return new StartupConfiguration(
                "com.provider",
                new ClasspathConfiguration(true, true),
                new ClassLoaderConfiguration(true, false),
                ALL,
                Collections.<String[]>emptyList());
    }
}
