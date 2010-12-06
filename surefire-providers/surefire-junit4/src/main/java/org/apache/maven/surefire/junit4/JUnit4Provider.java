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
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;

import java.util.Iterator;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnit4Provider
    implements SurefireProvider
{

    private final ReporterManagerFactory reporterManagerFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;

    public JUnit4Provider( ProviderParameters booterParameters )
    {
        this.reporterManagerFactory = booterParameters.getReporterManagerFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
    }

    @SuppressWarnings( { "UnnecessaryUnboxing" } )
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        JUnit4DirectoryTestSuite suite = getSuite();
        suite.locateTestSets( testClassLoader );
        if ( forkTestSet != null )
        {
            suite.execute( (String) forkTestSet, reporterManagerFactory, testClassLoader );
        }
        else
        {
            suite.execute( reporterManagerFactory, testClassLoader );
        }
        reporterManagerFactory.warnIfNoTests();

        return reporterManagerFactory.close();
    }

    private JUnit4DirectoryTestSuite getSuite()
    {
        return new JUnit4DirectoryTestSuite( directoryScanner );

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


}
