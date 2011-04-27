package org.apache.maven.surefire.booter;

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

import java.util.Iterator;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class ForkTimeoutTest
    extends TestCase
{
    public void testClose()
        throws Exception
    {
        final Integer forkTimeout1 = new Integer( 100 );
        SurefireProvider surefireProvider = new TestProvider();
        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( null, null );
        new ForkTimeout( 100, reporterConfiguration, surefireProvider );
        try
        {
            Thread.sleep( 1500 );
        }
        catch ( InterruptedException ignore )
        {

        }
    }

    public class TestProvider
        extends AbstractProvider
    {

        public TestProvider()
        {
        }

        public Iterator getSuites()
        {
            return null;
        }

        public RunResult invoke( Object forkTestSet )
            throws TestSetFailedException, ReporterException
        {
            return new RunResult( 1, 0, 0, 2 );
        }
    }

}
