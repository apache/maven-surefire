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

import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.junit.runner.notification.RunListener;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnit4Provider
    implements SurefireProvider
{

    private final ReporterFactory reporterFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;

    private final List<RunListener> customRunListeners;

    public JUnit4Provider( ProviderParameters booterParameters )
    {
        this.reporterFactory = booterParameters.getReporterFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
        customRunListeners =
            createCustomListeners( booterParameters.getProviderProperties().getProperty( "listener" ) );

    }

    @SuppressWarnings( { "UnnecessaryUnboxing" } )
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        JUnit4DirectoryTestSuite suite = getSuite();
        suite.locateTestSets( testClassLoader );
        if ( forkTestSet != null )
        {
            suite.execute( (String) forkTestSet, (ReporterManagerFactory) reporterFactory, testClassLoader );
        }
        else
        {
            suite.execute( (ReporterManagerFactory) reporterFactory, testClassLoader );
        }
        return reporterFactory.close();
    }

    private JUnit4DirectoryTestSuite getSuite()
    {
        return new JUnit4DirectoryTestSuite( directoryScanner, customRunListeners );
    }

    public Iterator getSuites()
    {
        try
        {
            return getSuite().locateTestSets( testClassLoader ).keySet().iterator();
        }
        catch ( TestSetFailedException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void upgradeCheck( JUnit4DirectoryTestSuite suite )
        throws TestSetFailedException
    {
        if ( isJunit4UpgradeCheck() && suite.getClassesSkippedByValidation().size() > 0 )
        {
            StringBuilder reason = new StringBuilder();
            reason.append( "Updated check failed\n" );
            reason.append( "There are tests that would be run with junit4 / surefire 2.6 but not with [2.7,):\n" );
            for ( Object o : suite.getClassesSkippedByValidation() )
            {
                Class testClass = (Class) o;
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
