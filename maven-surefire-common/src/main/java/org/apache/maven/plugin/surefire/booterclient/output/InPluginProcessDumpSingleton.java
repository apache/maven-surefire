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

import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.util.internal.DumpFileUtils;
import java.io.File;

/**
 * Reports errors to dump file.
 * Used only within java process of the plugin itself and not the forked JVM.
 */
public final class InPluginProcessDumpSingleton
{
    private static final InPluginProcessDumpSingleton SINGLETON = new InPluginProcessDumpSingleton();

    private final String creationDate = DumpFileUtils.newFormattedDateFileName();

    private InPluginProcessDumpSingleton()
    {
    }

    public static InPluginProcessDumpSingleton getSingleton()
    {
        return SINGLETON;
    }

    public synchronized void dumpException( Throwable t, String msg, DefaultReporterFactory defaultReporterFactory )
    {
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, newDumpFile( defaultReporterFactory ) );
    }

    public synchronized void dumpException( Throwable t, DefaultReporterFactory defaultReporterFactory )
    {
        DumpFileUtils.dumpException( t, newDumpFile( defaultReporterFactory ) );
    }

    public synchronized void dumpText( String msg, DefaultReporterFactory defaultReporterFactory )
    {
        DumpFileUtils.dumpText( msg == null ? "null" : msg, newDumpFile( defaultReporterFactory ) );
    }

    private File newDumpFile( DefaultReporterFactory defaultReporterFactory )
    {
        File reportsDirectory = defaultReporterFactory.getReportsDirectory();
        return new File( reportsDirectory, creationDate + ".dumpstream" );
    }
}
