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

import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.util.internal.DumpFileUtils;

import java.io.File;

import static org.apache.maven.surefire.util.internal.DumpFileUtils.newDumpFile;

/**
 * Dumps lost commands and caused exceptions in forked JVM. <p/>
 * Fail-safe.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19.2
 */
public final class DumpErrorSingleton
{
    private static final String DUMP_FILE_EXT = ".dump";
    private static final String DUMPSTREAM_FILE_EXT = ".dumpstream";
    private static final DumpErrorSingleton SINGLETON = new DumpErrorSingleton();

    private File dumpFile;
    private File dumpStreamFile;

    private DumpErrorSingleton()
    {
    }

    public static DumpErrorSingleton getSingleton()
    {
        return SINGLETON;
    }

    public synchronized void init( String dumpFileName, ReporterConfiguration configuration )
    {
        dumpFile = createDumpFile( dumpFileName, configuration );
        dumpStreamFile = createDumpStreamFile( dumpFileName, configuration );
    }

    public synchronized void dumpException( Throwable t, String msg )
    {
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dumpFile );
    }

    public synchronized void dumpException( Throwable t )
    {
        DumpFileUtils.dumpException( t, dumpFile );
    }

    public synchronized void dumpText( String msg )
    {
        DumpFileUtils.dumpText( msg == null ? "null" : msg, dumpFile );
    }

    public synchronized void dumpStreamException( Throwable t, String msg )
    {
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dumpStreamFile );
    }

    public synchronized void dumpStreamException( Throwable t )
    {
        DumpFileUtils.dumpException( t, dumpStreamFile );
    }

    public synchronized void dumpStreamText( String msg )
    {
        DumpFileUtils.dumpText( msg == null ? "null" : msg, dumpStreamFile );
    }

    private File createDumpFile( String dumpFileName, ReporterConfiguration configuration )
    {
        return newDumpFile( dumpFileName + DUMP_FILE_EXT, configuration );
    }

    private File createDumpStreamFile( String dumpFileName, ReporterConfiguration configuration )
    {
        return newDumpFile( dumpFileName + DUMPSTREAM_FILE_EXT, configuration );
    }
}
