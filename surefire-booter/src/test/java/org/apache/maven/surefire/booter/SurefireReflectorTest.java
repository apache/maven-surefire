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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.RunOrder;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class SurefireReflectorTest
    extends TestCase
{
    public void testSetDirectoryScannerParameters()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        DirectoryScannerParameters directoryScannerParameters =
            new DirectoryScannerParameters( new File( "ABC" ), new ArrayList<String>(), new ArrayList<String>(),
                                            new ArrayList<String>(), false, "hourly" );
        surefireReflector.setDirectoryScannerParameters( foo, directoryScannerParameters );
        assertTrue( isCalled( foo ) );

    }

    public void testRunOrderParameters()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        RunOrderParameters runOrderParameters = new RunOrderParameters( RunOrder.DEFAULT, new File( "." ) );
        surefireReflector.setRunOrderParameters( foo, runOrderParameters );
        assertTrue( isCalled( foo ) );

    }

    public void testTestSuiteDefinition()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestRequest testSuiteDefinition =
            new TestRequest( Arrays.asList( new File( "file1" ), new File( "file2" ) ),
                             new File( "TestSOurce" ), new TestListResolver( "aUserRequestedTest#aMethodRequested" ) );
        surefireReflector.setTestSuiteDefinition( foo, testSuiteDefinition );
        assertTrue( isCalled( foo ) );
    }

    public void testProviderProperties()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setProviderProperties( foo, new HashMap<String, String>() );
        assertTrue( isCalled( foo ) );
    }

    public void testReporterConfiguration()
        throws Exception
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

    public void testTestClassLoaderAware()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        surefireReflector.setTestClassLoader( foo, getClass().getClassLoader() );
        assertTrue( isCalled( foo ) );
    }

    public void testArtifactInfoAware()
        throws Exception
    {
        SurefireReflector surefireReflector = getReflector();
        Object foo = getFoo();

        TestArtifactInfo testArtifactInfo = new TestArtifactInfo( "12.3", "test" );
        surefireReflector.setTestArtifactInfo( foo, testArtifactInfo );
        assertTrue( isCalled( foo ) );
    }

    private SurefireReflector getReflector()
    {
        return new SurefireReflector( this.getClass().getClassLoader() );
    }

    public Object getFoo()
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
}
