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

import org.apache.maven.surefire.util.internal.DumpFileUtils;

import java.io.File;

import static java.lang.String.format;
import static org.apache.maven.plugin.surefire.SurefireHelper.DUMP_FILENAME;
import static org.apache.maven.plugin.surefire.SurefireHelper.DUMP_FILENAME_FORMATTER;
import static org.apache.maven.plugin.surefire.SurefireHelper.DUMPSTREAM_FILENAME;
import static org.apache.maven.plugin.surefire.SurefireHelper.DUMPSTREAM_FILENAME_FORMATTER;

/**
 * Reports errors to dump file.
 * Used only within java process of the plugin itself and not the forked JVM.
 */
public final class InPluginProcessDumpSingleton
{
    private static final InPluginProcessDumpSingleton SINGLETON = new InPluginProcessDumpSingleton();

    private InPluginProcessDumpSingleton()
    {
    }

    public static InPluginProcessDumpSingleton getSingleton()
    {
        return SINGLETON;
    }

    public synchronized File dumpStreamException( Throwable t, String msg, File reportsDirectory, int jvmRun )
    {
        File dump = newDumpStreamFile( reportsDirectory, jvmRun );
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dump );
        return dump;
    }

    public synchronized void dumpStreamException( Throwable t, String msg, File reportsDirectory )
    {
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, newDumpStreamFile( reportsDirectory ) );
    }

    public synchronized File dumpStreamText( String msg, File reportsDirectory, int jvmRun )
    {
        File dump = newDumpStreamFile( reportsDirectory, jvmRun );
        DumpFileUtils.dumpText( msg == null ? "null" : msg, dump );
        return dump;
    }

    public synchronized void dumpStreamText( String msg, File reportsDirectory )
    {
        DumpFileUtils.dumpText( msg == null ? "null" : msg, newDumpStreamFile( reportsDirectory ) );
    }

    public synchronized void dumpException( Throwable t, String msg, File reportsDirectory, int jvmRun )
    {
        File dump = newDumpFile( reportsDirectory, jvmRun );
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dump );
    }

    public synchronized void dumpException( Throwable t, String msg, File reportsDirectory )
    {
        File dump = newDumpFile( reportsDirectory );
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dump );
    }

    private File newDumpStreamFile( File reportsDirectory )
    {
        return new File( reportsDirectory, DUMPSTREAM_FILENAME );
    }

    private static File newDumpStreamFile( File reportsDirectory, int jvmRun )
    {
        return new File( reportsDirectory, format( DUMPSTREAM_FILENAME_FORMATTER, jvmRun ) );
    }

    private static File newDumpFile( File reportsDirectory, int jvmRun )
    {
        return new File( reportsDirectory, format( DUMP_FILENAME_FORMATTER, jvmRun ) );
    }

    private static File newDumpFile( File reportsDirectory )
    {
        return new File( reportsDirectory, DUMP_FILENAME );
    }
}
