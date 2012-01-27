package org.apache.maven.surefire.testng;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testng.conf.Configurator;
import org.apache.maven.surefire.testng.conf.TestNG4751Configurator;
import org.apache.maven.surefire.testng.conf.TestNG52Configurator;
import org.apache.maven.surefire.testng.conf.TestNGMapConfigurator;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.apache.maven.surefire.util.internal.StringUtils;

import org.testng.TestNG;

/**
 * Contains utility methods for executing TestNG.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGExecutor
{


    private TestNGExecutor()
    {
        // noop
    }

    public static void run( Class[] testClasses, String testSourceDirectory, Map options, ArtifactVersion version,
                            RunListener reportManager, TestNgTestSuite suite, File reportsDirectory,
                            final String methodNamePattern )
        throws TestSetFailedException
    {
        TestNG testng = new TestNG( true );

        applyGroupMatching( testng, options );
        if ( !StringUtils.isBlank( methodNamePattern ) )
        {
            applyMethodNameFiltering( testng, methodNamePattern );
        }

        Configurator configurator = getConfigurator( version );
        System.out.println( "Configuring TestNG with: " + configurator );
        configurator.configure( testng, options );
        postConfigure( testng, testSourceDirectory, reportManager, suite, reportsDirectory );
        testng.setTestClasses( testClasses );
        testng.run();
    }

    private static void applyMethodNameFiltering( TestNG testng, String methodNamePattern )
        throws TestSetFailedException
    {
        // the class is available in the testClassPath
        String clazzName = "org.apache.maven.surefire.testng.utils.MethodSelector";
        // looks to need a high value 
        testng.addMethodSelector( clazzName, 10000 );
        try
        {
            Class clazz = Class.forName( clazzName );

            Method method = clazz.getMethod( "setMethodName", new Class[]{ String.class } );
            method.invoke( null, new Object[]{ methodNamePattern } );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( SecurityException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
    }

    private static void applyGroupMatching( TestNG testng, Map options )
        throws TestSetFailedException
    {
        String groups = (String) options.get( ProviderParameterNames.TESTNG_GROUPS_PROP );
        String excludedGroups = (String) options.get( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP );

        if ( groups == null && excludedGroups == null )
        {
            return;
        }

        // the class is available in the testClassPath
        String clazzName = "org.apache.maven.surefire.testng.utils.GroupMatcherMethodSelector";
        // looks to need a high value
        testng.addMethodSelector( clazzName, 9999 );
        try
        {
            Class clazz = Class.forName( clazzName );

            // HORRIBLE hack, but TNG doesn't allow us to setup a method selector instance directly.
            Method method = clazz.getMethod( "setGroups", new Class[]{ String.class, String.class } );
            method.invoke( null, new Object[]{ groups, excludedGroups } );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( SecurityException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }
    }

    public static void run( List suiteFiles, String testSourceDirectory, Map options, ArtifactVersion version,
                            RunListener reportManager, TestNgTestSuite suite, File reportsDirectory )
        throws TestSetFailedException
    {
        TestNG testng = new TestNG( true );
        Configurator configurator = getConfigurator( version );
        configurator.configure( testng, options );
        postConfigure( testng, testSourceDirectory, reportManager, suite, reportsDirectory );
        testng.setTestSuites( suiteFiles );
        testng.run();
    }

    private static Configurator getConfigurator( ArtifactVersion version )
        throws TestSetFailedException
    {
        try
        {
            VersionRange range = VersionRange.createFromVersionSpec( "[4.7,5.1]" );
            if ( range.containsVersion( version ) )
            {
                return new TestNG4751Configurator();
            }
            range = VersionRange.createFromVersionSpec( "[5.2]" );
            if ( range.containsVersion( version ) )
            {
                return new TestNG52Configurator();
            }
            range = VersionRange.createFromVersionSpec( "[5.3,)" );
            if ( range.containsVersion( version ) )
            {
                return new TestNGMapConfigurator();
            }

            throw new TestSetFailedException( "Unknown TestNG version " + version );
        }
        catch ( InvalidVersionSpecificationException invsex )
        {
            throw new TestSetFailedException( "Bug in plugin. Please report it with the attached stacktrace", invsex );
        }
    }


    private static void postConfigure( TestNG testNG, String sourcePath, RunListener reportManager,
                                       TestNgTestSuite suite, File reportsDirectory )
        throws TestSetFailedException
    {
        // turn off all TestNG output
        testNG.setVerbose( 0 );

        TestNGReporter reporter = createTestNGReporter( reportManager, suite );
        testNG.addListener( (Object) reporter );

        // FIXME: use classifier to decide if we need to pass along the source dir (onyl for JDK14)
        if ( sourcePath != null )
        {
            testNG.setSourcePath( sourcePath );
        }

        testNG.setOutputDirectory( reportsDirectory.getAbsolutePath() );
    }

    // If we have access to IResultListener, return a ConfigurationAwareTestNGReporter
    // But don't cause NoClassDefFoundErrors if it isn't available; just return a regular TestNGReporter instead
    private static TestNGReporter createTestNGReporter( RunListener reportManager, TestNgTestSuite suite )
    {
        try
        {
            Class.forName( "org.testng.internal.IResultListener" );
            Class c = Class.forName( "org.apache.maven.surefire.testng.ConfigurationAwareTestNGReporter" );
            try
            {
                Constructor ctor = c.getConstructor( new Class[]{ RunListener.class, TestNgTestSuite.class } );
                return (TestNGReporter) ctor.newInstance( new Object[]{ reportManager, suite } );
            }
            catch ( Exception e )
            {
                throw new NestedRuntimeException( "Bug in ConfigurationAwareTestNGReporter", e );
            }
        }
        catch ( ClassNotFoundException e )
        {
            return new TestNGReporter( reportManager );
        }
    }

}
