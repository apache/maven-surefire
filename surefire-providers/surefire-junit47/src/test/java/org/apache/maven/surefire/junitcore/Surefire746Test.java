package org.apache.maven.surefire.junitcore;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.booter.BaseProviderFactory;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.DefaultConsoleReporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;

import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * {@code
 * <dependency>
 * <groupId>junit</groupId>
 * <artifactId>junit</artifactId>
 * <version>4.8.1</version>
 * <scope>test</scope>
 * </dependency>
 * <p/>
 * <dependency>
 * <groupId>org.apache.maven.surefire</groupId>
 * <artifactId>surefire-booter</artifactId>
 * <version>2.8.1</version>
 * <scope>test</scope>
 * </dependency>
 * <dependency>
 * <groupId>org.apache.maven.plugins</groupId>
 * <artifactId>maven-surefire-plugin</artifactId>
 * <version>2.8.1</version>
 * <scope>test</scope>
 * </dependency>
 * <dependency>
 * <groupId>org.apache.maven.surefire</groupId>
 * <artifactId>surefire-junit47</artifactId>
 * <version>2.8.1</version>
 * <scope>test</scope>
 * </dependency>
 * }
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class Surefire746Test
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void surefireIsConfused_ByMultipleIgnore_OnClassLevel()
        throws Exception
    {
        ReporterFactory reporterFactory = DefaultReporterFactory.defaultNoXml();
        BaseProviderFactory providerParameters = new BaseProviderFactory( reporterFactory, true );
        ConsoleLogger consoleLogger = new DefaultConsoleReporter( System.out );

        providerParameters.setReporterConfiguration( new ReporterConfiguration( new File( "" ), false ) );
        Properties junitProps = new Properties();
        junitProps.put( ProviderParameterNames.PARALLEL_PROP, "none" );

        JUnitCoreParameters jUnitCoreParameters = new JUnitCoreParameters( junitProps );

        final Map<String, TestSet> testSetMap = new ConcurrentHashMap<String, TestSet>();

        RunListener listener =
            ConcurrentRunListener.createInstance( testSetMap, reporterFactory, false, false, consoleLogger );

        TestsToRun testsToRun = new TestsToRun( Arrays.<Class>asList( TestClassTest.class ) );

        org.junit.runner.notification.RunListener jUnit4RunListener = new JUnitCoreRunListener( listener, testSetMap );

        List<org.junit.runner.notification.RunListener> customRunListeners =
            new ArrayList<org.junit.runner.notification.RunListener>();
        customRunListeners.add( 0, jUnit4RunListener );

        try
        {
            // JUnitCoreWrapper#execute() is calling JUnit4RunListener#rethrowAnyTestMechanismFailures()
            // and rethrows a failure which happened in listener
            exception.expect( TestSetFailedException.class );
            JUnitCoreWrapper.execute( testsToRun, jUnitCoreParameters, customRunListeners, null );
        }
        finally
        {
            RunResult result = reporterFactory.close();
            Assert.assertEquals( "JUnit should report correctly number of test ran(Finished)",
                    1, result.getCompletedCount() );
        }
    }

    @RunWith( TestCaseRunner.class )
    public static class TestClassTest
    {
        @Test
        public void shouldNeverBeCalled()
            throws Exception
        {
            Assert.assertTrue( true );
        }
    }

    public static class TestCaseRunner
        extends BlockJUnit4ClassRunner
    {
        public TestCaseRunner( Class<?> klass )
            throws InitializationError
        {
            super( klass );
        }

        @Override
        public void run( RunNotifier notifier )
        {
            notifier.addListener( new TestRunListener() );
            super.run( notifier );
        }

    }

    private static class TestRunListener
        extends org.junit.runner.notification.RunListener
    {
        @Override
        public void testFinished( Description description )
            throws Exception
        {
            throw new RuntimeException(
                "This Exception will cause Surefire to receive an internal JUnit Description and fail." );
        }
    }
}