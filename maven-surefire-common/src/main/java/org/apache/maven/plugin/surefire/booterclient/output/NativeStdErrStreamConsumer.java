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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.shared.utils.cli.StreamConsumer;

/**
 * Used by forked JMV, see {@link org.apache.maven.plugin.surefire.booterclient.ForkStarter}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 * @see org.apache.maven.plugin.surefire.booterclient.ForkStarter
 */
public final class NativeStdErrStreamConsumer
    implements StreamConsumer
{
    private final DefaultReporterFactory defaultReporterFactory;

    public NativeStdErrStreamConsumer( DefaultReporterFactory defaultReporterFactory )
    {
        this.defaultReporterFactory = defaultReporterFactory;
    }

    @Override
    public void consumeLine( String line )
    {
        InPluginProcessDumpSingleton.getSingleton().dumpText( line, defaultReporterFactory );
    }
}
