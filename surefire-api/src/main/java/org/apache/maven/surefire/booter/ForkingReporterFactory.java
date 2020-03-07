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

import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;

/**
 * Creates ForkingReporters, which are typically one instance per TestSet or thread.
 * This factory is only used inside forks.
 *
 * @author Kristian Rosenvold
 */
public class ForkingReporterFactory
    implements ReporterFactory
{
    private final boolean trimstackTrace;

    private final ForkedChannelEncoder eventChannel;

    public ForkingReporterFactory( boolean trimstackTrace, ForkedChannelEncoder eventChannel )
    {
        this.trimstackTrace = trimstackTrace;
        this.eventChannel = eventChannel;
    }

    @Override
    public RunListener createReporter()
    {
        return new ForkingRunListener( eventChannel, trimstackTrace );
    }

    @Override
    public RunResult close()
    {
        return new RunResult( 17, 17, 17, 17 );
    }
}
