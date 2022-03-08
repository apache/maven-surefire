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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;

/**
 * A stream-like object that preserves ordering between stdout/stderr
 */
@Deprecated // remove this class after StatelessXmlReporter is capable of parallel test sets processing
final class LogicalStream
{
    private final Queue<TestOutputReportEntry> output = new ConcurrentLinkedQueue<>();

    synchronized void write( TestOutputReportEntry reportEntry )
    {
        output.add( reportEntry );
    }

    void writeDetails( TestOutputReceiver<TestOutputReportEntry> outputReceiver )
    {
        for ( TestOutputReportEntry entry = output.poll(); entry != null; entry = output.poll() )
        {
            outputReceiver.writeTestOutput( entry );
        }
    }
}
