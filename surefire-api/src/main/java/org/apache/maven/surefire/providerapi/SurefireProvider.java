package org.apache.maven.surefire.providerapi;

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

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Iterator;

/**
 * Interface to be implemented by all Surefire providers.
 *
 * @author Kristian Rosenvold
 */
public interface SurefireProvider
{
    /**
     * Called when forkmode is pertest, allows the provider to define what "pertest" will be.
     *
     * @return
     */
    public Iterator getSuites();

    /**
     * The test that will be invoked through a fork; used only for forkmode=pertest, when the classpath
     * scanning happens on the plugin-side. When this is set, the forked process will run only that test
     * and not scan the classpath
     */

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException;
}
