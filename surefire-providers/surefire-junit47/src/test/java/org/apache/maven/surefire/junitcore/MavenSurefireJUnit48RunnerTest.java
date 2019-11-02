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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * TestCase that expose "No tests were executed!" on Test failure using Maven Surefire 2.6-SNAPSHOT
 * and the JUnit 4.8 Runner.
 * {@code
 * * <br>
 * -------------------------------------------------------
 * T E S T S
 * -------------------------------------------------------
 * <br>
 * Results:
 * <br>
 * Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
 * <br>
 * [INFO] ------------------------------------------------------------------------
 * [INFO] BUILD FAILURE
 * [INFO] ------------------------------------------------------------------------
 * [INFO] Total time: 11.011s
 * [INFO] Finished at: Thu Jul 15 13:59:14 CEST 2010
 * [INFO] Final Memory: 24M/355M
 * [INFO] ------------------------------------------------------------------------
 * [ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:2.5:test
 * (default-test) on project xxxxxx: No tests were executed!  (Set -DfailIfNoTests=false to
 * ignore this error.) -> [Help 1]
 * <br>
 * <br>
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
 * <version>2.6-SNAPSHOT</version>
 * <scope>test</scope>
 * </dependency>
 * <dependency>
 * <groupId>org.apache.maven.plugins</groupId>
 * <artifactId>maven-surefire-plugin</artifactId>
 * <version>2.6-SNAPSHOT</version>
 * <scope>test</scope>
 * </dependency>
 * <dependency>
 * <groupId>org.apache.maven.surefire</groupId>
 * <artifactId>surefire-junit48</artifactId>
 * <version>2.6-SNAPSHOT</version>
 * <scope>test</scope>
 * </dependency>
 * }
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class MavenSurefireJUnit48RunnerTest
    extends TestCase
{

    /*
    * Assumption:
    * The ConcurrentReportingRunListener assumes a Test will be Started before it Fails or Finishes.
    *
    * Reality:
    * JUnits ParentRunner is responsible for adding the BeforeClass/AfterClass statements to the
    * statement execution chain. After BeforeClass is executed, a Statement that delegates to the
    * abstract method: runChild(T child, RunNotifier notifier) is called. As the JavaDoc explains:
    * "Subclasses are responsible for making sure that relevant test events are reported through <b>notifier</b>".
    * When a @BeforeClass fail, the child that should handle the relevant test events(Started, Failed, Finished)
    * is never executed.
    *
    * Result:
    * When Test Failed event is received in ConcurrentReportingRunListener without a Started event received first,
    * it causes a NullPointException because there is no ClassReporter setup for that class yet. When this Exception
    * is thrown from the ConcurrentReportingRunListener, JUnit catches the exception and reports is as a Failed test.
    * But to avoid a wild loop, it removes the failing Listener before calling Failed test again. Since the
    * ConcurrentReportingRunListener now is removed from the chain it will never receive the RunFinished event
    * and the recorded state will never be replayed on the ReportManager.
    *
    * The End result: ReporterManager falsely believe no Test were run.
    *
    */
    @SuppressWarnings( { "unchecked", "ThrowableResultOfMethodCallIgnored" } )
    public void testSurefireShouldBeAbleToReportRunStatusEvenWithFailingTests()
        throws Exception
    {
        Result result = new JUnitCoreTester().run( false, FailingTestClassTestNot.class );

        Assert.assertEquals( "JUnit should report correctly number of test ran(Finished)", 0, result.getRunCount() );

        for ( Failure failure : result.getFailures() )
        {
            System.out.println( failure.getException().getMessage() );
        }

        Assert.assertEquals( "There should only be one Exception reported, the one from the failing TestCase", 1,
                             result.getFailureCount() );

        Assert.assertEquals( "The exception thrown by the failing TestCase", RuntimeException.class,
                             result.getFailures().get( 0 ).getException().getClass() );

    }

    /**
     * Simple TestCase to force a Exception in @BeforeClass.
     */
    public static class FailingTestClassTestNot
    {
        @BeforeClass
        public static void failingBeforeClass()
            throws Exception
        {
            throw new RuntimeException( "Opps, we failed in @BeforeClass" );
        }

        @Test
        public void shouldNeverBeCalled()
            throws Exception
        {
            Assert.assertTrue( true );
        }
    }
}
