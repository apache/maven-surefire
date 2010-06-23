package org.apache.maven.surefire.report;

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

import org.apache.maven.surefire.util.TeeStream;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Captures System.out/System.err streams to buffers.
 * <p/>
 * Please note that this design is inherently single-threaded test-linear, and is intended only
 * for use with ReporterManager, which is also test-linear. While it will capture
 * output in a multi-threaded scenario, there's no way to associate ouput with the correct
 * test/thread.
 * <p/>
 * Note; this class does not need synchronization because all of these methods are serially invoked on
 * the same thread. Or maybe not. See notes inside ReporterManager about the general improperness
 * of this design in multithreading.
 */
public class SystemStreamCapturer
{
    private final PrintStream oldOut;

    private final PrintStream oldErr;

    private final PrintStream newErr;

    private final PrintStream newOut;

    private final ByteArrayOutputStream stdOut;

    private final ByteArrayOutputStream stdErr;

    public SystemStreamCapturer()
    {
        stdOut = new ByteArrayOutputStream();

        newOut = new PrintStream( stdOut );

        oldOut = System.out;

        TeeStream tee = new TeeStream( oldOut, newOut );
        System.setOut( tee );

        stdErr = new ByteArrayOutputStream();

        newErr = new PrintStream( stdErr );

        oldErr = System.err;

        tee = new TeeStream( oldErr, newErr );
        System.setErr( tee );
    }


    public void restoreStreams()
    {
        // Note that the fields can be null if the test hasn't even started yet (an early error)
        if ( oldOut != null )
        {
            System.setOut( oldOut );
        }
        if ( oldErr != null )
        {
            System.setErr( oldErr );
        }

        IOUtil.close( newOut );
        IOUtil.close( newErr );
    }

    public void clearCapturedContent()
    {
        if ( stdOut != null )
        {
            stdOut.reset();
        }
        if ( stdErr != null )
        {
            stdErr.reset();
        }
    }

    public String getStdOutLog()
    {
        // Note that the fields can be null if the test hasn't even started yet (an early error)
        return stdOut != null ? stdOut.toString() : "";
    }

    public String getStdErrLog()
    {
        // Note that the fields can be null if the test hasn't even started yet (an early error)
        return stdErr != null ? stdErr.toString() : "";
    }
}
