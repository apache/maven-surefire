package org.apache.maven.surefire.api.booter;

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

import org.apache.maven.surefire.api.util.internal.DumpFileUtils;

import java.io.File;

import static org.apache.maven.surefire.api.util.internal.DumpFileUtils.newDumpFile;

/**
 * Dumps lost commands and caused exceptions in forked JVM. <br>
 * Fail-safe.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20
 */
public final class DumpErrorSingleton
{
    public static final String DUMP_FILE_EXT = ".dump";
    public static final String DUMPSTREAM_FILE_EXT = ".dumpstream";
    private static final DumpErrorSingleton SINGLETON = new DumpErrorSingleton();

    private File dumpFile;
    private File dumpStreamFile;
    private File binaryDumpStreamFile;

    private DumpErrorSingleton()
    {
    }

    public static DumpErrorSingleton getSingleton()
    {
        return SINGLETON;
    }

    public synchronized void init( File reportsDir, String dumpFileName )
    {
        dumpFile = createDumpFile( reportsDir, dumpFileName );
        dumpStreamFile = createDumpStreamFile( reportsDir, dumpFileName );
        String fileNameWithoutExtension =
            dumpFileName.contains( "." ) ? dumpFileName.substring( 0, dumpFileName.lastIndexOf( '.' ) ) : dumpFileName;
        binaryDumpStreamFile = newDumpFile( reportsDir, fileNameWithoutExtension + "-commands.bin" );
    }

    public synchronized File dumpException( Throwable t, String msg )
    {
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dumpFile );
        return dumpFile;
    }

    public synchronized File dumpException( Throwable t )
    {
        DumpFileUtils.dumpException( t, dumpFile );
        return dumpFile;
    }

    public synchronized File dumpText( String msg )
    {
        DumpFileUtils.dumpText( msg == null ? "null" : msg, dumpFile );
        return dumpFile;
    }

    public synchronized File dumpStreamException( Throwable t, String msg )
    {
        DumpFileUtils.dumpException( t, msg == null ? "null" : msg, dumpStreamFile );
        return dumpStreamFile;
    }

    public synchronized File dumpStreamException( Throwable t )
    {
        DumpFileUtils.dumpException( t, dumpStreamFile );
        return dumpStreamFile;
    }

    public synchronized File dumpStreamText( String msg )
    {
        DumpFileUtils.dumpText( msg == null ? "null" : msg, dumpStreamFile );
        return dumpStreamFile;
    }

    public File getCommandStreamBinaryFile()
    {
        return binaryDumpStreamFile;
    }

    private File createDumpFile( File reportsDir, String dumpFileName )
    {
        return newDumpFile( reportsDir, dumpFileName + DUMP_FILE_EXT );
    }

    private File createDumpStreamFile( File reportsDir, String dumpFileName )
    {
        return newDumpFile( reportsDir, dumpFileName + DUMPSTREAM_FILE_EXT );
    }
}
