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
package org.apache.maven.surefire.booter;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.maven.surefire.api.booter.BaseProviderFactory;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.provider.SurefireProvider;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.util.RunOrder;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.apache.maven.surefire.api.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.api.cli.CommandLineOption.SHOW_ERRORS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
public class SurefireReflectorTest {
    @Test
    public void testShouldCreateFactoryWithoutException() {
        ReporterFactory factory = new ReporterFactory() {
            @Override
            public TestReportListener<TestOutputReportEntry> createTestReportListener() {
                return null;
            }

            @Override
            public RunResult close() {
                return null;
            }
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        SurefireReflector reflector = new SurefireReflector(cl);
        BaseProviderFactory bpf = (BaseProviderFactory) reflector.createBooterConfiguration(cl, true);
        bpf.setReporterFactory(factory);
        assertNotNull(bpf.getReporterFactory());
        assertSame(factory, bpf.getReporterFactory());
    }

    @Test
    public void testSetDirectoryScannerParameters() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        DirectoryScannerParameters directoryScannerParameters = new DirectoryScannerParameters(
                new File("ABC"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
        surefireReflector.setDirectoryScannerParameters(foo, directoryScannerParameters);
        assertTrue(isCalled(foo));
        assertNotNull(((Foo) foo).getDirectoryScannerParameters());
    }

    @Test
    public void testNullSetDirectoryScannerParameters() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setDirectoryScannerParameters(foo, null);
        assertTrue(isCalled(foo));
        assertNull(((Foo) foo).getDirectoryScannerParameters());
    }

    @Test
    public void testSetIfDirScannerAware() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        DirectoryScannerParameters directoryScannerParameters = new DirectoryScannerParameters(
                new File("ABC"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
        surefireReflector.setIfDirScannerAware(foo, directoryScannerParameters);
        assertTrue(isCalled(foo));
    }

    @Test
    public void testRunOrderParameters() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        RunOrderParameters runOrderParameters = new RunOrderParameters(RunOrder.DEFAULT, new File("."));
        surefireReflector.setRunOrderParameters(foo, runOrderParameters);
        assertTrue(isCalled(foo));
    }

    @Test
    public void testRunOrderParametersWithRunOrderRandomSeed() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        // Arbitrary random seed that should be ignored because RunOrder is not RANDOM
        Long runOrderRandomSeed = 5L;

        RunOrderParameters runOrderParameters =
                new RunOrderParameters(RunOrder.DEFAULT, new File("."), runOrderRandomSeed);
        surefireReflector.setRunOrderParameters(foo, runOrderParameters);
        assertTrue(isCalled(foo));
    }

