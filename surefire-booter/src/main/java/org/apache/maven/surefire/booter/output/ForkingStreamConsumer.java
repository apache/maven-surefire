package org.apache.maven.surefire.booter.output;

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

import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * {@link StreamConsumer} that understands Surefire output made by {@link ForkingConsoleReporter}
 * and filters it depending on configuration options
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @since 2.1
 */
public class ForkingStreamConsumer
    implements StreamConsumer
{
    private static int STANDARD_PREFIX_LENGTH = ForkingConsoleReporter.FORKING_PREFIX_STANDARD.length();

    private static int HEADING_PREFIX_LENGTH = ForkingConsoleReporter.FORKING_PREFIX_HEADING.length();

    private static int FOOTER_PREFIX_LENGTH = ForkingConsoleReporter.FORKING_PREFIX_FOOTER.length();

    private OutputConsumer outputConsumer;

    public ForkingStreamConsumer( OutputConsumer outputConsumer )
    {
        this.outputConsumer = outputConsumer;
    }

    public void consumeLine( String line )
    {
        if ( line.startsWith( ForkingConsoleReporter.FORKING_PREFIX_HEADING ) )
        {
            outputConsumer.consumeHeaderLine( line.substring( HEADING_PREFIX_LENGTH ) );
        }
        else if ( line.startsWith( ForkingConsoleReporter.FORKING_PREFIX_STANDARD ) )
        {
            String message = line.substring( STANDARD_PREFIX_LENGTH );
            if ( ForkingConsoleReporter.isTestSetStartingMessage( message ) )
            {
                outputConsumer.testSetStarting( ForkingConsoleReporter.parseTestSetStartingMessage( message ) );
            }
            else if ( ForkingConsoleReporter.isTestSetCompletedMessage( message ) )
            {
                outputConsumer.testSetCompleted();
            }
            outputConsumer.consumeMessageLine( message );
        }
        else if ( line.startsWith( ForkingConsoleReporter.FORKING_PREFIX_FOOTER ) )
        {
            outputConsumer.consumeFooterLine( line.substring( FOOTER_PREFIX_LENGTH ) );
        }
        else
        {
            outputConsumer.consumeOutputLine( line );
        }
    }
}
