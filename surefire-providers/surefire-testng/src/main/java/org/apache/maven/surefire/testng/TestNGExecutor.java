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

import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testng.conf.Configurator;
import org.apache.maven.surefire.testng.utils.FailFastEventsSingleton;
import org.apache.maven.surefire.testng.utils.FailFastListener;
import org.apache.maven.surefire.testng.utils.Stoppable;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.internal.StringUtils;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlMethodSelector;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.surefire.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.cli.CommandLineOption.SHOW_ERRORS;
import static org.apache.maven.surefire.util.ReflectionUtils.instantiate;
import static org.apache.maven.surefire.util.ReflectionUtils.tryLoadClass;
import static org.apache.maven.surefire.util.internal.ConcurrencyUtils.countDownToZero;

/**
 * Contains utility methods for executing TestNG.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
final class TestNGExecutor
{
    /** The default name for a suite launched from the maven surefire plugin */
    private static final String DEFAULT_SUREFIRE_SUITE_NAME = "Surefire suite";

    /** The default name for a test launched from the maven surefire plugin */
    private static final String DEFAULT_SUREFIRE_TEST_NAME = "Surefire test";

    private static final boolean HAS_TEST_ANNOTATION_ON_CLASSPATH =
            tryLoadClass( TestNGExecutor.class.getClassLoader(), "org.testng.annotations.Test" ) != null;

    private TestNGExecutor()
    {
        throw new IllegalStateException( "not instantiable constructor" );
    }

    @SuppressWarnings( "checkstyle:parameternumbercheck" )
    static void run( Iterable<Class<?>> testClasses, String testSourceDirectory,
                            Map<String, String> options, // string,string because TestNGMapConfigurator#configure()
                            RunListener reportManager, File reportsDirectory,
                            TestListResolver methodFilter, List<CommandLineOption> mainCliOptions,
                            int skipAfterFailureCount )
        throws TestSetFailedException
    {
        TestNG testng = new TestNG( true );

        Configurator configurator = getConfigurator( options.get( "testng.configurator" ) );

        if ( isCliDebugOrShowErrors( mainCliOptions ) )
        {
            System.out.println( "Configuring TestNG with: " + configurator.getClass().getSimpleName() );
        }

        XmlMethodSelector groupMatchingSelector = createGroupMatchingSelector( options );
        XmlMethodSelector methodNameFilteringSelector = createMethodNameFilteringSelector( methodFilter );

        Map<String, SuiteAndNamedTests> suitesNames = new HashMap<>();

        List<XmlSuite> xmlSuites = new ArrayList<>();
        for ( Class<?> testClass : testClasses )
        {
            TestMetadata metadata = findTestMetadata( testClass );

            SuiteAndNamedTests suiteAndNamedTests = suitesNames.get( metadata.suiteName );
            if ( suiteAndNamedTests == null )
            {
                suiteAndNamedTests = new SuiteAndNamedTests();
                suiteAndNamedTests.xmlSuite.setName( metadata.suiteName );
                configurator.configure( suiteAndNamedTests.xmlSuite, options );
                xmlSuites.add( suiteAndNamedTests.xmlSuite );

                suitesNames.put( metadata.suiteName, suiteAndNamedTests );
            }

            XmlTest xmlTest = suiteAndNamedTests.testNameToTest.get( metadata.testName );
            if ( xmlTest == null )
            {
                xmlTest = new XmlTest( suiteAndNamedTests.xmlSuite );
                xmlTest.setName( metadata.testName );
                addSelector( xmlTest, groupMatchingSelector );
                addSelector( xmlTest, methodNameFilteringSelector );
                xmlTest.setXmlClasses( new ArrayList<XmlClass>() );

                suiteAndNamedTests.testNameToTest.put( metadata.testName, xmlTest );
            }

            xmlTest.getXmlClasses().add( new XmlClass( testClass.getName() ) );
        }

        testng.setXmlSuites( xmlSuites );
        configurator.configure( testng, options );
        postConfigure( testng, testSourceDirectory, reportManager, reportsDirectory, skipAfterFailureCount,
                       extractVerboseLevel( options ) );
        testng.run();
    }

    private static boolean isCliDebugOrShowErrors( List<CommandLineOption> mainCliOptions )
    {
        return mainCliOptions.contains( LOGGING_LEVEL_DEBUG ) || mainCliOptions.contains( SHOW_ERRORS );
    }

    private static TestMetadata findTestMetadata( Class<?> testClass )
    {
        TestMetadata result = new TestMetadata();
        if ( HAS_TEST_ANNOTATION_ON_CLASSPATH )
        {
            Test testAnnotation = findAnnotation( testClass, Test.class );
            if ( null != testAnnotation )
            {
                if ( !StringUtils.isBlank( testAnnotation.suiteName() ) )
                {
                    result.suiteName = testAnnotation.suiteName();
                }

                if ( !StringUtils.isBlank( testAnnotation.testName() ) )
                {
                    result.testName = testAnnotation.testName();
                }
            }
        }
        return result;
    }

    private static <T extends Annotation> T findAnnotation( Class<?> clazz, Class<T> annotationType )
    {
        if ( clazz == null )
        {
            return null;
        }

        T result = clazz.getAnnotation( annotationType );
        if ( result != null )
        {
            return result;
        }

        return findAnnotation( clazz.getSuperclass(), annotationType );
    }

    private static class TestMetadata
    {
        private String testName = DEFAULT_SUREFIRE_TEST_NAME;

        private String suiteName = DEFAULT_SUREFIRE_SUITE_NAME;
    }

    private static class SuiteAndNamedTests
    {
        private XmlSuite xmlSuite = new XmlSuite();

        private Map<String, XmlTest> testNameToTest = new HashMap<>();
    }

    private static void addSelector( XmlTest xmlTest, XmlMethodSelector selector )
    {
        if ( selector != null )
        {
            xmlTest.getMethodSelectors().add( selector );
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private static XmlMethodSelector createMethodNameFilteringSelector( TestListResolver methodFilter )
        throws TestSetFailedException
    {
        if ( methodFilter != null && !methodFilter.isEmpty() )
        {
            // the class is available in the testClassPath
            String clazzName = "org.apache.maven.surefire.testng.utils.MethodSelector";
            try
            {
                Class<?> clazz = Class.forName( clazzName );
                Method method = clazz.getMethod( "setTestListResolver", TestListResolver.class );
                method.invoke( null, methodFilter );
            }
            catch ( Exception e )
            {
                throw new TestSetFailedException( e.getMessage(), e );
            }

            XmlMethodSelector xms = new XmlMethodSelector();

            xms.setName( clazzName );
            // looks to need a high value
            xms.setPriority( 10000 );

            return xms;
        }
        else
        {
            return null;
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private static XmlMethodSelector createGroupMatchingSelector( Map<String, String> options )
        throws TestSetFailedException
    {
        final String groups = options.get( ProviderParameterNames.TESTNG_GROUPS_PROP );
        final String excludedGroups = options.get( ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP );

        if ( groups == null && excludedGroups == null )
        {
            return null;
        }

        // the class is available in the testClassPath
        final String clazzName = "org.apache.maven.surefire.testng.utils.GroupMatcherMethodSelector";
        try
        {
            Class<?> clazz = Class.forName( clazzName );

            // HORRIBLE hack, but TNG doesn't allow us to setup a method selector instance directly.
            Method method = clazz.getMethod( "setGroups", String.class, String.class );
            method.invoke( null, groups, excludedGroups );
        }
        catch ( Exception e )
        {
            throw new TestSetFailedException( e.getMessage(), e );
        }

        XmlMethodSelector xms = new XmlMethodSelector();

        xms.setName( clazzName );
        // looks to need a high value
        xms.setPriority( 9999 );

        return xms;
    }

    static void run( List<String> suiteFiles, String testSourceDirectory,
                            Map<String, String> options, // string,string because TestNGMapConfigurator#configure()
                            RunListener reportManager, File reportsDirectory, int skipAfterFailureCount )
        throws TestSetFailedException
    {
        TestNG testng = new TestNG( true );
        Configurator configurator = getConfigurator( options.get( "testng.configurator" ) );
        configurator.configure( testng, options );
        postConfigure( testng, testSourceDirectory, reportManager, reportsDirectory, skipAfterFailureCount,
                       extractVerboseLevel( options ) );
        testng.setTestSuites( suiteFiles );
        testng.run();
    }

    private static Configurator getConfigurator( String className )
    {
        try
        {
            return (Configurator) Class.forName( className ).newInstance();
        }
        catch ( ReflectiveOperationException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static void postConfigure( TestNG testNG, String sourcePath, final RunListener reportManager,
                                       File reportsDirectory, int skipAfterFailureCount, int verboseLevel )
    {
        // 0 (default): turn off all TestNG output
        testNG.setVerbose( verboseLevel );

        TestNGReporter reporter = createTestNGReporter( reportManager );
        testNG.addListener( (Object) reporter );

        if ( skipAfterFailureCount > 0 )
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            testNG.addListener( instantiate( cl, "org.apache.maven.surefire.testng.utils.FailFastNotifier",
                                             Object.class ) );
            testNG.addListener( new FailFastListener( createStoppable( reportManager, skipAfterFailureCount ) ) );
        }

        // FIXME: use classifier to decide if we need to pass along the source dir (only for JDK14)
        if ( sourcePath != null )
        {
            testNG.setSourcePath( sourcePath );
        }

        testNG.setOutputDirectory( reportsDirectory.getAbsolutePath() );
    }

    private static Stoppable createStoppable( final RunListener reportManager, int skipAfterFailureCount )
    {
        final AtomicInteger currentFaultCount = new AtomicInteger( skipAfterFailureCount );

        return new Stoppable()
        {
            @Override
            public void fireStopEvent()
            {
                if ( countDownToZero( currentFaultCount ) )
                {
                    FailFastEventsSingleton.getInstance().setSkipOnNextTest();
                }

                reportManager.testExecutionSkippedByUser();
            }
        };
    }

    // If we have access to IResultListener, return a ConfigurationAwareTestNGReporter
    // But don't cause NoClassDefFoundErrors if it isn't available; just return a regular TestNGReporter instead
    private static TestNGReporter createTestNGReporter( RunListener reportManager )
    {
        try
        {
            Class.forName( "org.testng.internal.IResultListener" );
            Class c = Class.forName( "org.apache.maven.surefire.testng.ConfigurationAwareTestNGReporter" );
            @SuppressWarnings( "unchecked" ) Constructor<?> ctor = c.getConstructor( RunListener.class );
            return (TestNGReporter) ctor.newInstance( reportManager );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( "Bug in ConfigurationAwareTestNGReporter", e.getCause() );
        }
        catch ( ClassNotFoundException e )
        {
            return new TestNGReporter( reportManager );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Bug in ConfigurationAwareTestNGReporter", e );
        }
    }

    private static int extractVerboseLevel( Map<String, String> options )
        throws TestSetFailedException
    {
        try
        {
            String verbose = options.get( "surefire.testng.verbose" );
            return verbose == null ? 0 : Integer.parseInt( verbose );
        }
        catch ( NumberFormatException e )
        {
            throw new TestSetFailedException( "Provider property 'surefire.testng.verbose' should refer to "
                                                  + "number -1 (debug mode), 0, 1 .. 10 (most detailed).", e );
        }
    }
}
