package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.surefire.report.ReportEntry;

/**
 * Imposes synchronization on a non-thredsafe OutputConsumer
 *
 * @author Kristian Rosenvold
 */
public class SynchronizedOutputConsumer
    implements OutputConsumer
{

    final OutputConsumer target;

    public SynchronizedOutputConsumer( OutputConsumer target )
    {
        this.target = target;
    }

    public synchronized void consumeHeaderLine( String line )
    {
        target.consumeHeaderLine( line );
    }

    public synchronized void consumeMessageLine( String line )
    {
        target.consumeMessageLine( line );
    }

    public synchronized void consumeFooterLine( String line )
    {
        target.consumeFooterLine( line );
    }

    public synchronized void consumeOutputLine( String line )
    {
        target.consumeOutputLine( line );
    }

    public synchronized void testSetStarting( ReportEntry reportEntry )
    {
        target.testSetStarting( reportEntry );
    }

    public synchronized void testSetCompleted()
    {
        target.testSetCompleted();
    }
}
