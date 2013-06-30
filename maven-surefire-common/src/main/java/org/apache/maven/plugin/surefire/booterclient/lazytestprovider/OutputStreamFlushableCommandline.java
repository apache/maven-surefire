package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import java.io.IOException;
import java.io.OutputStream;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.Commandline;

/**
 * A {@link Commandline} implementation that provides the output stream of
 * the executed process in form of a {@link FlushReceiver}, for it to be
 * flushed on demand.
 *
 * @author Andreas Gudian
 */
public class OutputStreamFlushableCommandline
    extends Commandline
    implements FlushReceiverProvider
{
    /**
     * Wraps an output stream in order to delegate a flush.
     */
    private final class OutputStreamFlushReceiver
        implements FlushReceiver
    {
        private final OutputStream outputStream;

        private OutputStreamFlushReceiver( OutputStream outputStream )
        {
            this.outputStream = outputStream;
        }

        public void flush()
            throws IOException
        {
            outputStream.flush();
        }
    }

    private FlushReceiver flushReceiver;

    @Override
    public Process execute()
        throws CommandLineException
    {
        Process process = super.execute();

        if ( process.getOutputStream() != null )
        {
            flushReceiver = new OutputStreamFlushReceiver( process.getOutputStream() );
        }

        return process;
    }

    /* (non-Javadoc)
      * @see org.apache.maven.plugin.surefire.booterclient.lazytestprovider.FlushReceiverProvider#getFlushReceiver()
      */
    public FlushReceiver getFlushReceiver()
    {
        return flushReceiver;
    }

}