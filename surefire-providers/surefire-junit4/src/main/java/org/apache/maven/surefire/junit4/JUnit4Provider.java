package org.apache.maven.surefire.junit4;

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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DefaultDirectoryScanner;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;


/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnit4Provider
    implements SurefireProvider
{

    private static ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private final ReporterFactory reporterFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;

    private final List<RunListener> customRunListeners;

    private final JUnit4TestChecker jUnit4TestChecker;

    private TestsToRun testsToRun;

    public JUnit4Provider( ProviderParameters booterParameters )
    {
        this.reporterFactory = booterParameters.getReporterFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
        customRunListeners =
            createCustomListeners( booterParameters.getProviderProperties().getProperty( "listener" ) );
        jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );

    }

    @SuppressWarnings( { "UnnecessaryUnboxing" } )
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        if ( testsToRun == null )
        {
            testsToRun = forkTestSet == null ? scanClassPath() : TestsToRun.fromClass( (Class) forkTestSet );
        }

        upgradeCheck();

        JUnit4TestSetReporter jUnit4TestSetReporter = new JUnit4TestSetReporter( null, null );

        RunNotifier runNotifer = getRunNotifer( jUnit4TestSetReporter, customRunListeners );
        for ( Class clazz : testsToRun.getLocatedClasses() )
        {
            ReporterManager reporter = (ReporterManager) reporterFactory.createReporter();
            jUnit4TestSetReporter.setTestSet( clazz );
            jUnit4TestSetReporter.setReportMgr( reporter );
            executeTestSet( clazz, reporter, testClassLoader, runNotifer );
        }

        closeRunNotifer( jUnit4TestSetReporter, customRunListeners );

        return reporterFactory.close();

    }

    private RunNotifier getRunNotifer( RunListener main, List<RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.addListener( main );
        for ( RunListener listener : others )
        {
            fNotifier.addListener( listener );
        }
        return fNotifier;
    }

    // I am not entierly sure as to why we do this explicit freeing, it's one of those
    // pieces of code that just seem to linger on in here ;)

    private void closeRunNotifer( RunListener main, List<RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.removeListener( main );
        for ( RunListener listener : others )
        {
            fNotifier.removeListener( listener );
        }
    }

    public Iterator getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    private void executeTestSet( Class clazz, ReporterManager reporter, ClassLoader classLoader, RunNotifier listeners )
        throws ReporterException, TestSetFailedException
    {

        ReportEntry report = new SimpleReportEntry( this.getClass().getName(), clazz.getName() );

        reporter.testSetStarting( report );

        JUnit4TestSet.execute( clazz, listeners );

        report = new SimpleReportEntry( this.getClass().getName(), clazz.getName() );

        reporter.testSetCompleted( report );

        reporter.reset();

    }


    private TestsToRun scanClassPath()
    {
        return directoryScanner.locateTestClasses( testClassLoader, jUnit4TestChecker );
    }

    private void upgradeCheck()
        throws TestSetFailedException
    {
        if ( isJunit4UpgradeCheck()
            && ( (DefaultDirectoryScanner) directoryScanner ).getClassesSkippedByValidation().size() > 0 )
        {
            StringBuilder reason = new StringBuilder();
            reason.append( "Updated check failed\n" );
            reason.append( "There are tests that would be run with junit4 / surefire 2.6 but not with [2.7,):\n" );
            //noinspection unchecked
            for ( Class testClass : (List<Class>) ( (DefaultDirectoryScanner) directoryScanner ).getClassesSkippedByValidation() )
            {
                reason.append( "   " );
                reason.append( testClass.getCanonicalName() );
                reason.append( "\n" );
            }
            throw new TestSetFailedException( reason.toString() );
        }
    }

    private boolean isJunit4UpgradeCheck()
    {
        final String property = System.getProperty( "surefire.junit4.upgradecheck" );
        return property != null;
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }

    private List<RunListener> createCustomListeners( String listenerProperty )
    {
        List<RunListener> result = new LinkedList<RunListener>();
        if ( listenerProperty == null )
        {
            return result;
        }

        for ( String thisListenerName : listenerProperty.split( "," ) )
        {
            RunListener customRunListener =
                (RunListener) ReflectionUtils.instantiate( Thread.currentThread().getContextClassLoader(),
                                                           thisListenerName );
            result.add( customRunListener );
        }

        return result;
    }
}
