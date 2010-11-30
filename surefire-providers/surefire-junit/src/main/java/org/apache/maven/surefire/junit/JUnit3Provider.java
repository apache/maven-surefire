package org.apache.maven.surefire.junit;

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

import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;

import java.util.Iterator;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class JUnit3Provider
    implements SurefireProvider
{
    private final ReporterManagerFactory reporterManagerFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;

    public JUnit3Provider( ReporterManagerFactory reporterManagerFactory, ClassLoader testClassLoader,
                           DirectoryScanner directoryScanner )
    {
        this.reporterManagerFactory = reporterManagerFactory;
        this.testClassLoader = testClassLoader;
        this.directoryScanner = directoryScanner;
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        JUnitDirectoryTestSuite suite = getSuite();
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

    private JUnitDirectoryTestSuite getSuite()
    {
        return new JUnitDirectoryTestSuite( directoryScanner );

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
}
