package org.apache.maven.surefire.suite;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Map;

/**
 * A complete test suite that contains one or more test sets.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface SurefireTestSuite
{
    void execute( ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException;

    void execute( String testSetName, ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException;

    int getNumTests();

    int getNumTestSets();

    Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException;
}
