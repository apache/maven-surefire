package org.apache.maven.surefire.booter;

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
import org.apache.maven.surefire.api.booter.BaseProviderFactory;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.provider.SurefireProvider;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.util.RunOrder;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.apache.maven.surefire.api.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.api.cli.CommandLineOption.SHOW_ERRORS;

/**
 *
 */
public class SurefireReflectorTest
        extends TestCase
{
    public void testShouldCreateFactoryWithoutException()
    {
        ReporterFactory factory = new ReporterFactory()
        {
            @Override
            public TestReportListener createTestReportListener()
            {
                return null;
            }

            @Override
            public RunResult close()
            {
                return null;
            }
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        SurefireReflector reflector = new SurefireReflector( cl );
        BaseProviderFactory bpf = (BaseProviderFactory) reflector.createBooterConfiguration( cl, true );
        bpf.setReporterFactory( factory );
        assertNotNull( bpf.getReporterFactory() );
        assertSame( factory, bpf.getReporterFactory() );
    }

    public void testSetDirectoryScannerParameters()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        DirectoryScannerParameters directoryScannerParameters =
                new DirectoryScannerParameters( new File( "ABC" ), new ArrayList<String>(), new ArrayList<String>(),
                        new ArrayList<String>(), "hourly" );
        surefireReflector.setDirectoryScannerParameters( foo, directoryScannerParameters );
        assertTrue( isCalled( foo ) );
        assertNotNull( ( (Foo) foo ).getDirectoryScannerParameters() );
    }

    public void testNullSetDirectoryScannerParameters()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setDirectoryScannerParameters( foo, null );
        assertTrue( isCalled( foo ) );
        assertNull( ( (Foo) foo ).getDirectoryScannerParameters() );
    }

    public void testSetIfDirScannerAware()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        DirectoryScannerParameters directoryScannerParameters =
            new DirectoryScannerParameters( new File( "ABC" ), new ArrayList<String>(), new ArrayList<String>(),
                new ArrayList<String>(), "hourly" );
        surefireReflector.setIfDirScannerAware( foo, directoryScannerParameters );
        assertTrue( isCalled( foo ) );
    }

    public void testRunOrderParameters()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        RunOrderParameters runOrderParameters = new RunOrderParameters( RunOrder.DEFAULT, new File( "." ) );
        surefireReflector.setRunOrderParameters( foo, runOrderParameters );
        assertTrue( isCalled( foo ) );
    }

    public void testRunOrderParametersWithRunOrderRandomSeed()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        // Arbitrary random seed that should be ignored because RunOrder is not RANDOM
        Long runOrderRandomSeed = 5L;

        RunOrderParameters runOrderParameters = new RunOrderParameters( RunOrder.DEFAULT, new File( "." ),
                                                                        runOrderRandomSeed );
        surefireReflector.setRunOrderParameters( foo, runOrderParameters );
        assertTrue( isCalled( foo ) );
    }

    public void testNullRunOrderParameters()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setRunOrderParameters( foo, null );
        assertTrue( isCalled( foo ) );
        try
        {
            ( (Foo) foo ).getRunOrderCalculator();
        }
        catch ( NullPointerException e )
        {
            return;
        }
        fail();
    }

    public void testTestSuiteDefinition()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestRequest testSuiteDefinition =
                new TestRequest( asList( new File( "file1" ), new File( "file2" ) ),
                        new File( "TestSOurce" ), new TestListResolver( "aUserRequestedTest#aMethodRequested" ) );
        surefireReflector.setTestSuiteDefinition( foo, testSuiteDefinition );
        assertTrue( isCalled( foo ) );
        assertNotNull( ( (Foo) foo ).getTestRequest() );
    }

    public void testNullTestSuiteDefinition()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();
        surefireReflector.setTestSuiteDefinition( foo, null );
        assertTrue( isCalled( foo ) );
        assertNull( ( (Foo) foo ).getTestRequest() );
    }

    public void testProviderProperties()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setProviderProperties( foo, new HashMap<String, String>() );
        assertTrue( isCalled( foo ) );
    }

    public void testReporterConfiguration()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        ReporterConfiguration reporterConfiguration = getReporterConfiguration();
        surefireReflector.setReporterConfigurationAware( foo, reporterConfiguration );
        assertTrue( isCalled( foo ) );
    }

    private ReporterConfiguration getReporterConfiguration()
    {
        return new ReporterConfiguration( new File( "CDE" ), true );
    }

    public void testTestClassLoader()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestClassLoader( foo, getClass().getClassLoader() );
        assertTrue( isCalled( foo ) );
    }

    public void testTestClassLoaderAware()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestClassLoaderAware( foo, getClass().getClassLoader() );
        assertTrue( isCalled( foo ) );
    }

    public void testArtifactInfo()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestArtifactInfo testArtifactInfo = new TestArtifactInfo( "12.3", "test" );
        surefireReflector.setTestArtifactInfo( foo, testArtifactInfo );
        assertTrue( isCalled( foo ) );
    }

    public void testNullArtifactInfo()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestArtifactInfo( foo, null );
        assertTrue( isCalled( foo ) );
        assertNull( ( (Foo) foo ).getTestArtifactInfo() );
    }

    public void testArtifactInfoAware()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestArtifactInfo testArtifactInfo = new TestArtifactInfo( "12.3", "test" );
        surefireReflector.setTestArtifactInfoAware( foo, testArtifactInfo );
        assertTrue( isCalled( foo ) );
        assertEquals( testArtifactInfo.getClassifier(), "test" );
        assertEquals( testArtifactInfo.getVersion(), "12.3" );
    }

    public void testReporterFactory()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        ReporterFactory reporterFactory = new ReporterFactory()
        {
            @Override
            public TestReportListener createTestReportListener()
            {
                return null;
            }

            @Override
            public RunResult close()
            {
                return null;
            }
        };

        surefireReflector.setReporterFactory( foo, reporterFactory );
        assertTrue( isCalled( foo ) );
    }

    public void testReporterFactoryAware()
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        ReporterFactory reporterFactory = new ReporterFactory()
        {
            @Override
            public TestReportListener createTestReportListener()
            {
                return null;
            }

            @Override
            public RunResult close()
            {
                return null;
            }
        };

        surefireReflector.setReporterFactoryAware( foo, reporterFactory );
        assertTrue( isCalled( foo ) );
        assertSame( ( (Foo) foo ).getReporterFactory(), reporterFactory );
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testConvertIfRunResult()
    {
        RunResult runResult = new RunResult( 20, 1, 2, 3, 4, "IOException", true );
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        RunResult obj = (RunResult) reflector.convertIfRunResult( runResult );
        assertEquals( obj.getCompletedCount(), 20 );
        assertEquals( obj.getErrors(), 1 );
        assertEquals( obj.getFailures(), 2 );
        assertEquals( obj.getSkipped(), 3 );
        assertFalse( obj.isErrorFree() );
        assertFalse( obj.isInternalError() );
        assertEquals( obj.getFailsafeCode(), (Integer) RunResult.FAILURE );

        assertNull( reflector.convertIfRunResult( null ) );
        assertEquals( reflector.convertIfRunResult( "" ), "" );
    }

    public void testInstantiateProvider()
    {
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        Object booterParams = getFoo();
        Object provider = reflector.instantiateProvider( DummyProvider.class.getName(), booterParams );
        assertNotNull( provider );
        assertEquals( provider.getClass(), DummyProvider.class );
    }

    public void testSetMainCliOptions()
    {
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        Object booterParams = getFoo();
        reflector.setMainCliOptions( booterParams, asList( SHOW_ERRORS, LOGGING_LEVEL_DEBUG ) );
        assertEquals( ( (BaseProviderFactory) booterParams ).getMainCliOptions().size(), 2 );
        assertEquals( ( (BaseProviderFactory) booterParams ).getMainCliOptions().get( 0 ), SHOW_ERRORS );
        assertEquals( ( (BaseProviderFactory) booterParams ).getMainCliOptions().get( 1 ), LOGGING_LEVEL_DEBUG );
    }

    public void testSetSkipAfterFailureCount()
    {
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        Foo booterParams = (Foo) getFoo();
        assertEquals( booterParams.getSkipAfterFailureCount(), 0 );
        reflector.setSkipAfterFailureCount( booterParams, 5 );
        assertEquals( booterParams.getSkipAfterFailureCount(), 5 );
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testSetSystemExitTimeout()
    {
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        Foo booterParams = (Foo) getFoo();
        assertNull( booterParams.getSystemExitTimeout() );
        reflector.setSystemExitTimeout( booterParams, 60 );
        assertEquals( booterParams.getSystemExitTimeout(), (Integer) 60 );
    }

    public void testSetTestSuiteDefinitionAware()
    {
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        Foo booterParams = (Foo) getFoo();
        TestRequest request = new TestRequest( Collections.emptyList(), null, null );
        reflector.setTestSuiteDefinitionAware( booterParams, request );
        assertTrue( booterParams.isCalled() );
        assertNotNull( booterParams.getTestRequest() );
        assertTrue( booterParams.getTestRequest().getSuiteXmlFiles().isEmpty() );
        assertNull( booterParams.getTestRequest().getTestSourceDirectory() );
        assertNull( booterParams.getTestRequest().getTestListResolver() );
        assertEquals( booterParams.getTestRequest().getRerunFailingTestsCount(), 0 );
    }

    public void testSetProviderPropertiesAware()
    {
        SurefireReflector reflector = new SurefireReflector( Thread.currentThread().getContextClassLoader() );
        Foo booterParams = (Foo) getFoo();
        reflector.setProviderPropertiesAware( booterParams, Collections.singletonMap( "k", "v" ) );
        assertTrue( booterParams.isCalled() );
        assertNotNull( booterParams.getProviderProperties() );
        assertEquals( booterParams.getProviderProperties().size(), 1 );
        assertEquals( booterParams.getProviderProperties().get( "k" ), "v" );
    }

    private SurefireReflector getReflector()
    {
        return new SurefireReflector( getClass().getClassLoader() );
    }

    private Object getFoo()
    { // Todo: Setup a different classloader so we can really test crossing
        return new Foo();
    }

    private Boolean isCalled( Object foo )
    {
        final Method isCalled;
        try
        {
            isCalled = foo.getClass().getMethod( "isCalled" );
            return (Boolean) isCalled.invoke( foo );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     *
     */
    public static final class DummyProvider implements SurefireProvider
    {
        public DummyProvider( ProviderParameters providerParameters )
        {
        }

        @Override
        public Iterable<Class<?>> getSuites()
        {
            return null;
        }

        @Override
        public RunResult invoke( Object forkTestSet )
        {
            return null;
        }

        @Override
        public void cancel()
        {

        }
    }
}
