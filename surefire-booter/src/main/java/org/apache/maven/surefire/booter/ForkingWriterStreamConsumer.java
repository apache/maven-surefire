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

import org.apache.maven.surefire.booter.output.ForkingStreamConsumer;
import org.apache.maven.surefire.report.ForkingConsoleReporter;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author Jason van Zyl
 * @version $Revision$
 * @deprecated use {@link ForkingStreamConsumer}
 */
public class ForkingWriterStreamConsumer
    implements StreamConsumer
{
    private PrintWriter printWriter;

    private int standardPrefixLength;

    private int headingPrefixLength;

    private boolean showHeading;

    private int footerPrefixLength;

    private boolean showFooter;

    public ForkingWriterStreamConsumer( Writer writer, boolean showHeading, boolean showFooter )
    {
        this.showHeading = showHeading;

        this.showFooter = showFooter;

        printWriter = new PrintWriter( writer );

        standardPrefixLength = ForkingConsoleReporter.FORKING_PREFIX_STANDARD.length();

        headingPrefixLength = ForkingConsoleReporter.FORKING_PREFIX_HEADING.length();

        footerPrefixLength = ForkingConsoleReporter.FORKING_PREFIX_FOOTER.length();
    }

    public void consumeLine( String line )
    {
        if ( line.startsWith( ForkingConsoleReporter.FORKING_PREFIX_HEADING ) )
        {
            if ( showHeading )
            {
                printWriter.println( line.substring( headingPrefixLength ) );
            }
        }
        else if ( line.startsWith( ForkingConsoleReporter.FORKING_PREFIX_STANDARD ) )
        {
            printWriter.println( line.substring( standardPrefixLength ) );
        }
        else if ( line.startsWith( ForkingConsoleReporter.FORKING_PREFIX_FOOTER ) )
        {
            if ( showFooter )
            {
                printWriter.println( line.substring( footerPrefixLength ) );
            }
        }
        else
        {
            // stdout
            printWriter.println( line );
        }
        printWriter.flush();
    }
}
