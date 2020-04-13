package org.apache.maven.surefire.junitcore;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.surefire.booter.BaseProviderFactory;
import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.junit4.MockReporter;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.TestsToRun;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import static junit.framework.Assert.assertEquals;

/**
 * {@code
 * <dependency>
 * <groupId>junit</groupId>
 * <artifactId>junit</artifactId>
 * <version>4.8.1</version>
 * <scope>test</scope>
 * </dependency>
 * <br>
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
    @SuppressWarnings( "checkstyle:methodname" )
    public void surefireIsConfused_ByMultipleIgnore_OnClassLevel() throws Exception
    {
        ReporterFactory reporterFactory = JUnitCoreTester.defaultNoXml();
        BaseProviderFactory providerParameters = new BaseProviderFactory( true );
        providerParameters.setReporterFactory( reporterFactory );

        providerParameters.setReporterConfiguration( new ReporterConfiguration( new File( "" ), false ) );
        Map<String, String> junitProps = new HashMap<>();
        junitProps.put( ProviderParameterNames.PARALLEL_PROP, "none" );

        JUnitCoreParameters jUnitCoreParameters = new JUnitCoreParameters( junitProps );

        final Map<String, TestSet> testSetMap = new ConcurrentHashMap<>();

        RunListener listener = ConcurrentRunListener.createInstance( testSetMap, reporterFactory, false, false,
                new DefaultDirectConsoleReporter( System.out ) );

        TestsToRun testsToRun = new TestsToRun( Collections.<Class<?>>singleton( TestClassTest.class ) );

        org.junit.runner.notification.RunListener jUnit4RunListener = new JUnitCoreRunListener( listener, testSetMap );

        List<org.junit.runner.notification.RunListener> customRunListeners = new ArrayList<>();
        customRunListeners.add( 0, jUnit4RunListener );

        try
        {
            // JUnitCoreWrapper#execute() is calling JUnit4RunListener#rethrowAnyTestMechanismFailures()
            // and rethrows a failure which happened in listener
            exception.expect( TestSetFailedException.class );
            JUnit4RunListener dummy = new JUnit4RunListener( new MockReporter() );
            new JUnitCoreWrapper( new Notifier( dummy, 0 ), jUnitCoreParameters,
                    new DefaultDirectConsoleReporter( System.out ) ).execute( testsToRun, customRunListeners, null );
        }
        finally
        {
            RunResult result = reporterFactory.close();
            assertEquals( "JUnit should report correctly number of test ran(Finished)", 1,
                    result.getCompletedCount() );
        }
    }

    /**
     *
     */
    @RunWith( TestCaseRunner.class )
    public static class TestClassTest
    {
        @Test
        public void shouldNeverBeCalled() throws Exception
        {
        }
    }

    /**
     *
     */
    public static class TestCaseRunner extends BlockJUnit4ClassRunner
    {
        public TestCaseRunner( Class<?> klass ) throws InitializationError
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

    private static class TestRunListener extends org.junit.runner.notification.RunListener
    {
        @Override
        public void testFinished( Description description ) throws Exception
        {
            throw new RuntimeException(
                    "This Exception will cause Surefire to receive an internal JUnit Description and fail." );
        }
    }
}