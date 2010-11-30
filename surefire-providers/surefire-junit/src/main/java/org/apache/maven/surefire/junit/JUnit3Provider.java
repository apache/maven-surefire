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

import org.apache.maven.surefire.providerapi.FileScanningProvider;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Iterator;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class JUnit3Provider
    extends FileScanningProvider
    implements SurefireProvider
{

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        JUnitDirectoryTestSuite suite = getSuite();
        suite.locateTestSets( getTestsClassLoader() );
        if ( forkTestSet != null )
        {
            suite.execute( (String) forkTestSet, getReporterManagerFactory(),
                           getTestsClassLoader() );
        }
        else
        {
            suite.execute( getReporterManagerFactory(), getTestsClassLoader() );
        }
        return RunResult.totalCountOnly( suite.getNumTests() );
    }

    private JUnitDirectoryTestSuite getSuite()
    {
        return new JUnitDirectoryTestSuite( getDirectoryScanner() );

    }

    public Iterator getSuites()
    {
        try
        {
            return getSuite().locateTestSets( getTestsClassLoader() ).keySet().iterator();
        }
        catch ( TestSetFailedException e )
        {
            throw new RuntimeException( e );
        }
    }
}