    @Test
    public void testNullRunOrderParameters() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setRunOrderParameters(foo, null);
        assertTrue(isCalled(foo));
        try {
            ((Foo) foo).getRunOrderCalculator();
        } catch (NullPointerException e) {
            return;
        }
        fail();
    }

    @Test
    public void testTestSuiteDefinition() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestRequest testSuiteDefinition =
                new TestRequest(new File("TestSOurce"), new TestListResolver("aUserRequestedTest#aMethodRequested"), 0);
        surefireReflector.setTestSuiteDefinition(foo, testSuiteDefinition);
        assertTrue(isCalled(foo));
        assertNotNull(((Foo) foo).getTestRequest());
    }

    @Test
    public void testNullTestSuiteDefinition() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();
        surefireReflector.setTestSuiteDefinition(foo, null);
        assertTrue(isCalled(foo));
        assertNull(((Foo) foo).getTestRequest());
    }

    @Test
    public void testProviderProperties() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setProviderProperties(foo, new HashMap<>());
        assertTrue(isCalled(foo));
    }

    @Test
    public void testReporterConfiguration() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        ReporterConfiguration reporterConfiguration = getReporterConfiguration();
        surefireReflector.setReporterConfigurationAware(foo, reporterConfiguration);
        assertTrue(isCalled(foo));
    }

    private ReporterConfiguration getReporterConfiguration() {
        return new ReporterConfiguration(new File("CDE"), true);
    }

    @Test
    public void testTestClassLoader() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestClassLoader(foo, getClass().getClassLoader());
        assertTrue(isCalled(foo));
    }

    @Test
    public void testTestClassLoaderAware() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestClassLoaderAware(foo, getClass().getClassLoader());
        assertTrue(isCalled(foo));
    }

    @Test
    public void testArtifactInfo() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestArtifactInfo testArtifactInfo = new TestArtifactInfo("12.3", "test");
        surefireReflector.setTestArtifactInfo(foo, testArtifactInfo);
        assertTrue(isCalled(foo));
    }

    @Test
    public void testNullArtifactInfo() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestArtifactInfo(foo, null);
        assertTrue(isCalled(foo));
        assertNull(((Foo) foo).getTestArtifactInfo());
    }

    @Test
    public void testArtifactInfoAware() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestArtifactInfo testArtifactInfo = new TestArtifactInfo("12.3", "test");
        surefireReflector.setTestArtifactInfoAware(foo, testArtifactInfo);
        assertTrue(isCalled(foo));
        assertEquals("test", testArtifactInfo.getClassifier());
        assertEquals("12.3", testArtifactInfo.getVersion());
    }

    @Test
    public void testReporterFactory() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        ReporterFactory reporterFactory = new ReporterFactory() {
            @Override
            public TestReportListener<TestOutputReportEntry> createTestReportListener() {
                return null;
            }

            @Override
            public RunResult close() {
                return null;
            }
        };

        surefireReflector.setReporterFactory(foo, reporterFactory);
        assertTrue(isCalled(foo));
    }

    @Test
    public void testReporterFactoryAware() {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        ReporterFactory reporterFactory = new ReporterFactory() {
            @Override
            public TestReportListener<TestOutputReportEntry> createTestReportListener() {
                return null;
            }

            @Override
            public RunResult close() {
                return null;
            }
        };

        surefireReflector.setReporterFactoryAware(foo, reporterFactory);
        assertTrue(isCalled(foo));
        assertSame(((Foo) foo).getReporterFactory(), reporterFactory);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test
    public void testConvertIfRunResult() {
        RunResult runResult = new RunResult(20, 1, 2, 3, 4, "IOException", true);
        SurefireReflector reflector =
                new SurefireReflector(Thread.currentThread().getContextClassLoader());
        RunResult obj = (RunResult) reflector.convertIfRunResult(runResult);
        assertEquals(20, obj.getCompletedCount());
        assertEquals(1, obj.getErrors());
        assertEquals(2, obj.getFailures());
        assertEquals(3, obj.getSkipped());
        assertFalse(obj.isErrorFree());
        assertFalse(obj.isInternalError());
        assertEquals((Integer) RunResult.FAILURE, obj.getFailsafeCode());

        assertNull(reflector.convertIfRunResult(null));
        assertEquals("", reflector.convertIfRunResult(""));
    }

    @Test
    public void testInstantiateProvider() {
        SurefireReflector reflector =
                new SurefireReflector(Thread.currentThread().getContextClassLoader());
        Object booterParams = getFoo();
        Object provider = reflector.instantiateProvider(DummyProvider.class.getName(), booterParams);
        assertNotNull(provider);
        assertEquals(DummyProvider.class, provider.getClass());
    }

    @Test
    public void testSetMainCliOptions() {
        SurefireReflector reflector =
                new SurefireReflector(Thread.currentThread().getContextClassLoader());
        Object booterParams = getFoo();
        reflector.setMainCliOptions(booterParams, asList(SHOW_ERRORS, LOGGING_LEVEL_DEBUG));
        assertEquals(2, ((BaseProviderFactory) booterParams).getMainCliOptions().size());
        assertEquals(
                SHOW_ERRORS,
                ((BaseProviderFactory) booterParams).getMainCliOptions().get(0));
        assertEquals(
                LOGGING_LEVEL_DEBUG,
                ((BaseProviderFactory) booterParams).getMainCliOptions().get(1));
    }

    @Test
    public void testSetSkipAfterFailureCount() {
        SurefireReflector reflector =
                new SurefireReflector(Thread.currentThread().getContextClassLoader());
        Foo booterParams = (Foo) getFoo();
        assertEquals(0, booterParams.getSkipAfterFailureCount());
        reflector.setSkipAfterFailureCount(booterParams, 5);
        assertEquals(5, booterParams.getSkipAfterFailureCount());
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test
    public void testSetSystemExitTimeout() {
        SurefireReflector reflector =
                new SurefireReflector(Thread.currentThread().getContextClassLoader());
        Foo booterParams = (Foo) getFoo();
        assertNull(booterParams.getSystemExitTimeout());
        reflector.setSystemExitTimeout(booterParams, 60);
        assertEquals((Integer) 60, booterParams.getSystemExitTimeout());
    }

    @Test
    public void testSetProviderPropertiesAware() {
        SurefireReflector reflector =
                new SurefireReflector(Thread.currentThread().getContextClassLoader());
        Foo booterParams = (Foo) getFoo();
        reflector.setProviderPropertiesAware(booterParams, Collections.singletonMap("k", "v"));
        assertTrue(booterParams.isCalled());
        assertNotNull(booterParams.getProviderProperties());
        assertEquals(1, booterParams.getProviderProperties().size());
        assertEquals("v", booterParams.getProviderProperties().get("k"));
    }

    private SurefireReflector getReflector() {
        return new SurefireReflector(getClass().getClassLoader());
    }

    private Object getFoo() { // Todo: Setup a different classloader so we can really test crossing
        return new Foo();
    }

    private Boolean isCalled(Object foo) {
        final Method isCalled;
        try {
            isCalled = foo.getClass().getMethod("isCalled");
            return (Boolean) isCalled.invoke(foo);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     */
    public static final class DummyProvider implements SurefireProvider {
        public DummyProvider(ProviderParameters providerParameters) {}

        @Override
        public Iterable<Class<?>> getSuites() {
            return null;
        }

        @Override
        public RunResult invoke(Object forkTestSet) {
            return null;
        }

        @Override
        public void cancel() {}
    }
}
