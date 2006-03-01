package org.apache.maven.surefire;

import org.apache.maven.surefire.report.ForkingReport;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author Jason van Zyl
 * @version $Revision$
 */
public class ForkingWriterStreamConsumer
    implements StreamConsumer
{
    private PrintWriter printWriter;

    private int standardPrefixLength;

    private int headingPrefixLength;

    boolean showHeading;

    public ForkingWriterStreamConsumer( Writer writer, boolean showHeading )
    {
        this.showHeading = showHeading;

        printWriter = new PrintWriter( writer );

        standardPrefixLength = ForkingReport.FORKING_PREFIX_STANDARD.length();

        headingPrefixLength = ForkingReport.FORKING_PREFIX_HEADING.length();
    }

    public void consumeLine( String line )
    {
        if ( line.startsWith( ForkingReport.FORKING_PREFIX_HEADING ) )
        {
            if ( showHeading )
            {
                printWriter.println( line.substring( headingPrefixLength ) );

                printWriter.flush();
            }
        }
        else if ( line.startsWith( ForkingReport.FORKING_PREFIX_STANDARD ) )
        {
            printWriter.println( line.substring( standardPrefixLength ) );

            printWriter.flush();
        }
        else
        {
            printWriter.println( line );

            printWriter.flush();
        }
    }
}
